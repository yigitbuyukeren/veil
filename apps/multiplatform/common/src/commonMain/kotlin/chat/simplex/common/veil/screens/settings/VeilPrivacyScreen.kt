package chat.simplex.common.veil.screens.settings

import androidx.compose.runtime.*
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.chatModel
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Veil privacy screen. The toggles use upstream's preference objects and side effects; what is not grouped here is
 * reachable through "Diğer gizlilik ayarları" (upstream's privacy screen).
 */
@Composable
fun VeilPrivacyScreen(setPerformLA: (Boolean) -> Unit, close: () -> Unit) {
  val applyScreenProtection = rememberScreenProtectionApplier()
  val showPreviews = remember { appPrefs.privacyShowChatPreviews.state }
  val protectScreen = remember { appPrefs.privacyProtectScreen.state }
  val incognito = remember { appPrefs.incognito.state }
  val callsOptIn = remember { appPrefs.veilCallsOptIn.state }
  val performLA = remember { appPrefs.performLA.state }
  val laMode = remember { appPrefs.laMode.state }
  val notifPreviewMode = chatModel.notificationPreviewMode
  val user = chatModel.currentUser.value

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_privacy_title), close) {
    VeilSectionLabel(stringResource(MR.strings.veil_set_privacy_previews))
    VeilGroup {
      VeilSwitchRow(
        stringResource(MR.strings.veil_set_privacy_list_previews),
        checked = showPreviews.value,
        onChange = { on ->
          appPrefs.privacyShowChatPreviews.set(on)
          chatModel.showChatPreviews.value = on
        }
      )
      VeilRowDivider()
      VeilValueRow(
        stringResource(MR.strings.veil_set_privacy_notif_previews), sub = null, readOnly = false,
        value = notificationPreviewModes().firstOrNull { it.value == notifPreviewMode.value }?.title,
        onClick = {
          ModalManager.start.showModalCloseable(true) {
            NotificationPreviewView(notifPreviewMode) { mode ->
              appPrefs.notificationPreviewMode.set(mode.name)
              chatModel.notificationPreviewMode.value = mode
            }
          }
        }
      )
    }

    VeilSectionLabel(stringResource(MR.strings.veil_set_privacy_device))
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_set_privacy_app_lock), sub = null, readOnly = false,
        value = if (performLA.value) laMode.value.text else stringResource(MR.strings.veil_set_off),
        onClick = { showUpstreamSettings { SimplexLockView(chatModel, appPrefs.laMode, setPerformLA) } }
      )
      VeilRowDivider()
      VeilSwitchRow(
        stringResource(MR.strings.veil_set_privacy_screen_protection),
        checked = protectScreen.value,
        onChange = { on ->
          appPrefs.privacyProtectScreen.set(on)
          applyScreenProtection(on)
        }
      )
    }

    VeilSectionLabel(stringResource(MR.strings.veil_set_privacy_connections))
    VeilGroup {
      VeilSwitchRow(
        stringResource(MR.strings.veil_set_privacy_incognito),
        description = stringResource(MR.strings.veil_set_privacy_incognito_desc),
        checked = incognito.value,
        onChange = { appPrefs.incognito.set(it) }
      )
      VeilRowDivider()
      // Link previews are fetched outside the Tor-only message path, so they stay off (VeilNetworkPolicy.LINK_PREVIEWS_ENABLED).
      VeilValueRow(
        stringResource(MR.strings.veil_set_privacy_link_previews),
        sub = stringResource(MR.strings.veil_set_privacy_link_previews_desc),
        readOnly = true,
        value = stringResource(MR.strings.veil_set_off)
      )
    }

    // Calls are off by default; turning them on is confirmed once because call media does not go through Tor.
    VeilSectionLabel(stringResource(MR.strings.veil_calls_section))
    VeilGroup {
      VeilSwitchRow(
        stringResource(MR.strings.veil_calls_toggle),
        description = stringResource(MR.strings.veil_calls_toggle_desc),
        checked = callsOptIn.value,
        onChange = { on ->
          if (!on) {
            appPrefs.veilCallsOptIn.set(false)
          } else {
            AlertManager.shared.showAlertDialog(
              title = generalGetString(MR.strings.veil_calls_optin_title),
              text = generalGetString(MR.strings.veil_calls_optin_text),
              confirmText = generalGetString(MR.strings.veil_calls_optin_confirm),
              dismissText = generalGetString(MR.strings.veil_calls_cancel),
              onConfirm = { appPrefs.veilCallsOptIn.set(true) }
            )
          }
        }
      )
    }

    if (user != null && !chatModel.desktopNoUserNoRemote) {
      VeilSectionLabel(stringResource(MR.strings.veil_set_privacy_receipts))
      VeilGroup {
        VeilSwitchRow(
          stringResource(MR.strings.veil_set_privacy_receipts_contacts),
          checked = user.sendRcptsContacts,
          onChange = { on ->
            withLongRunningApi(slow = 60_000) {
              chatModel.controller.apiSetUserContactReceipts(user, UserMsgReceiptSettings(on, clearOverrides = false))
              appPrefs.privacyDeliveryReceiptsSet.set(true)
              chatModel.currentUser.value = user.copy(sendRcptsContacts = on)
            }
          }
        )
        VeilRowDivider()
        VeilSwitchRow(
          stringResource(MR.strings.veil_set_privacy_receipts_groups),
          checked = user.sendRcptsSmallGroups,
          onChange = { on ->
            withLongRunningApi(slow = 60_000) {
              chatModel.controller.apiSetUserGroupReceipts(user, UserMsgReceiptSettings(on, clearOverrides = false))
              appPrefs.privacyDeliveryReceiptsSet.set(true)
              chatModel.currentUser.value = user.copy(sendRcptsSmallGroups = on)
            }
          }
        )
      }
      VeilNote(stringResource(MR.strings.veil_set_privacy_receipts_note))
    }

    VeilSectionLabel("")
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_set_privacy_more), sub = null, readOnly = false,
        onClick = {
          showUpstreamSettings {
            PrivacySettingsView(
              chatModel,
              showSettingsModal = { modalView -> { ModalManager.start.showModal(settings = true, cardScreen = true) { modalView(chatModel) } } },
              setPerformLA = setPerformLA
            )
          }
        }
      )
    }
  }
}
