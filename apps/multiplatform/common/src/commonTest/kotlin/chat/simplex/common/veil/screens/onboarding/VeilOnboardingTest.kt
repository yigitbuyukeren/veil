package chat.simplex.common.veil.screens.onboarding

import chat.simplex.common.model.ServerOperator
import chat.simplex.common.model.ServerRoles
import chat.simplex.common.veil.data.defaultOnboardingOperatorIds
import chat.simplex.common.veil.data.operatorsWithRequiredRoles
import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.views.onboarding.OnboardingStage
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.screens.onboarding.*'
class VeilOnboardingTest {
  @Test
  fun everyStageMapsToAStep() {
    val expected = mapOf(
      OnboardingStage.Step1_SimpleXInfo to VeilOnbStep.Intro,
      OnboardingStage.Step2_CreateProfile to VeilOnbStep.Name,
      OnboardingStage.Step3_ChooseServerOperators to VeilOnbStep.Notifications,
      OnboardingStage.Step3_CreateSimpleXAddress to VeilOnbStep.Notifications,
      OnboardingStage.Step4_SetNotificationsMode to VeilOnbStep.Notifications,
      OnboardingStage.Step4_NetworkCommitments to VeilOnbStep.Conditions,
      OnboardingStage.Step2_5_SetupDatabasePassphrase to VeilOnbStep.Upstream,
      OnboardingStage.LinkAMobile to VeilOnbStep.Upstream,
    )
    for (stage in OnboardingStage.values()) {
      if (stage == OnboardingStage.OnboardingComplete) assertNull(veilOnbStepFor(stage))
      else assertEquals(expected[stage], veilOnbStepFor(stage), "stage $stage")
    }
  }

  @Test
  fun stepsFollowTheUpstreamStageOrder() {
    // Resuming must never go backwards past a step that already changed the core state.
    val order = listOf(
      OnboardingStage.Step1_SimpleXInfo, OnboardingStage.Step2_CreateProfile,
      OnboardingStage.Step3_ChooseServerOperators, OnboardingStage.Step4_NetworkCommitments,
    ).map { veilOnbStepFor(it)!! }
    assertEquals(listOf(VeilOnbStep.Intro, VeilOnbStep.Name, VeilOnbStep.Notifications, VeilOnbStep.Conditions), order)
  }

  @Test
  fun storyPages() {
    assertNull(storyPageBefore(0))
    assertEquals(0, storyPageBefore(1))
    assertEquals(1, storyPageAfter(0))
    assertEquals(2, storyPageAfter(1))
    assertNull(storyPageAfter(STORY_PAGES - 1))
  }

  @Test
  fun orbotCheckStartsWaitingAndCannotContinue() {
    val s = OrbotCheckState()
    assertEquals(OrbotPhase.Waiting, s.phase)
    assertFalse(s.canContinue)
    assertEquals(TorRingState.Connecting, s.ringState)
  }

  @Test
  fun orbotCheckFollowsTheProbe() {
    val answers = ArrayDeque(listOf(false, false, true, true, false))
    val check = OrbotCheck { answers.removeFirst() }
    val seen = (1..5).map { check.tick() }
    assertEquals(listOf(false, false, true, true, false), seen.map { it.answered })
    assertEquals(listOf(false, false, true, true, false), seen.map { it.canContinue })
    assertEquals(listOf(1, 2, 3, 4, 5), seen.map { it.probes })
    assertEquals(TorRingState.ProxyAnswers, seen[2].ringState)
    assertEquals(TorRingState.Connecting, seen[4].ringState)
  }

  @Test
  fun orbotCheckTreatsProbeFailureAsNoAnswer() {
    val check = OrbotCheck { throw IllegalStateException("boom") }
    val s = check.tick()
    assertEquals(OrbotPhase.Waiting, s.phase)
    assertFalse(s.canContinue)
  }

  private fun op(id: Long, enabled: Boolean, roles: ServerRoles) =
    ServerOperator.sampleData1.copy(operatorId = id, enabled = enabled, smpRoles = roles, xftpRoles = roles)

  private val all = ServerRoles(storage = true, proxy = true, names = false)
  private val none = ServerRoles(storage = false, proxy = false, names = false)

  @Test
  fun defaultOperatorIdsAreTheEnabledOnes() {
    val ops = listOf(op(1, true, all), op(2, false, all), op(3, true, all))
    assertEquals(setOf(1L, 3L), defaultOnboardingOperatorIds(ops))
  }

  @Test
  fun operatorsAreEnabledBySelection() {
    val ops = listOf(op(1, false, all), op(2, true, all))
    val r = operatorsWithRequiredRoles(ops, setOf(1L))!!
    assertEquals(listOf(true, false), r.map { it.enabled })
  }

  @Test
  fun missingRolesAreGrantedToTheFirstEnabledOperator() {
    val ops = listOf(op(1, true, none), op(2, true, none))
    val r = operatorsWithRequiredRoles(ops, setOf(1L, 2L))!!
    assertTrue(r[0].smpRoles.storage && r[0].smpRoles.proxy && r[0].xftpRoles.storage && r[0].xftpRoles.proxy)
    assertFalse(r[1].smpRoles.storage)
  }

  @Test
  fun nothingToEnableGivesNull() {
    assertNull(operatorsWithRequiredRoles(emptyList(), setOf(1L)))
    assertNull(operatorsWithRequiredRoles(listOf(op(1, true, none)), emptySet()))
  }
}
