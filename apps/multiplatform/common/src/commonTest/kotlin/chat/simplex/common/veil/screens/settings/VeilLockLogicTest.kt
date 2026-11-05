package chat.simplex.common.veil.screens.settings

import kotlin.test.*

class VeilLockLogicTest {
  @Test
  fun lockStateFollowsTheSwitchThenTheMode() {
    assertEquals(AppLockState.Off, appLockState(performLA = false, passcodeMode = false))
    // the stored mode does not matter while the lock is off
    assertEquals(AppLockState.Off, appLockState(performLA = false, passcodeMode = true))
    assertEquals(AppLockState.System, appLockState(performLA = true, passcodeMode = false))
    assertEquals(AppLockState.Passcode, appLockState(performLA = true, passcodeMode = true))
  }

  @Test
  fun delayChoicesAreTheFourStandardOnes() {
    assertEquals(listOf(0, 30, 60, 300), lockDelayChoices(30))
    assertEquals(listOf(0, 30, 60, 300), lockDelayChoices(0))
    assertEquals(listOf(0, 30, 60, 300), lockDelayChoices(300))
  }

  @Test
  fun anUpstreamOnlyDelayIsKeptSoItShowsAsSelected() {
    assertEquals(listOf(0, 10, 30, 60, 300), lockDelayChoices(10))
    assertEquals(listOf(0, 30, 60, 180, 300), lockDelayChoices(180))
    assertEquals(listOf(0, 30, 60, 300, 600), lockDelayChoices(600))
  }

  @Test
  fun delayRowOnlyWithTheLockOn() {
    assertTrue(lockDelayShown(true))
    assertFalse(lockDelayShown(false))
  }

  @Test
  fun delayTexts() {
    assertEquals(LockDelayText.Immediately, lockDelayText(0))
    assertEquals(LockDelayText.Seconds, lockDelayText(30))
    assertEquals(30, lockDelayAmount(30))
    assertEquals(LockDelayText.Minutes, lockDelayText(60))
    assertEquals(1, lockDelayAmount(60))
    assertEquals(5, lockDelayAmount(300))
    // not a whole number of minutes: seconds
    assertEquals(LockDelayText.Seconds, lockDelayText(90))
    assertEquals(90, lockDelayAmount(90))
  }

  @Test
  fun onlyTurningScreenProtectionOffAsksFirst() {
    assertTrue(screenProtectionOffNeedsWarning(false))
    assertFalse(screenProtectionOffNeedsWarning(true))
  }

  @Test
  fun selfDestructNeedsThePasscodeLock() {
    assertEquals(SelfDestructEntry.NeedsPasscodeLock, selfDestructEntry(performLA = false, passcodeMode = true))
    assertEquals(SelfDestructEntry.NeedsPasscodeLock, selfDestructEntry(performLA = true, passcodeMode = false))
    assertEquals(SelfDestructEntry.NeedsPasscodeLock, selfDestructEntry(performLA = false, passcodeMode = false))
    assertEquals(SelfDestructEntry.WarnThenOpen, selfDestructEntry(performLA = true, passcodeMode = true))
  }

  @Test
  fun selfDestructShowsOnlyWhenItCanWork() {
    assertTrue(selfDestructActive(selfDestructPref = true, performLA = true, passcodeMode = true))
    assertFalse(selfDestructActive(selfDestructPref = true, performLA = true, passcodeMode = false))
    assertFalse(selfDestructActive(selfDestructPref = true, performLA = false, passcodeMode = true))
    assertFalse(selfDestructActive(selfDestructPref = false, performLA = true, passcodeMode = true))
  }

  @Test
  fun localFileEncryptionCanBeTurnedOnButNotOff() {
    assertFalse(encryptFilesCanChange(current = true))
    assertTrue(encryptFilesCanChange(current = false))
  }
}
