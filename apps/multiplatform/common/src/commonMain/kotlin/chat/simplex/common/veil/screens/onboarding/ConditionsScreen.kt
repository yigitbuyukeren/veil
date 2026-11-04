package chat.simplex.common.veil.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatModel
import chat.simplex.common.veil.data.acceptOnboardingConditions
import chat.simplex.common.veil.data.defaultOnboardingOperatorIds
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.chat.item.openBrowserAlert
import chat.simplex.common.views.onboarding.prepareChatBeforeFinishingOnboarding
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

const val SIMPLEX_PRIVACY_URL = "https://simplex.chat/privacy/"

/**
 * Network conditions. Accepting the conditions of use for the preset operators is required by the relay terms and is
 * upstream's own flow (`acceptOnboardingConditions` mirrors upstream `acceptConditions`); there is no network
 * connection before this is accepted. The conditions text is linked, not fetched: the link opens through the
 * external-link warning (it leaves Tor).
 */
@Composable
internal fun ConditionsScreen(chatModel: ChatModel) {
  val colors = VeilTheme.colors
  val uriHandler = LocalUriHandler.current
  LaunchedEffect(Unit) { prepareChatBeforeFinishingOnboarding() }
  val operators by remember { derivedStateOf { chatModel.conditions.value.serverOperators } }
  val selected = defaultOnboardingOperatorIds(operators)
  var accepting by remember { mutableStateOf(false) }
  LaunchedEffect(accepting) {
    // A failed accept shows an alert from the controller; let the person try again afterwards.
    if (accepting) {
      delay(4000)
      accepting = false
    }
  }

  OnbScaffold(
    onBack = null,
    bottom = {
      VeilButton(
        stringResource(MR.strings.veil_onb_terms_accept),
        onClick = {
          accepting = true
          acceptOnboardingConditions(selected, onFailed = { accepting = false })
        },
        enabled = selected.isNotEmpty() && !accepting,
      )
    },
  ) {
    Spacer(Modifier.height(18.dp))
    OnbHeadline(stringResource(MR.strings.veil_onb_terms_title))
    Spacer(Modifier.height(14.dp))
    VeilCard(Modifier.fillMaxWidth()) {
      BasicText(stringResource(MR.strings.veil_onb_terms_independent), style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
    }
    Spacer(Modifier.height(14.dp))
    VeilCard(Modifier.fillMaxWidth(), layer = VeilLayer.Mid) {
      BasicText(stringResource(MR.strings.veil_onb_terms_body), style = VeilTheme.type.body.copy(color = colors.ink2))
      Spacer(Modifier.height(12.dp))
      BasicText(
        stringResource(MR.strings.veil_onb_terms_link),
        Modifier.clickable(role = Role.Button) { openBrowserAlert(SIMPLEX_PRIVACY_URL, uriHandler) }.padding(vertical = 4.dp),
        style = VeilTheme.type.body.copy(color = colors.mintInk, textDecoration = TextDecoration.Underline)
      )
    }
    Spacer(Modifier.height(14.dp))
    OnbNote(stringResource(MR.strings.veil_onb_terms_calls), Modifier.padding(horizontal = 4.dp))
    if (selected.isEmpty()) {
      Spacer(Modifier.height(10.dp))
      OnbNote(stringResource(MR.strings.veil_onb_terms_loading), Modifier.padding(horizontal = 4.dp))
    }
  }
}
