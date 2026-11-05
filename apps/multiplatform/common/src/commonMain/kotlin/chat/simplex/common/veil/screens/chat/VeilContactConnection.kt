package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.settings.VeilNote
import chat.simplex.common.veil.screens.settings.VeilSectionLabel
import chat.simplex.common.views.chatlist.deleteContactConnectionAlert
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.AddContactLearnMore
import chat.simplex.common.views.newchat.sharedProfileInfo
import chat.simplex.common.views.usersettings.IncognitoView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Veil page of a pending connection (an invitation you created or one you accepted): replaces upstream's
 * `ContactConnectionInfoView`.
 *
 * What upstream's screen does, all kept: shows the invitation in `ChatModel.showingInvitation` while it is open (and drops
 * it when the screen goes away), saves the local name with `controller.apiSetConnectionAlias` + `updateContactConnection`,
 * shows the one-time link as a QR code (short or full form) with share (marks the invitation as used) and the incognito
 * note (upstream's `IncognitoView`), the "learn more" explainer (upstream's `AddContactLearnMore`), the line about which
 * profile is shared (`sharedProfileInfo`) and deletes the connection through upstream's `deleteContactConnectionAlert`.
 */
@Composable
fun VeilContactConnectionScreen(rhId: Long?, connLinkInvitation: CreatedConnLink?, contactConnection: PendingContactConnection, focusAlias: Boolean, close: () -> Unit) {
  LaunchedEffect(connLinkInvitation) {
    if (connLinkInvitation != null) {
      chatModel.showingInvitation.value = ShowingInvitation(contactConnection.id, connLinkInvitation, false, conn = contactConnection)
    }
  }
  // Like upstream: when "learn more" is open the invitation must stay, otherwise it is dropped with this screen
  DisposableEffect(Unit) {
    onDispose {
      if (!ModalManager.center.hasModalsOpen() || appPlatform.isDesktop) {
        chatModel.showingInvitation.value = null
      }
    }
  }
  val showShortLink = remember { mutableStateOf(true) }
  val clipboard = LocalClipboardManager.current
  val initiated = contactConnection.initiated
  val link = connLinkInvitation

  VeilScreenScaffold(
    title = stringResource(if (initiated) MR.strings.veil_cconn_title_invited else MR.strings.veil_cconn_title_accepted),
    onBack = close,
  ) {
    VeilNote(
      stringResource(
        when (connectionWait(contactConnection.viaContactUri, contactConnection.groupLinkId != null)) {
          ConnectionWait.GroupHost -> MR.strings.veil_cconn_wait_group
          ConnectionWait.ConnectionRequest -> MR.strings.veil_cconn_wait_request
          ConnectionWait.ContactDevice -> MR.strings.veil_cconn_wait_contact
        }
      ),
      Modifier.padding(bottom = 6.dp),
    )

    if (connectionAliasEditable(contactConnection.groupLinkId != null)) {
      VeilAliasField(
        key = contactConnection.id,
        initial = contactConnection.localAlias,
        placeholder = stringResource(MR.strings.veil_chatinfo_alias_hint),
        focus = focusAlias,
        save = { setConnectionAlias(rhId, contactConnection, it) },
      )
    }

    val showLink = link != null && connectionLinkShown(link.connFullLink.isNotEmpty(), initiated)
    VeilSectionLabel(stringResource(if (showLink) MR.strings.veil_cconn_link else MR.strings.veil_cconn_details))
    VeilCard(Modifier.padding(bottom = 6.dp), contentPadding = PaddingValues(8.dp)) {
      if (showLink && link != null) {
        val short = showShortLink.value
        VeilQr(link.simplexChatUri(short = short), small = short && link.connShortLink != null, description = stringResource(MR.strings.veil_cconn_qr))
        if (connectionLinkToggleShown(appPrefs.developerTools.state.value, link.connShortLink != null)) {
          VeilInfoRow(
            icon = MR.images.ic_qr_code,
            title = stringResource(if (short) MR.strings.veil_cconn_full_link else MR.strings.veil_cconn_short_link),
            onClick = { showShortLink.value = !short },
          )
        }
      }
      if (contactConnection.incognito) {
        VeilInfoRow(
          icon = MR.images.ic_theater_comedy_filled,
          title = stringResource(MR.strings.veil_chat_incognito),
          value = stringResource(MR.strings.veil_cconn_incognito_hint),
          onClick = { ModalManager.end.showModal { IncognitoView() } },
        )
      }
      if (showLink && link != null) {
        VeilInfoRow(
          icon = MR.images.ic_share,
          title = stringResource(MR.strings.veil_cconn_share),
          onClick = {
            chatModel.showingInvitation.value = chatModel.showingInvitation.value?.copy(connChatUsed = true)
            clipboard.shareText(simplexChatLink(link.simplexChatUri(short = showShortLink.value)))
          },
        )
      }
      VeilInfoRow(
        icon = MR.images.ic_info,
        title = stringResource(MR.strings.veil_cconn_learn_more),
        onClick = { ModalManager.end.showModalCloseable { closeMore -> AddContactLearnMore(closeMore) } },
      )
    }
    VeilNote(sharedProfileInfo(chatModel, contactConnection.incognito), Modifier.padding(bottom = 10.dp))

    VeilButton(
      stringResource(MR.strings.veil_cconn_delete),
      onClick = { deleteContactConnectionAlert(rhId, contactConnection, chatModel, close) },
      style = VeilButtonStyle.Danger,
    )
  }
}

/** Same call and refresh as upstream's private `setContactAlias` of the pending connection screen. */
private fun setConnectionAlias(rhId: Long?, contactConnection: PendingContactConnection, localAlias: String) = withBGApi {
  chatModel.controller.apiSetConnectionAlias(rhId, contactConnection.pccConnId, localAlias)?.let {
    withContext(Dispatchers.Main) {
      chatModel.chatsContext.updateContactConnection(rhId, it)
    }
  }
}
