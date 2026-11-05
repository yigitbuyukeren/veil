package chat.simplex.common.veil.screens.settings

import kotlin.test.*

class VeilBackupLogicTest {
  @Test
  fun backupNeedsAPassphraseTheOwnerChose() {
    assertEquals(BackupGate.Allowed, backupGate(initialRandomPassphrase = false, chatDbEncrypted = true))
  }

  @Test
  fun theRandomInitialPassphraseDoesNotCount() {
    assertEquals(BackupGate.NeedsPassphrase, backupGate(initialRandomPassphrase = true, chatDbEncrypted = true))
  }

  @Test
  fun anUnencryptedDatabaseIsNotBackedUp() {
    assertEquals(BackupGate.NeedsPassphrase, backupGate(initialRandomPassphrase = false, chatDbEncrypted = false))
    assertEquals(BackupGate.NeedsPassphrase, backupGate(initialRandomPassphrase = true, chatDbEncrypted = false))
  }

  @Test
  fun anUnknownStateIsTreatedAsNoPassphrase() {
    assertEquals(BackupGate.NeedsPassphrase, backupGate(initialRandomPassphrase = false, chatDbEncrypted = null))
  }
}
