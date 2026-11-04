package chat.simplex.common.veil.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.createProfileOnboarding
import chat.simplex.common.views.mkValidName
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.views.onboarding.setLastVersionDefault
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

private const val NAME_MAX_LENGTH = 28

/**
 * First profile. Creation is upstream's `createProfileOnboarding` (creates the user through the controller and moves
 * the persisted stage on); this screen only collects the name and shows the mask derived from it.
 */
@Composable
internal fun NameScreen(chatModel: ChatModel) {
  var name by rememberSaveable { mutableStateOf("") }
  var creating by remember { mutableStateOf(false) }
  val trimmed = name.trim()
  // Same rule as upstream's first-profile screen: the name must survive the core's name cleanup unchanged.
  val valid = remember(trimmed) { trimmed.isNotEmpty() && mkValidName(trimmed) == trimmed }
  val focus = remember { FocusRequester() }

  fun back() {
    // Same as upstream's onboardingBackAction: back to the first step while there is no visible profile.
    if (chatModel.users.none { !it.user.hidden }) appPrefs.onboardingStage.set(OnboardingStage.Step1_SimpleXInfo)
  }

  fun submit() {
    if (!valid || creating) return
    creating = true
    createProfileOnboarding(chatModel, trimmed, close = {})
  }

  BackHandler { back() }
  LaunchedEffect(Unit) {
    setLastVersionDefault(chatModel)
    delay(300)
    try { focus.requestFocus() } catch (e: Exception) {}
  }
  // createProfileOnboarding reports nothing on failure (the controller shows an alert); allow another try then.
  LaunchedEffect(creating) {
    if (creating) {
      delay(3000)
      creating = false
    }
  }

  OnbScaffold(
    onBack = ::back,
    bottom = {
      VeilButton(stringResource(MR.strings.veil_onb_continue), onClick = ::submit, enabled = valid && !creating)
    },
  ) {
    Spacer(Modifier.height(6.dp))
    OnbHeadline(stringResource(MR.strings.veil_onb_name_title))
    Spacer(Modifier.height(10.dp))
    OnbBody(stringResource(MR.strings.veil_onb_name_text))
    Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
      MaskAvatar(trimmed.ifEmpty { stringResource(MR.strings.veil_onb_name_preview_empty) }, 120.dp)
    }
    VeilTextField(
      value = name,
      onValueChange = { name = it.take(NAME_MAX_LENGTH) },
      modifier = Modifier.focusRequester(focus),
      placeholder = stringResource(MR.strings.veil_onb_name_hint),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { submit() }),
    )
    if (trimmed.isNotEmpty() && !valid) {
      BasicText(
        stringResource(MR.strings.veil_onb_name_invalid),
        Modifier.padding(start = 4.dp, top = 8.dp),
        style = VeilTheme.type.caption.copy(color = VeilTheme.colors.warn)
      )
    }
    OnbNote(stringResource(MR.strings.veil_onb_name_note), Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
  }
}
