package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.platform.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.helpers.annotatedStringResource
import chat.simplex.common.views.usersettings.changeNotificationsMode
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

private fun modeTitle(mode: NotificationsMode): StringResource = when (mode) {
  NotificationsMode.SERVICE -> MR.strings.veil_onb_ntf_service_title
  NotificationsMode.PERIODIC -> MR.strings.veil_onb_ntf_periodic_title
  NotificationsMode.OFF -> MR.strings.veil_onb_ntf_off_title
}

private fun modeText(mode: NotificationsMode): StringResource = when (mode) {
  NotificationsMode.SERVICE -> MR.strings.veil_onb_ntf_service_text
  NotificationsMode.PERIODIC -> MR.strings.veil_onb_ntf_periodic_text
  NotificationsMode.OFF -> MR.strings.veil_onb_ntf_off_text
}

/**
 * Veil notifications screen. Same actions as upstream's `NotificationsSettingsView`: the mode lives in
 * `appPrefs.notificationsMode` and is changed only through `changeNotificationsMode` (preference plus the platform
 * hook). The wording of the three modes is that of the onboarding step.
 */
@Composable
fun VeilNotificationsScreen(close: () -> Unit) {
  val mode = remember { appPrefs.notificationsMode.state }
  val options = remember { notificationModeOptions(appPlatform == AppPlatform.ANDROID) }

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_row_notifications), close) {
    if (options.isEmpty()) {
      VeilNote(stringResource(MR.strings.veil_set_ntf_unavailable))
    } else {
      VeilSectionLabel(stringResource(MR.strings.veil_set_ntf_modes))
      VeilGroup {
        Column(Modifier.selectableGroup()) {
          options.forEachIndexed { i, m ->
            if (i > 0) VeilRowDivider()
            ModeOption(stringResource(modeTitle(m)), stringResource(modeText(m)), mode.value == m) {
              if (shouldChangeNotificationsMode(mode.value, m)) changeNotificationsMode(m, chatModel)
            }
          }
        }
      }
      VeilNote(stringResource(MR.strings.veil_set_ntf_note))
      if (showXiaomiHint(platform.androidIsXiaomiDevice(), mode.value)) {
        BasicText(
          annotatedStringResource(MR.strings.xiaomi_ignore_battery_optimization),
          Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
          style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink2)
        )
      }
    }
  }
}

@Composable
private fun ModeOption(title: String, text: String, selected: Boolean, onSelect: () -> Unit) {
  val colors = VeilTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
      BasicText(text, style = VeilTheme.type.caption.copy(color = colors.ink2))
    }
    if (selected) BasicText("●", style = VeilTheme.type.bodyLarge.copy(color = colors.accent))
  }
}
