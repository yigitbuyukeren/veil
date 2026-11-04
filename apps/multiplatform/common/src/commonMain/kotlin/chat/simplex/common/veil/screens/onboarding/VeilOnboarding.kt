package chat.simplex.common.veil.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import chat.simplex.common.model.ChatModel
import chat.simplex.common.veil.design.VeilTheme
import chat.simplex.common.views.onboarding.LinkAMobile
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.views.onboarding.SetupDatabasePassphrase

/**
 * Entry point of the Veil onboarding (Android). [stage] is the persisted `appPrefs.onboardingStage`; [veilOnbStepFor]
 * decides what is shown, so the flow resumes where it stopped after the process died. Every forward move is a change of
 * the persisted stage made by the upstream functions the steps call.
 *
 * The database passphrase step (`Step2_5_SetupDatabasePassphrase`) is not part of the Android flow upstream
 * (only desktop reaches it), so Android keeps the random passphrase; the stage is still handled by showing upstream's
 * screen so that nothing can dead-end.
 */
@Composable
fun VeilOnboarding(chatModel: ChatModel, stage: OnboardingStage) {
  var phase by rememberSaveable { mutableStateOf(IntroPhase.Story) }
  val step = veilOnbStepFor(stage) ?: return
  val fadeMs = VeilTheme.motion.lift(VeilTheme.reduceMotion)
  Box(Modifier.fillMaxSize().background(VeilTheme.colors.night)) {
    Crossfade(targetState = step, animationSpec = tween(fadeMs), label = "onboardingStep") { s ->
      when (s) {
        VeilOnbStep.Intro -> Crossfade(targetState = phase, animationSpec = tween(fadeMs), label = "onboardingIntro") { p ->
          when (p) {
            IntroPhase.Story -> StoryScreen(onDone = { phase = IntroPhase.Orbot })
            IntroPhase.Orbot -> OrbotScreen(
              onBack = { phase = IntroPhase.Story },
              onContinue = { chatModel.controller.appPrefs.onboardingStage.set(OnboardingStage.Step2_CreateProfile) },
            )
          }
        }
        VeilOnbStep.Name -> {
          // Going back from the name lands on the Orbot page, not the start of the story.
          LaunchedEffect(Unit) { phase = IntroPhase.Orbot }
          NameScreen(chatModel)
        }
        VeilOnbStep.Notifications -> NotificationsScreen(chatModel)
        VeilOnbStep.Conditions -> ConditionsScreen(chatModel)
        VeilOnbStep.Upstream -> when (stage) {
          OnboardingStage.LinkAMobile -> LinkAMobile()
          else -> SetupDatabasePassphrase(chatModel)
        }
      }
    }
  }
}
