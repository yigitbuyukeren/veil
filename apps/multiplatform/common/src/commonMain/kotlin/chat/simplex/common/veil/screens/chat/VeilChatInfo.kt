package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.VeilUi
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.groups.veilGroupEligible
import chat.simplex.common.veil.screens.groups.veilOpenGroupInfo
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chatlist.toggleNotifications
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

/**
 * Tap on the top bar name ([VeilChatHooks.onChatInfo] default). Direct contacts get the Veil screen below and plain
 * groups the Veil group info (`veilOpenGroupInfo`); every other chat type (notes, channels, business chats, groups
 * with a support scope) keeps upstream's info through [openUpstreamInfo].
 */
fun veilShowChatInfo(openUpstreamInfo: () -> Unit) {
  val chat = chatModel.chatId.value?.let { chatModel.getChat(it) }
  val info = chat?.chatInfo
  if (veilChatActive() && VeilUi.groups && chat != null && info is ChatInfo.Group) {
    val g = info.groupInfo
    if (veilGroupEligible(g.useRelays, g.businessChat != null, g.nextConnectPrepared, info.groupChatScope != null)) {
      veilOpenGroupInfo(chat, info)
    } else {
      openUpstreamInfo()
    }
    return
  }
  if (!veilChatActive() || chat == null || info !is ChatInfo.Direct) {
    openUpstreamInfo()
    return
  }
  // same toggle as upstream's info(): a second tap closes the open info
  if (ModalManager.end.hasModalsOpen()) {
    ModalManager.end.closeModals()
    return
  }
  val rhId = chat.remoteHostId
  val contactId = info.contact.contactId
  withBGApi {
    // preload before showing the screen, like upstream
    val stats = chatModel.controller.apiContactInfo(rhId, contactId)
    val code = chatModel.controller.apiGetContactCode(rhId, contactId)?.second
    ModalManager.end.showCustomModal { close ->
      VeilContactInfoScreen(rhId, contactId, stats?.first, stats?.second, code, close)
    }
  }
}

