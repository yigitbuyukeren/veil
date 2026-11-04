package chat.simplex.common.veil.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.views.onboarding.prepareChatBeforeFinishingOnboarding
import chat.simplex.common.views.usersettings.changeNotificationsMode
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

private class NtfChoice(val mode: NotificationsMode, val title: StringResource, val text: StringResource)

private val choices = listOf(
  NtfChoice(NotificationsMode.SERVICE, MR.strings.veil_onb_ntf_service_title, MR.strings.veil_onb_ntf_service_text),
  NtfChoice(NotificationsMode.PERIODIC, MR.strings.veil_onb_ntf_periodic_title, MR.strings.veil_onb_ntf_periodic_text),
  NtfChoice(NotificationsMode.OFF, MR.strings.veil_onb_ntf_off_title, MR.strings.veil_onb_ntf_off_text),
)

/**
 * Notification mode. Logic is upstream's: the chat is prepared exactly as `YourNetworkView` does, the choice is applied
 * with `changeNotificationsMode`, and the persisted stage moves to `Step4_NetworkCommitments`.
 * (Server operators are not chosen here; the preset operators stay enabled, as upstream's default.)
 */
@Composable
internal fun NotificationsScreen(chatModel: ChatModel) {
  var mode by rememberSaveable { mutableStateOf(NotificationsMode.default) }
  LaunchedEffect(Unit) { prepareChatBeforeFinishingOnboarding() }

  OnbScaffold(
    onBack = null,
    bottom = {
      VeilButton(
        stringResource(MR.strings.veil_onb_continue),
        onClick = {
          changeNotificationsMode(mode, chatModel)
          appPrefs.onboardingStage.set(OnboardingStage.Step4_NetworkCommitments)
        }
      )
    },
  ) {
    Spacer(Modifier.height(18.dp))
    OnbHeadline(stringResource(MR.strings.veil_onb_ntf_title))
    Spacer(Modifier.height(10.dp))
    OnbBody(stringResource(MR.strings.veil_onb_ntf_text))
    Spacer(Modifier.height(20.dp))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      choices.forEach { c ->
        ChoiceCard(selected = mode == c.mode, title = stringResource(c.title), text = stringResource(c.text)) { mode = c.mode }
      }
    }
    Spacer(Modifier.height(10.dp))
  }
}

@Composable
private fun ChoiceCard(selected: Boolean, title: String, text: String, onSelect: () -> Unit) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.card
  VeilSurface(
    Modifier
      .fillMaxWidth()
      .then(if (selected) Modifier.border(1.5.dp, colors.gradient, shape) else Modifier)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    layer = if (selected) VeilLayer.Mid else VeilLayer.Low,
    border = !selected,
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      BasicText(title, style = VeilTheme.type.rowTitle.copy(color = if (selected) colors.ink else colors.ink))
      BasicText(text, style = VeilTheme.type.body.copy(color = colors.ink2))
    }
  }
}
