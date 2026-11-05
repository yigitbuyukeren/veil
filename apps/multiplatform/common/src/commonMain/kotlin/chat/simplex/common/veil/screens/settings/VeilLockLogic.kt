package chat.simplex.common.veil.screens.settings

/**
 * Decisions of the Veil app-lock, self-destruct, screen-protection and local-file-encryption rows that do not need the
 * UI. What the lock does (modes, passcode screens, authentication) is upstream's `SimplexLockView` and the Veil lock
 * screen; nothing here sets or checks a passcode.
 */

enum class AppLockState { Off, System, Passcode }

/** [performLA] is upstream's "lock is on"; [passcodeMode] is `laMode == PASSCODE`. The mode only matters while the lock is on. */
fun appLockState(performLA: Boolean, passcodeMode: Boolean): AppLockState = when {
  !performLA -> AppLockState.Off
  passcodeMode -> AppLockState.Passcode
  else -> AppLockState.System
}

/** Lock delays the Veil page offers, in seconds: right away, 30 s, 1 min, 5 min. */
val LOCK_DELAYS: List<Int> = listOf(0, 30, 60, 300)

/** The offered delays; a stored value that is not among them (upstream also offers 10 s, 3 min and 10 min) is kept as an extra choice so it is shown as selected. */
fun lockDelayChoices(current: Int): List<Int> = (LOCK_DELAYS + current).distinct().sortedBy { if (it == 0) -1 else it }

/** The delay row exists only while the lock is on. */
fun lockDelayShown(performLA: Boolean): Boolean = performLA

enum class LockDelayText { Immediately, Seconds, Minutes }

/** Which text a delay uses: 0 = right away; below a minute or not a whole number of minutes in seconds; otherwise minutes. */
fun lockDelayText(seconds: Int): LockDelayText = when {
  seconds == 0 -> LockDelayText.Immediately
  seconds < 60 || seconds % 60 != 0 -> LockDelayText.Seconds
  else -> LockDelayText.Minutes
}

/** The value shown with the text: seconds or whole minutes. */
fun lockDelayAmount(seconds: Int): Int = if (lockDelayText(seconds) == LockDelayText.Minutes) seconds / 60 else seconds

/** Turning the screen protection off asks first (screenshots and the app switcher preview show the chats); turning it on does not. */
fun screenProtectionOffNeedsWarning(newValue: Boolean): Boolean = !newValue

enum class SelfDestructEntry {
  /** The self-destruct passcode exists only next to a Veil passcode lock: explain that first. */
  NeedsPasscodeLock,
  /** Show the warning, then upstream's lock page where it is set. */
  WarnThenOpen,
}

fun selfDestructEntry(performLA: Boolean, passcodeMode: Boolean): SelfDestructEntry =
  if (appLockState(performLA, passcodeMode) == AppLockState.Passcode) SelfDestructEntry.WarnThenOpen else SelfDestructEntry.NeedsPasscodeLock

/** The self-destruct row's value: on only when the preference is on and a passcode lock is active (upstream clears it when the lock moves to the system). */
fun selfDestructActive(selfDestructPref: Boolean, performLA: Boolean, passcodeMode: Boolean): Boolean =
  selfDestructPref && appLockState(performLA, passcodeMode) == AppLockState.Passcode

/**
 * Local-file encryption is locked on: while it is on the switch cannot be turned off (a deleted file's pieces stay
 * unreadable). If it is off (an imported setting), it can still be turned on.
 */
fun encryptFilesCanChange(current: Boolean): Boolean = !current