/** Info screen of a direct contact: who, which identity, verification, notifications, preferences, clear / delete. */
@Composable
fun VeilContactInfoScreen(rhId: Long?, contactId: Long, connectionStats: ConnectionStats?, customUserProfile: Profile?, connectionCode: String?, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val chat = remember(contactId) { derivedStateOf { chatModel.getContactChat(contactId) } }.value
  val contact = (chat?.chatInfo as? ChatInfo.Direct)?.contact
  if (chat == null || contact == null) {
    // the contact was deleted while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  val view = LocalMultiplatformView()
  LaunchedEffect(Unit) { hideKeyboard(view) }
  val incognito = customUserProfile != null || contact.contactConnIncognito
  val canVerify = contact.ready && contact.active && connectionCode != null
  val notificationsOn = contact.chatSettings.enableNtfs == MsgFilter.All

  VeilScreenScaffold(title = stringResource(MR.strings.veil_chatinfo_title), onBack = close) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      MaskAvatar(
        seed = chatAvatarSeed(chat.id, contact.profile.displayName),
        size = 88.dp,
        incognito = contact.contactConnIncognito,
        modifier = Modifier.padding(bottom = 10.dp),
      )
      BasicText(
        contact.displayName,
        style = type.sheetTitle.copy(color = colors.ink, textAlign = TextAlign.Center),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      // the name the contact chose, when a local alias hides it
      val profileName = contact.profile.displayName
      if (contact.localAlias.isNotBlank() && profileName != contact.displayName) {
        BasicText(profileName, Modifier.padding(top = 2.dp), style = type.caption.copy(color = colors.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      if (incognito) {
        Spacer(Modifier.height(8.dp))
        VeilChip(stringResource(MR.strings.veil_chat_incognito), style = VeilChipStyle.Dashed)
      }
    }

    AliasField(chat, contact.localAlias)

    if (customUserProfile != null) {
      VeilCard(Modifier.padding(bottom = 14.dp)) {
        BasicText(stringResource(MR.strings.veil_chatinfo_incognito_title), style = type.rowTitle.copy(color = colors.ink))
        BasicText(
          String.format(stringResource(MR.strings.veil_chatinfo_incognito_body), customUserProfile.chatViewName),
          Modifier.padding(top = 4.dp),
          style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
        )
      }
    }

    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      if (canVerify) {
        VeilInfoRow(
          icon = MR.images.ic_verified_user,
          title = stringResource(MR.strings.veil_verify_title),
          value = stringResource(if (contact.verified) MR.strings.veil_chat_verified else MR.strings.veil_verify_unverified),
          onClick = { veilShowContactSecurityCode(rhId, contactId, connectionCode!!) },
        )
      }
      VeilInfoRow(
        icon = if (notificationsOn) MR.images.ic_notifications else MR.images.ic_notifications_off,
        title = stringResource(MR.strings.veil_chatinfo_notifications),
        value = stringResource(if (notificationsOn) MR.strings.veil_chatinfo_notifications_on else MR.strings.veil_chatinfo_notifications_off),
        onClick = { toggleNotifications(rhId, chat.chatInfo, contact.chatSettings.enableNtfs.nextMode(false), chatModel) },
        trailing = {
          VeilSwitch(notificationsOn, onCheckedChange = { toggleNotifications(rhId, chat.chatInfo, contact.chatSettings.enableNtfs.nextMode(false), chatModel) })
        },
      )
      if (contact.ready && contact.active) {
        VeilInfoRow(
          icon = MR.images.ic_settings,
          title = stringResource(MR.strings.veil_chatinfo_preferences),
          onClick = {
            ModalManager.end.showCustomModal { closePrefs ->
              val user = chatModel.currentUser.value
              if (user != null) ContactPreferencesView(chatModel, user, rhId, contactId, closePrefs)
            }
          },
        )
      }
    }

    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      // upstream's full contact info: resync, address switch, send receipts, disappearing messages timer, servers
      VeilInfoRow(
        icon = MR.images.ic_settings_backup_restore,
        title = stringResource(MR.strings.veil_chatinfo_advanced),
        onClick = {
          ModalManager.end.showCustomModal { closeAdvanced ->
            ModalView(closeAdvanced, cardScreen = true) {
              val ct = remember { derivedStateOf { (chatModel.getContactChat(contactId)?.chatInfo as? ChatInfo.Direct)?.contact } }.value
              if (ct != null) {
                ChatInfoView(chatModel.chatsContext, chatModel, ct, connectionStats, customUserProfile, ct.localAlias, connectionCode, closeAdvanced) { }
              } else LaunchedEffect(Unit) { closeAdvanced() }
            }
          }
        },
      )
    }

    VeilButton(
      stringResource(MR.strings.veil_chatinfo_clear),
      onClick = { clearChatDialog(chat, close) },
      modifier = Modifier.padding(bottom = 10.dp),
      style = VeilButtonStyle.Secondary,
    )
    VeilButton(
      stringResource(MR.strings.veil_chatinfo_delete),
      onClick = { deleteContactDialog(chat, chatModel, close) },
      style = VeilButtonStyle.Danger,
    )
  }
}

/** Local alias, saved a moment after typing stops (like upstream's `LocalAliasEditor`) with upstream's save call. */
@Composable
private fun AliasField(chat: Chat, initial: String) {
  var text by remember(chat.id) { mutableStateOf(initial) }
  var saved by remember(chat.id) { mutableStateOf(initial) }
  LaunchedEffect(chat.id, text) {
    if (text == saved) return@LaunchedEffect
    delay(500)
    saved = text
    setContactAlias(chat, text, chatModel)
  }
  DisposableEffect(chat.id) {
    onDispose { if (text != saved) setContactAlias(chat, text, chatModel) }
  }
  VeilTextField(
    value = text,
    onValueChange = { text = it },
    modifier = Modifier.padding(bottom = 14.dp),
    placeholder = stringResource(MR.strings.veil_chatinfo_alias_hint),
  )
}
