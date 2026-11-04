package chat.simplex.common.veil.screens.onboarding

import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.views.onboarding.OnboardingStage

/**
 * Veil onboarding steps, mapped onto the persisted upstream [OnboardingStage] (no parallel state machine):
 *
 * | OnboardingStage                                         | Veil step                         |
 * |---------------------------------------------------------|-----------------------------------|
 * | Step1_SimpleXInfo                                       | [Intro]: story, then Orbot setup  |
 * | Step2_CreateProfile                                     | [Name]                            |
 * | Step3_ChooseServerOperators / Step3_CreateSimpleXAddress / Step4_SetNotificationsMode | [Notifications] |
 * | Step4_NetworkCommitments                                | [Conditions]                      |
 * | Step2_5_SetupDatabasePassphrase, LinkAMobile            | [Upstream] (desktop-only stages)  |
 * | OnboardingComplete                                      | none                              |
 *
 * The story page and the Orbot check are not persisted: after a process restart the flow resumes at the start of the
 * step the stage belongs to (the story), which is cheap because both only read state.
 */
enum class VeilOnbStep { Intro, Name, Notifications, Conditions, Upstream }

fun veilOnbStepFor(stage: OnboardingStage): VeilOnbStep? = when (stage) {
  OnboardingStage.OnboardingComplete -> null
  OnboardingStage.Step1_SimpleXInfo -> VeilOnbStep.Intro
  OnboardingStage.Step2_CreateProfile -> VeilOnbStep.Name
  OnboardingStage.Step3_ChooseServerOperators,
  OnboardingStage.Step3_CreateSimpleXAddress,
  OnboardingStage.Step4_SetNotificationsMode -> VeilOnbStep.Notifications
  OnboardingStage.Step4_NetworkCommitments -> VeilOnbStep.Conditions
  OnboardingStage.Step2_5_SetupDatabasePassphrase,
  OnboardingStage.LinkAMobile -> VeilOnbStep.Upstream
}

/** Sub-pages of [VeilOnbStep.Intro]. */
enum class IntroPhase { Story, Orbot }

const val STORY_PAGES = 3

/** The page before [page] in the story, or null on the first page. */
fun storyPageBefore(page: Int): Int? = if (page > 0) page - 1 else null

/** The page after [page] in the story, or null on the last page (the story is done). */
fun storyPageAfter(page: Int): Int? = if (page < STORY_PAGES - 1) page + 1 else null

/** How often the local Tor SOCKS proxy is probed while the Orbot screen is open. */
const val ORBOT_POLL_INTERVAL_MS = 2000L

enum class OrbotPhase { Waiting, Answered }

/**
 * What the Orbot screen knows. It only ever knows whether the local SOCKS proxy answered a greeting; it does not know
 * whether Tor has built circuits, and nothing here may be presented as "Tor is healthy".
 */
data class OrbotCheckState(val phase: OrbotPhase = OrbotPhase.Waiting, val probes: Int = 0) {
  val answered: Boolean get() = phase == OrbotPhase.Answered
  /** "Devam" is enabled only while the proxy answers. */
  val canContinue: Boolean get() = answered
  val ringState: TorRingState get() = if (answered) TorRingState.ProxyAnswers else TorRingState.Connecting
}

/** Probes through [probe] (blocking; call off the main thread). A proxy that stops answering returns the state to Waiting. */
class OrbotCheck(private val probe: () -> Boolean) {
  var state: OrbotCheckState = OrbotCheckState()
    private set

  fun tick(): OrbotCheckState {
    val ok = try { probe() } catch (e: Exception) { false }
    state = OrbotCheckState(if (ok) OrbotPhase.Answered else OrbotPhase.Waiting, state.probes + 1)
    return state
  }
}
