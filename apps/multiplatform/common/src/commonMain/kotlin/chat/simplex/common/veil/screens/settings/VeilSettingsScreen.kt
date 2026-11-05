package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.VeilUi
import chat.simplex.common.veil.data.rememberTorRingState
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.avatarSeed
import chat.simplex.common.views.database.DatabaseView
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/** True when settings open as the Veil screens: Android with `VeilUi.settings` on. */
fun veilSettingsActive(): Boolean = VeilUi.settings && appPlatform.isAndroid

/** Opens the Veil settings home on the start side of the screen. */
fun veilOpenSettings(setPerformLA: (Boolean) -> Unit) {
  ModalManager.start.showCustomModal { close -> VeilSettingsHome(setPerformLA, close) }
}

/** Opens a Veil settings sub-screen as its own modal; [content] gets the close function. */
internal fun veilShowSettingsPage(content: @Composable (close: () -> Unit) -> Unit) {
  ModalManager.start.showCustomModal { close -> content(close) }
}

/** Opens an upstream settings screen in a card modal, the way upstream's own settings list does. */
internal fun showUpstreamSettings(content: @Composable () -> Unit) {
  ModalManager.start.showModal(settings = true, cardScreen = true) { content() }
}

internal fun renderSummary(p: SummaryPart): String = when (p.key) {
  SummaryKey.Profiles -> String.format(generalGetString(MR.strings.veil_set_sum_profiles), p.arg ?: "0")
  SummaryKey.ProfilesHidden -> String.format(generalGetString(MR.strings.veil_set_sum_profiles_hidden), p.arg ?: "0")
  SummaryKey.PreviewsOn -> generalGetString(MR.strings.veil_set_sum_previews_on)
  SummaryKey.PreviewsOff -> generalGetString(MR.strings.veil_set_sum_previews_off)
  SummaryKey.ScreenProtected -> generalGetString(MR.strings.veil_set_sum_screen)
  SummaryKey.AppLock -> generalGetString(MR.strings.veil_set_sum_lock)
  SummaryKey.NotificationPreviewHidden -> generalGetString(MR.strings.veil_set_sum_notif_hidden)
  SummaryKey.NotificationsOff -> generalGetString(MR.strings.veil_set_sum_notifications_off)
  SummaryKey.NotificationsPeriodic -> generalGetString(MR.strings.veil_set_sum_notifications_periodic)
  SummaryKey.NotificationsService -> generalGetString(MR.strings.veil_set_sum_notifications_service)
  SummaryKey.ThemeNight -> generalGetString(MR.strings.veil_set_sum_theme)
  SummaryKey.DbEncrypted -> generalGetString(MR.strings.veil_set_sum_db_encrypted)
  SummaryKey.DbPlain -> generalGetString(MR.strings.veil_set_sum_db_plain)
  SummaryKey.DbUnknown -> generalGetString(MR.strings.veil_set_sum_db_unknown)
  SummaryKey.Version -> String.format(generalGetString(MR.strings.veil_set_sum_version), p.arg ?: "")
}

private fun appVersionName(): String = chat.simplex.common.platform.appVersionInfo.first

/** Settings home: profile card and the seven rows of the prototype. */
@Composable
fun VeilSettingsHome(setPerformLA: (Boolean) -> Unit, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val stopped = chatModel.chatRunning.value == false
  KeyChangeEffect(chatModel.updatingProgress.value != null) { close() }

  val user = chatModel.currentUser.value
  val users = chatModel.users
  val hiddenCount = users.count { it.user.hidden }
  val notificationsMode = remember { appPrefs.notificationsMode.state }.value
  val showPreviews = chatModel.showChatPreviews.value
  val screenProtected = remember { appPrefs.privacyProtectScreen.state }.value
  val appLock = remember { appPrefs.performLA.state }.value
  val notifPreview = chatModel.notificationPreviewMode.value
  val ring = rememberTorRingState()

  fun summary(parts: List<SummaryPart>) = parts.joinSummary(::renderSummary)

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_title), close) {
    VeilCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val name = user?.displayName ?: ""
        MaskAvatar(avatarSeed("u${user?.userId ?: 0}", name), 60.dp)
        Column(Modifier.weight(1f)) {
          BasicText(name, style = type.sheetTitle.copy(color = colors.ink), maxLines = 1)
          BasicText(stringResource(MR.strings.veil_set_profile_sub), style = type.caption.copy(color = colors.ink3))
        }
      }
    }

    VeilGroup {
      VeilNavRow(
        MR.images.ic_manage_accounts, stringResource(MR.strings.veil_set_row_profiles),
        summary(profilesSummary(users.size, hiddenCount)),
        onClick = ::openProfiles, enabled = !stopped
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_visibility_off, stringResource(MR.strings.veil_set_row_privacy),
        summary(privacySummary(showPreviews, screenProtected, appLock, notifPreview)),
        onClick = { veilShowSettingsPage { c -> VeilPrivacyScreen(setPerformLA, c) } }, enabled = !stopped
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_notifications, stringResource(MR.strings.veil_set_row_notifications),
        summary(notificationsSummary(notificationsMode)),
        onClick = { showUpstreamSettings { NotificationsSettingsView(chatModel) } }, enabled = !stopped
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_palette, stringResource(MR.strings.veil_set_row_appearance),
        summary(appearanceSummary()),
        onClick = { veilShowSettingsPage { c -> VeilAppearanceScreen(c) } }
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_database, stringResource(MR.strings.veil_set_row_database),
        summary(databaseSummary(chatModel.chatDbEncrypted.value)),
        onClick = { showUpstreamSettings { DatabaseView() } }
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_wifi_tethering, stringResource(MR.strings.veil_set_row_tor),
        torRingLabel(ring),
        onClick = { veilOpenTorScreen() }
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_info, stringResource(MR.strings.veil_set_row_about),
        summary(aboutSummary(appVersionName())),
        onClick = { veilShowSettingsPage { c -> VeilAboutScreen(c) } }
      )
    }

    Spacer(Modifier.height(16.dp))
    VeilGroup {
      VeilNavRow(
        MR.images.ic_code, stringResource(MR.strings.veil_set_row_advanced),
        stringResource(MR.strings.veil_adv_sum_advanced),
        onClick = { veilShowSettingsPage { c -> VeilAdvancedScreen(c) } }
      )
    }
    BasicText(
      String.format(stringResource(MR.strings.veil_set_footer), appVersionName()),
      Modifier.fillMaxWidth().padding(vertical = 18.dp),
      style = type.caption.copy(color = colors.ink3, textAlign = TextAlign.Center),
    )
  }
}

/** Profiles: upstream's profile list (switch, create, hide, delete), same wrapper as the user picker uses. */
private fun openProfiles() {
  ModalManager.start.showCustomModal(keyboardCoversBar = false) { close ->
    val search = rememberSaveable { mutableStateOf("") }
    val profileHidden = rememberSaveable { mutableStateOf(false) }
    val authorized = remember { stateGetOrPut("authorized") { false } }
    ModalView(
      { close() },
      showSearch = true,
      searchAlwaysVisible = true,
      onSearchValueChanged = { search.value = it },
      content = {
        UserProfilesView(chatModel, search, profileHidden) { block ->
          if (authorized.value) {
            block()
          } else {
            doWithAuth(
              generalGetString(MR.strings.auth_open_chat_profiles),
              generalGetString(MR.strings.auth_log_in_using_credential)
            ) {
              authorized.value = true
              block()
            }
          }
        }
      })
  }
}
