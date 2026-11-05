package chat.simplex.common.veil.screens.settings

import kotlin.test.*

class VeilDatabaseLogicTest {
  @Test
  fun passphraseIconFollowsUpstream() {
    assertEquals(PassphraseIcon.OpenLock, passphraseIcon(chatDbEncrypted = false, passphraseKept = true))
    assertEquals(PassphraseIcon.OpenLock, passphraseIcon(chatDbEncrypted = false, passphraseKept = false))
    assertEquals(PassphraseIcon.Key, passphraseIcon(chatDbEncrypted = true, passphraseKept = true))
    assertEquals(PassphraseIcon.Lock, passphraseIcon(chatDbEncrypted = true, passphraseKept = false))
    // state not read yet: not shown as unencrypted
    assertEquals(PassphraseIcon.Key, passphraseIcon(chatDbEncrypted = null, passphraseKept = true))
    assertEquals(PassphraseIcon.Lock, passphraseIcon(chatDbEncrypted = null, passphraseKept = false))
  }

  @Test
  fun operationsAreDisabledOnlyWhileSomethingRuns() {
    assertTrue(operationsDisabled(progress = true, desktopNoUserNoRemote = false))
    assertFalse(operationsDisabled(progress = false, desktopNoUserNoRemote = false))
    assertFalse(operationsDisabled(progress = true, desktopNoUserNoRemote = true))
  }

  @Test
  fun ttlNeedsARunningChatAndNoOperation() {
    assertTrue(ttlEnabled(stopped = false, progress = false))
    assertFalse(ttlEnabled(stopped = true, progress = false))
    assertFalse(ttlEnabled(stopped = false, progress = true))
    assertFalse(ttlEnabled(stopped = true, progress = true))
  }

  @Test
  fun messagesSectionIsHiddenWithoutAUser() {
    assertTrue(messagesSectionShown(desktopNoUserNoRemote = false))
    assertFalse(messagesSectionShown(desktopNoUserNoRemote = true))
  }

  @Test
  fun deleteFilesIsDisabledWithoutFilesOrWhileBusy() {
    assertFalse(deleteFilesDisabled(operationsDisabled = false, fileCount = 3))
    assertTrue(deleteFilesDisabled(operationsDisabled = false, fileCount = 0))
    assertTrue(deleteFilesDisabled(operationsDisabled = true, fileCount = 3))
    assertTrue(hasStoredFiles(1))
    assertFalse(hasStoredFiles(0))
  }

  @Test
  fun connectedRemoteHostBlocksTheRunChatSwitch() {
    assertTrue(runChatToggleEnabled(connectedRemoteHosts = 0))
    assertFalse(runChatToggleEnabled(connectedRemoteHosts = 1))
    assertTrue(runChatSwitchEnabled(toggleEnabled = true, progress = false))
    assertFalse(runChatSwitchEnabled(toggleEnabled = true, progress = true))
    assertFalse(runChatSwitchEnabled(toggleEnabled = false, progress = false))
  }

  @Test
  fun disconnectRowsFollowUpstream() {
    assertTrue(disconnectHostsInDatabaseGroup(localUserCreated = null, toggleEnabled = false))
    assertTrue(disconnectHostsInDatabaseGroup(localUserCreated = false, toggleEnabled = false))
    assertFalse(disconnectHostsInDatabaseGroup(localUserCreated = true, toggleEnabled = false))
    assertFalse(disconnectHostsInDatabaseGroup(localUserCreated = null, toggleEnabled = true))
    assertTrue(disconnectHostsInRunChat(toggleEnabled = false))
    assertFalse(disconnectHostsInRunChat(toggleEnabled = true))
  }

  @Test
  fun runChatSectionNeedsALocalUser() {
    assertTrue(runChatSectionShown(true))
    assertFalse(runChatSectionShown(false))
    assertFalse(runChatSectionShown(null))
  }

  @Test
  fun footerAboutTheLatestVersionShowsWhileStopped() {
    assertTrue(recentVersionFooterShown(stopped = true))
    assertFalse(recentVersionFooterShown(stopped = false))
  }

  @Test
  fun switchingOffAsksBeforeStoppingAndSwitchingOnStarts() {
    assertEquals(RunChatAction.Start, runChatAction(switchedOn = true))
    assertEquals(RunChatAction.AskToStop, runChatAction(switchedOn = false))
  }
}
