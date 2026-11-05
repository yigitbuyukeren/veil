package chat.simplex.common.veil.data

import chat.simplex.common.platform.settings

/**
 * Removes Veil's own device-local lists (`VeilPins` and any other list stored under a `veil_` key). Called from upstream's
 * `deleteChatDatabaseFilesAndState`, which every delete path runs (delete database, self-destruct passcode, failed
 * migration cleanup). A new profile gets user id 1 again, so leftover pins would point at the wrong chats.
 */
fun veilWipeDeviceLists() {
  veilKeysToWipe(settings.keys).forEach { settings.remove(it) }
  VeilPins.version.value++
}
