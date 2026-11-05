package chat.simplex.common.veil.screens.settings

/**
 * Decisions of the Veil backup and restore rows. The archive itself (export, file picker, import, replace
 * confirmation, restart message) is upstream's `databaseManagementActions`; this only decides what the Veil row asks
 * before handing over.
 */

enum class BackupGate {
  /** The database has a passphrase the owner chose: the backup can be taken. */
  Allowed,
  /** No passphrase of the owner's: the backup is not taken; the owner is sent to upstream's passphrase screen. */
  NeedsPassphrase,
}

/**
 * A backup is always protected by a passphrase the owner knows. A database created with the random initial passphrase
 * ([initialRandomPassphrase], kept in the device keystore) does not qualify, nor does an unencrypted one; an unknown
 * state ([chatDbEncrypted] null) is treated the same, to be safe.
 */
fun backupGate(initialRandomPassphrase: Boolean, chatDbEncrypted: Boolean?): BackupGate =
  if (!initialRandomPassphrase && chatDbEncrypted == true) BackupGate.Allowed else BackupGate.NeedsPassphrase
