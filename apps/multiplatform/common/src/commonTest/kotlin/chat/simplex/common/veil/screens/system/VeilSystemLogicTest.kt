package chat.simplex.common.veil.screens.system

import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.MTRError
import chat.simplex.common.views.helpers.MigrationError
import chat.simplex.common.views.helpers.UpMigration
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.*'
class VeilSystemLogicTest {
  @Test
  fun destructiveConfirmIsDangerOthersPrimary() {
    assertEquals(VeilAlertRole.Danger, veilConfirmRole(true))
    assertEquals(VeilAlertRole.Primary, veilConfirmRole(false))
  }

  @Test
  fun twoShortLabelsShareARowOtherwiseStack() {
    assertEquals(VeilAlertLayout.Row, veilAlertLayout(listOf("Sil", "Vazgeç")))
    assertEquals(VeilAlertLayout.Column, veilAlertLayout(listOf("Tamam")))
    assertEquals(VeilAlertLayout.Column, veilAlertLayout(listOf("A", "B", "C")))
    assertEquals(VeilAlertLayout.Column, veilAlertLayout(listOf("Parolayı kaydet ve aç", "İptal")))
  }

  @Test
  fun okAndNoErrorMeansNoErrorScreen() {
    assertEquals(VeilDbError.None, veilDbError(null, false, false))
    assertEquals(VeilDbError.None, veilDbError(DBMigrationResult.OK, true, true))
    assertTrue(veilDbOptions(VeilDbError.None, restoreAvailable = true).isEmpty())
  }

  @Test
  fun notADatabaseFollowsKeystoreState() {
    val s = DBMigrationResult.ErrorNotADatabase("chat.db")
    assertEquals(VeilDbError.WrongPassphrase, veilDbError(s, useKeychain = true, storedKeyPresent = true))
    assertEquals(VeilDbError.PassphraseRequiredStore, veilDbError(s, useKeychain = true, storedKeyPresent = false))
    assertEquals(VeilDbError.PassphraseRequired, veilDbError(s, useKeychain = false, storedKeyPresent = true))
  }

  @Test
  fun migrationErrorsMapToTheirScreens() {
    assertEquals(VeilDbError.Upgrade, veilDbError(DBMigrationResult.ErrorMigration("f", MigrationError.Upgrade(listOf(UpMigration("m1")))), false, false))
    assertEquals(VeilDbError.Downgrade, veilDbError(DBMigrationResult.ErrorMigration("f", MigrationError.Downgrade(listOf("m1"))), false, false))
    assertEquals(VeilDbError.IncompatibleVersion, veilDbError(DBMigrationResult.ErrorMigration("f", MigrationError.Error(MTRError.NoDown(listOf("m1")))), false, false))
    assertEquals(VeilDbError.DatabaseError, veilDbError(DBMigrationResult.ErrorSQL("f", "x"), false, false))
    assertEquals(VeilDbError.Keychain, veilDbError(DBMigrationResult.ErrorKeychain, false, false))
    assertEquals(VeilDbError.InvalidConfirmation, veilDbError(DBMigrationResult.InvalidConfirmation, false, false))
    assertEquals(VeilDbError.Unknown, veilDbError(DBMigrationResult.Unknown("{}"), false, false))
  }

  @Test
  fun everyStateKeepsUpstreamsActions() {
    // passphrase entry always offers a way to submit it
    assertContains(veilDbOptions(VeilDbError.WrongPassphrase, false), VeilDbOption.SaveAndOpen)
    assertContains(veilDbOptions(VeilDbError.PassphraseRequiredStore, false), VeilDbOption.SaveAndOpen)
    assertContains(veilDbOptions(VeilDbError.PassphraseRequired, false), VeilDbOption.Open)
    for (k in listOf(VeilDbError.WrongPassphrase, VeilDbError.PassphraseRequiredStore, VeilDbError.PassphraseRequired)) {
      assertContains(veilDbOptions(k, false), VeilDbOption.PassphraseField)
    }
    assertContains(veilDbOptions(VeilDbError.Upgrade, false), VeilDbOption.UpgradeAndOpen)
    assertContains(veilDbOptions(VeilDbError.Downgrade, false), VeilDbOption.DowngradeAndOpen)
    assertContains(veilDbOptions(VeilDbError.Downgrade, false), VeilDbOption.DowngradeWarning)
    assertContains(veilDbOptions(VeilDbError.DatabaseError, false), VeilDbOption.ErrorInfo)
    assertContains(veilDbOptions(VeilDbError.Unknown, false), VeilDbOption.ErrorInfo)
  }

  @Test
  fun restoreIsOfferedInEveryErrorStateWhenAvailable() {
    for (k in VeilDbError.values().filter { it != VeilDbError.None }) {
      assertContains(veilDbOptions(k, true), VeilDbOption.RestoreBackup, "state $k")
      assertFalse(VeilDbOption.RestoreBackup in veilDbOptions(k, false), "state $k")
    }
  }

  @Test
  fun stoppedWinsThenNoTor() {
    assertEquals(VeilShellNotice.Stopped, veilShellNotice(false, TorRingState.Unavailable))
    assertEquals(VeilShellNotice.Stopped, veilShellNotice(false, TorRingState.ProxyAnswers))
    assertEquals(VeilShellNotice.TorDown, veilShellNotice(true, TorRingState.Unavailable))
    assertEquals(VeilShellNotice.TorDown, veilShellNotice(null, TorRingState.Unavailable))
    assertEquals(VeilShellNotice.None, veilShellNotice(true, TorRingState.ProxyAnswers))
    assertEquals(VeilShellNotice.None, veilShellNotice(null, TorRingState.Connecting))
  }

  @Test
  fun passcodeEntryIsBounded() {
    assertEquals("12", veilPasscodeAppend("1", 2))
    assertEquals("1234567890123456", veilPasscodeAppend("1234567890123456", 7))
    assertEquals("1", veilPasscodeAppend("1", 12))
    assertEquals(4, veilPasscodeSlots(0))
    assertEquals(9, veilPasscodeSlots(9))
    assertEquals(16, veilPasscodeSlots(40))
  }

  @Test
  fun submitNeedsFourDigitsAllowedCodeAndEnabledButtons() {
    assertFalse(veilPasscodeSubmitEnabled("123", null, true))
    assertTrue(veilPasscodeSubmitEnabled("1234", null, true))
    assertTrue(veilPasscodeSubmitEnabled("1234", true, true))
    assertFalse(veilPasscodeSubmitEnabled("1234", false, true))
    assertFalse(veilPasscodeSubmitEnabled("1234", null, false))
  }
}
