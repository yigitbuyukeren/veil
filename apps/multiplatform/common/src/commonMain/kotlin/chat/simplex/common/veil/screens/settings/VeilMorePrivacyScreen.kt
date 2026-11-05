package chat.simplex.common.veil.screens.settings

import SectionItemView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Veil "Diğer gizlilik ayarları": replaces upstream's `PrivacySettingsView` / `MorePrivacyView` for what the Veil privacy
 * screen does not already show (app lock, screen protection, previews, incognito, locked link previews, calls and
 * delivery receipts are there). Every switch uses upstream's preference and side effect; "Verify SimpleX names" is
 * not offered because nothing reads that preference.
 */
@Composable
fun VeilMorePrivacyScreen(close: () -> Unit) {
  val sanitize = remember { appPrefs.privacySanitizeLinks.state }
  val saveDraft = remember { appPrefs.privacySaveLastDraft.state }
  val showEncryption = remember { appPrefs.privacyShowEncryption.state }
  val acceptImages = remember { appPrefs.privacyAcceptImages.state }
  val blurRadius = remember { appPrefs.privacyMediaBlurRadius.state }
  val encryptFiles = remember { appPrefs.privacyEncryptLocalFiles.state }
  val askRelays = remember { appPrefs.privacyAskToApproveRelays.state }
  val user = chatModel.currentUser.value

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_privacy_more), close) {
    VeilSectionLabel(stringResource(MR.strings.veil_mp_chats))
    VeilGroup {
      VeilSwitchRow(
        stringResource(MR.strings.veil_mp_sanitize),
        description = stringResource(MR.strings.veil_mp_sanitize_desc),
        checked = sanitize.value,
        onChange = { appPrefs.privacySanitizeLinks.set(it) }
      )
      VeilRowDivider()
      VeilSwitchRow(
        stringResource(MR.strings.veil_mp_draft),
        description = stringResource(MR.strings.veil_mp_draft_desc),
        checked = saveDraft.value,
        onChange = { on ->
          appPrefs.privacySaveLastDraft.set(on)
          if (draftToggleClearsDraft(on)) {
            chatModel.draft.value = null
            chatModel.draftChatId.value = null
          }
        }
      )
      VeilRowDivider()
      VeilSwitchRow(
        stringResource(MR.strings.veil_mp_encryption),
        description = stringResource(MR.strings.veil_mp_encryption_desc),
        checked = showEncryption.value,
        onChange = { appPrefs.privacyShowEncryption.set(it) }
      )
    }

    VeilSectionLabel(stringResource(MR.strings.veil_mp_files))
    VeilGroup {
      VeilSwitchRow(
        stringResource(MR.strings.veil_mp_accept_images),
        description = stringResource(MR.strings.veil_mp_accept_images_desc),
        checked = acceptImages.value,
        onChange = { appPrefs.privacyAcceptImages.set(it) }
      )
      VeilRowDivider()
      VeilBlurRow(blurRadius.value) { appPrefs.privacyMediaBlurRadius.set(it) }
      VeilRowDivider()
      VeilSwitchRow(
        stringResource(MR.strings.veil_mp_encrypt_files),
        description = stringResource(if (encryptFilesCanChange(encryptFiles.value)) MR.strings.veil_mp_encrypt_files_desc else MR.strings.veil_mp_encrypt_files_locked),
        checked = encryptFiles.value,
        // Locked on: it can be turned on but not off (an imported setting may have left it off).
        enabled = encryptFilesCanChange(encryptFiles.value),
        onChange = { on ->
          appPrefs.privacyEncryptLocalFiles.set(on)
          withBGApi { chatModel.controller.apiSetEncryptLocalFiles(on) }
        }
      )
      VeilRowDivider()
      VeilSwitchRow(
        stringResource(MR.strings.veil_mp_relays),
        description = stringResource(
          when (relayNote(askRelays.value)) {
            RelayNote.Asks -> MR.strings.veil_mp_relays_on
            RelayNote.DoesNotAsk -> MR.strings.veil_mp_relays_off
          }
        ),
        checked = askRelays.value,
        onChange = { appPrefs.privacyAskToApproveRelays.set(it) }
      )
    }

    if (user != null && profileSectionsShown(true, chatModel.desktopNoUserNoRemote)) {
      VeilSectionLabel(stringResource(MR.strings.veil_mp_groups))
      VeilGroup {
        VeilSwitchRow(
          stringResource(MR.strings.veil_mp_groups_auto),
          checked = user.autoAcceptMemberContacts,
          onChange = { on ->
            withApi {
              chatModel.controller.apiSetUserAutoAcceptMemberContacts(user, on)
              chatModel.currentUser.value = user.copy(autoAcceptMemberContacts = on)
            }
          }
        )
      }
      VeilNote(String.format(generalGetString(MR.strings.veil_mp_groups_note), user.displayName))
    }
  }
}

