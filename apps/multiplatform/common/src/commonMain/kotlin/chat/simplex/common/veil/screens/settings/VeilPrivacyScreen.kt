package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.screens.chat.VeilChoiceRow
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Veil privacy screen. The toggles use upstream's preference objects and side effects; what is not grouped here is
 * reachable through "Diğer gizlilik ayarları" ([VeilMorePrivacyScreen]).
 */
@Composable
fun VeilPrivacyScreen(setPerformLA: (Boolean) -> Unit, close: () -> Unit) {
  val applyScreenProtection = rememberScreenProtectionApplier()
  val showPreviews = remember { appPrefs.privacyShowChatPreviews.state }
  val protectScreen = remember { appPrefs.privacyProtectScreen.state }
  val incognito = remember { appPrefs.incognito.state }
  val callsOptIn = remember { appPrefs.veilCallsOptIn.state }
  var syncingCalls by remember { mutableStateOf(false) }
  val performLA = remember { appPrefs.performLA.state }
  val laMode = remember { appPrefs.laMode.state }
  val lockDelay = remember { appPrefs.laLockDelay.state }
  val notifPreviewMode = chatModel.notificationPreviewMode
  val user = chatModel.currentUser.value

  fun setCallsEnabled(enabled: Boolean) {
    appPrefs.veilCallsOptIn.set(enabled)
    syncingCalls = true
    withBGApi {
      try {
        VeilCallPreferences.syncCurrent(chatModel.controller, chatModel.remoteHostId())
      } finally {
        syncingCalls = false
      }
    }
  }

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
      // The lock itself (on/off, mode, passcode screens, authentication) is upstream's lock page; the self-destruct section of it is reached only from "Gelişmiş".
      VeilValueRow(
        stringResource(MR.strings.veil_set_privacy_app_lock), sub = null, readOnly = false,
        value = when (appLockState(performLA.value, laMode.value == LAMode.PASSCODE)) {
          AppLockState.Off -> stringResource(MR.strings.veil_set_off)
          AppLockState.System -> stringResource(MR.strings.veil_lock_system)
          AppLockState.Passcode -> stringResource(MR.strings.veil_lock_passcode)
        },
        onClick = { showUpstreamSettings { SimplexLockView(chatModel, appPrefs.laMode, setPerformLA, showSelfDestruct = false) } }
      )
      if (lockDelayShown(performLA.value)) {
        VeilRowDivider()
        VeilChoiceRow(
          title = stringResource(MR.strings.veil_lock_delay),
          options = lockDelayChoices(lockDelay.value).map { it to veilLockDelayLabel(it) },
          selected = lockDelay.value,
          onSelect = { appPrefs.laLockDelay.set(it) },
          modifier = Modifier.padding(horizontal = 8.dp),
        )
      }
      VeilRowDivider()
      VeilSwitchRow(
        stringResource(MR.strings.veil_set_privacy_screen_protection),
        checked = protectScreen.value,
        onChange = { on ->
          if (screenProtectionOffNeedsWarning(on)) {
            AlertManager.shared.showAlertDialog(
              title = generalGetString(MR.strings.veil_screen_off_title),
              text = generalGetString(MR.strings.veil_screen_off_text),
              confirmText = generalGetString(MR.strings.veil_screen_off_confirm),
              destructive = true,
              onConfirm = {
                appPrefs.privacyProtectScreen.set(false)
                applyScreenProtection(false)
              }
            )
          } else {
            appPrefs.privacyProtectScreen.set(true)
            applyScreenProtection(true)
          }
        }
      )
    }
    VeilNote(stringResource(MR.strings.veil_lock_note))

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
        enabled = !syncingCalls,
        onChange = { on ->
          if (!on) {
            setCallsEnabled(false)
          } else {
            AlertManager.shared.showAlertDialog(
              title = generalGetString(MR.strings.veil_calls_optin_title),
              text = generalGetString(MR.strings.veil_calls_optin_text),
              confirmText = generalGetString(MR.strings.veil_calls_optin_confirm),
              dismissText = generalGetString(MR.strings.veil_calls_cancel),
              onConfirm = { setCallsEnabled(true) }
            )
          }
        }
      )
    }

    VeilNote(stringResource(MR.strings.veil_calls_availability_note))

    if (user != null && !chatModel.desktopNoUserNoRemote) {
      VeilSectionLabel(stringResource(MR.strings.veil_set_privacy_receipts))
      VeilGroup {
        VeilSwitchRow(
          stringResource(MR.strings.veil_set_privacy_receipts_contacts),
          checked = user.sendRcptsContacts,
          onChange = { on -> veilSetReceipts(user, forContacts = true, enable = on) }
        )
        VeilRowDivider()
        VeilSwitchRow(
          stringResource(MR.strings.veil_set_privacy_receipts_groups),
          checked = user.sendRcptsSmallGroups,
          onChange = { on -> veilSetReceipts(user, forContacts = false, enable = on) }
        )
      }
      VeilNote(stringResource(MR.strings.veil_set_privacy_receipts_note))
    }

    VeilSectionLabel("")
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_set_privacy_more), sub = null, readOnly = false,
        onClick = { veilShowSettingsPage { c -> VeilMorePrivacyScreen(c) } }
      )
    }
  }
}

/** "Hemen", "30 sn", "1 dk": the text of one lock delay choice. */
@Composable
private fun veilLockDelayLabel(seconds: Int): String = when (lockDelayText(seconds)) {
  LockDelayText.Immediately -> stringResource(MR.strings.veil_lock_delay_now)
  LockDelayText.Seconds -> String.format(stringResource(MR.strings.veil_lock_delay_sec), lockDelayAmount(seconds))
  LockDelayText.Minutes -> String.format(stringResource(MR.strings.veil_lock_delay_min), lockDelayAmount(seconds))
}
