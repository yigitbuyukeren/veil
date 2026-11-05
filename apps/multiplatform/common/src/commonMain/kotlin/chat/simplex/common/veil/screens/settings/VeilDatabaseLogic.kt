package chat.simplex.common.veil.screens.settings

/**
 * Decisions of the Veil database screens that do not need the UI. They only restate when upstream's `DatabaseView` and
 * `DatabaseManagementView` show or enable a row; what a row does is upstream's code (`databaseActions`,
 * `databaseManagementActions`, the passphrase screen) and is not decided here.
 */

/** Which icon the passphrase row carries, as upstream picks it: open lock, key (passphrase kept on the device), lock. */
enum class PassphraseIcon { OpenLock, Key, Lock }

/** [chatDbEncrypted] is null while the app has not read the database state; upstream treats that as "not unencrypted". */
fun passphraseIcon(chatDbEncrypted: Boolean?, passphraseKept: Boolean): PassphraseIcon = when {
  chatDbEncrypted == false -> PassphraseIcon.OpenLock
  passphraseKept -> PassphraseIcon.Key
  else -> PassphraseIcon.Lock
}

/** Rows are greyed while an operation runs, except on a desktop that has no user (never the case on Android). */
fun operationsDisabled(progress: Boolean, desktopNoUserNoRemote: Boolean): Boolean = progress && !desktopNoUserNoRemote

/** "Delete messages after" can be changed only while the chat runs and nothing else is in progress. */
fun ttlEnabled(stopped: Boolean, progress: Boolean): Boolean = !stopped && !progress

/** The messages section is hidden on a desktop that has no user. */
fun messagesSectionShown(desktopNoUserNoRemote: Boolean): Boolean = !desktopNoUserNoRemote

/** "Delete all files" is greyed while an operation runs and when there are no files. */
fun deleteFilesDisabled(operationsDisabled: Boolean, fileCount: Int): Boolean = operationsDisabled || fileCount == 0

/** The files line under the delete button: nothing stored, or the count and size. */
fun hasStoredFiles(fileCount: Int): Boolean = fileCount != 0

/** Remote hosts block the run-chat switch while one is connected. */
fun runChatToggleEnabled(connectedRemoteHosts: Int): Boolean = connectedRemoteHosts == 0

/** The "disconnect phones" row in the database group: no local user yet and a host is connected. */
fun disconnectHostsInDatabaseGroup(localUserCreated: Boolean?, toggleEnabled: Boolean): Boolean =
  localUserCreated != true && !toggleEnabled

/** The run-chat section exists only when a local user has been created. */
fun runChatSectionShown(localUserCreated: Boolean?): Boolean = localUserCreated == true

/** The "disconnect phones" row in the run-chat section. */
fun disconnectHostsInRunChat(toggleEnabled: Boolean): Boolean = !toggleEnabled

/** The run-chat switch is usable when no host is connected and nothing is in progress. */
fun runChatSwitchEnabled(toggleEnabled: Boolean, progress: Boolean): Boolean = toggleEnabled && !progress

/** The warning about using the latest database on one device only is shown while the chat is stopped. */
fun recentVersionFooterShown(stopped: Boolean): Boolean = stopped

enum class RunChatAction { Start, AskToStop }

/** Switching on starts the chat directly; switching off opens upstream's stop-chat question first. */
fun runChatAction(switchedOn: Boolean): RunChatAction = if (switchedOn) RunChatAction.Start else RunChatAction.AskToStop