@Composable
private fun VeilBlurRow(current: Int, onSelected: (Int) -> Unit) {
  val colors = VeilTheme.colors
  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    BasicText(stringResource(MR.strings.veil_mp_blur), style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      blurChoices(current).forEach { choice ->
        val label = when (choice.level) {
          BlurLevel.Off -> stringResource(MR.strings.veil_mp_blur_off)
          BlurLevel.Soft -> stringResource(MR.strings.veil_mp_blur_soft)
          BlurLevel.Medium -> stringResource(MR.strings.veil_mp_blur_medium)
          BlurLevel.Strong -> stringResource(MR.strings.veil_mp_blur_strong)
          null -> choice.radius.toString()
        }
        val isSelected = choice.radius == current
        VeilChip(
          label,
          Modifier.semantics { selected = isSelected },
          style = if (isSelected) VeilChipStyle.Gradient else VeilChipStyle.Default,
          onClick = { onSelected(choice.radius) }
        )
      }
    }
  }
}

/**
 * Delivery receipts for contacts or small groups. Same calls as upstream's privacy screen: when chats of the kind have
 * their own choice that differs, upstream's dialog asks whether to keep or replace them.
 */
internal fun veilSetReceipts(user: User, forContacts: Boolean, enable: Boolean) {
  // a chat that follows the global choice (or is of the other kind) is null and never counts as an override
  val perChat: List<Boolean?> = chatModel.chats.value.map { chat ->
    val info = chat.chatInfo
    when {
      forContacts && info is ChatInfo.Direct -> info.contact.chatSettings.sendRcpts
      !forContacts && info is ChatInfo.Group -> info.groupInfo.chatSettings.sendRcpts
      else -> null
    }
  }
  val overrides = receiptOverrides(perChat, enable)
  when (receiptsStep(overrides)) {
    ReceiptsStep.Apply -> applyReceipts(user, forContacts, enable, clearOverrides = false)
    ReceiptsStep.AskAboutOverrides -> askAboutReceiptOverrides(user, forContacts, enable, overrides)
  }
}

private fun applyReceipts(user: User, forContacts: Boolean, enable: Boolean, clearOverrides: Boolean) {
  withLongRunningApi(slow = 60_000) {
    val settings = UserMsgReceiptSettings(enable, clearOverrides)
    if (forContacts) chatModel.controller.apiSetUserContactReceipts(user, settings) else chatModel.controller.apiSetUserGroupReceipts(user, settings)
    appPrefs.privacyDeliveryReceiptsSet.set(true)
    chatModel.currentUser.value = if (forContacts) user.copy(sendRcptsContacts = enable) else user.copy(sendRcptsSmallGroups = enable)
    if (clearOverrides) {
      withContext(Dispatchers.Main) {
        // index loop: the list can change while it is walked
        for (i in 0 until chatModel.chatsContext.chats.size) {
          val chat = chatModel.chatsContext.chats[i]
          val info = chat.chatInfo
          if (forContacts && info is ChatInfo.Direct) {
            val sendRcpts = info.contact.chatSettings.sendRcpts
            if (sendRcpts != null && sendRcpts != enable) {
              chatModel.chatsContext.updateContact(user.remoteHostId, info.contact.copy(chatSettings = info.contact.chatSettings.copy(sendRcpts = null)))
            }
          } else if (!forContacts && info is ChatInfo.Group) {
            val sendRcpts = info.groupInfo.chatSettings.sendRcpts
            if (sendRcpts != null && sendRcpts != enable) {
              chatModel.chatsContext.updateGroup(user.remoteHostId, info.groupInfo.copy(chatSettings = info.groupInfo.chatSettings.copy(sendRcpts = null)))
            }
          }
        }
      }
    }
  }
}

/** Upstream's three-way dialog (keep, replace for all, cancel) with upstream's texts. */
private fun askAboutReceiptOverrides(user: User, forContacts: Boolean, enable: Boolean, overrides: Int) {
  fun pick(contactsOn: StringResource, contactsOff: StringResource, groupsOn: StringResource, groupsOff: StringResource): StringResource =
    if (forContacts) (if (enable) contactsOn else contactsOff) else (if (enable) groupsOn else groupsOff)
  AlertManager.shared.showAlertDialogButtonsColumn(
    title = generalGetString(pick(MR.strings.receipts_contacts_title_enable, MR.strings.receipts_contacts_title_disable, MR.strings.receipts_groups_title_enable, MR.strings.receipts_groups_title_disable)),
    text = AnnotatedString(String.format(
      generalGetString(pick(MR.strings.receipts_contacts_override_disabled, MR.strings.receipts_contacts_override_enabled, MR.strings.receipts_groups_override_disabled, MR.strings.receipts_groups_override_enabled)),
      overrides
    )),
    buttons = {
      Column {
        SectionItemView({
          AlertManager.shared.hideAlert()
          applyReceipts(user, forContacts, enable, clearOverrides = false)
        }) {
          val t = stringResource(pick(MR.strings.receipts_contacts_enable_keep_overrides, MR.strings.receipts_contacts_disable_keep_overrides, MR.strings.receipts_groups_enable_keep_overrides, MR.strings.receipts_groups_disable_keep_overrides))
          Text(t, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.primary)
        }
        SectionItemView({
          AlertManager.shared.hideAlert()
          applyReceipts(user, forContacts, enable, clearOverrides = true)
        }) {
          val t = stringResource(pick(MR.strings.receipts_contacts_enable_for_all, MR.strings.receipts_contacts_disable_for_all, MR.strings.receipts_groups_enable_for_all, MR.strings.receipts_groups_disable_for_all))
          Text(t, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Red)
        }
        SectionItemView({ AlertManager.shared.hideAlert() }) {
          Text(stringResource(MR.strings.cancel_verb), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.onBackground)
        }
      }
    }
  )
}
