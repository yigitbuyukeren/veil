package chat.simplex.common.veil.screens.system

import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.MigrationError

// Pure decisions behind the Veil system screens (alerts, database error screen at start, shell notices).

// --- Alert buttons -----------------------------------------------------------------------------------------------

/** How an alert button is drawn: [Primary] = gradient VeilButton, [Secondary] = translucent, [Danger] = red tint. */
enum class VeilAlertRole { Primary, Secondary, Danger }

/** The confirm button is Danger for destructive actions and Primary otherwise. Cancel/dismiss is always Secondary. */
fun veilConfirmRole(destructive: Boolean): VeilAlertRole = if (destructive) VeilAlertRole.Danger else VeilAlertRole.Primary

enum class VeilAlertLayout { Row, Column }

/** Two short buttons sit side by side; anything else (3+ buttons, long labels, one button) is stacked full width. */
fun veilAlertLayout(labels: List<String>): VeilAlertLayout =
  if (labels.size == 2 && labels.all { it.length <= ROW_LABEL_MAX }) VeilAlertLayout.Row else VeilAlertLayout.Column

private const val ROW_LABEL_MAX = 14

// --- Database error screen at start ------------------------------------------------------------------------------

enum class VeilDbError {
  /** A stored passphrase exists but does not open the database. */
  WrongPassphrase,
  /** Encrypted database, passphrase is kept in the keystore: ask for it and store it. */
  PassphraseRequiredStore,
  /** Encrypted database, passphrase is not stored: ask for it and open. */
  PassphraseRequired,
  Upgrade,
  Downgrade,
  IncompatibleVersion,
  DatabaseError,
  Keychain,
  InvalidConfirmation,
  Unknown,
  /** Not an error state (OK or still loading); the error screen is not shown. */
  None,
}

/** Mirrors the branches of upstream's `DatabaseErrorView`. */
fun veilDbError(status: DBMigrationResult?, useKeychain: Boolean, storedKeyPresent: Boolean): VeilDbError = when (status) {
  null, is DBMigrationResult.OK -> VeilDbError.None
  is DBMigrationResult.ErrorNotADatabase ->
    if (useKeychain && storedKeyPresent) VeilDbError.WrongPassphrase
    else if (useKeychain) VeilDbError.PassphraseRequiredStore
    else VeilDbError.PassphraseRequired
  is DBMigrationResult.ErrorMigration -> when (status.migrationError) {
    is MigrationError.Upgrade -> VeilDbError.Upgrade
    is MigrationError.Downgrade -> VeilDbError.Downgrade
    is MigrationError.Error -> VeilDbError.IncompatibleVersion
  }
  is DBMigrationResult.ErrorSQL -> VeilDbError.DatabaseError
  is DBMigrationResult.ErrorKeychain -> VeilDbError.Keychain
  is DBMigrationResult.InvalidConfirmation -> VeilDbError.InvalidConfirmation
  is DBMigrationResult.Unknown -> VeilDbError.Unknown
}

/** Everything upstream's error screen can show or offer. The Veil screen must render each option of its state. */
enum class VeilDbOption {
  PassphraseField, SaveAndOpen, Open, UpgradeAndOpen, DowngradeAndOpen, DowngradeWarning,
  FileName, Migrations, AppVersion, ErrorInfo, RestoreBackup,
}

fun veilDbOptions(kind: VeilDbError, restoreAvailable: Boolean): List<VeilDbOption> {
  val base = when (kind) {
    VeilDbError.WrongPassphrase -> listOf(VeilDbOption.PassphraseField, VeilDbOption.SaveAndOpen, VeilDbOption.FileName)
    VeilDbError.PassphraseRequiredStore -> listOf(VeilDbOption.PassphraseField, VeilDbOption.SaveAndOpen)
    VeilDbError.PassphraseRequired -> listOf(VeilDbOption.PassphraseField, VeilDbOption.Open)
    VeilDbError.Upgrade -> listOf(VeilDbOption.UpgradeAndOpen, VeilDbOption.FileName, VeilDbOption.Migrations, VeilDbOption.AppVersion)
    VeilDbError.Downgrade -> listOf(VeilDbOption.DowngradeAndOpen, VeilDbOption.DowngradeWarning, VeilDbOption.FileName, VeilDbOption.Migrations, VeilDbOption.AppVersion)
    VeilDbError.IncompatibleVersion -> listOf(VeilDbOption.FileName, VeilDbOption.ErrorInfo)
    VeilDbError.DatabaseError -> listOf(VeilDbOption.FileName, VeilDbOption.ErrorInfo)
    VeilDbError.Unknown -> listOf(VeilDbOption.ErrorInfo)
    VeilDbError.Keychain, VeilDbError.InvalidConfirmation, VeilDbError.None -> emptyList()
  }
  return if (restoreAvailable && kind != VeilDbError.None) base + VeilDbOption.RestoreBackup else base
}

// --- Notices shown by the app shell ----------------------------------------------------------------------------

enum class VeilShellNotice { None, Stopped, TorDown }

/** Chat stopped wins (the user can start it again); otherwise no Tor shows the Tor notice; otherwise nothing. */
fun veilShellNotice(chatRunning: Boolean?, ring: TorRingState): VeilShellNotice = when {
  chatRunning == false -> VeilShellNotice.Stopped
  ring == TorRingState.Unavailable -> VeilShellNotice.TorDown
  else -> VeilShellNotice.None
}

// --- Passcode ----------------------------------------------------------------------------------------------------

const val VEIL_PASSCODE_MAX = 16
const val VEIL_PASSCODE_MIN = 4

fun veilPasscodeAppend(current: String, digit: Int): String =
  if (current.length < VEIL_PASSCODE_MAX && digit in 0..9) current + digit else current

/** Dots filled for each entered digit, at least [VEIL_PASSCODE_MIN] slots are shown so the minimum length is visible. */
fun veilPasscodeSlots(length: Int): Int = maxOf(VEIL_PASSCODE_MIN, minOf(length, VEIL_PASSCODE_MAX))

fun veilPasscodeSubmitEnabled(passcode: String, extraCheck: Boolean?, buttonsEnabled: Boolean): Boolean =
  extraCheck != false && passcode.length >= VEIL_PASSCODE_MIN && buttonsEnabled
