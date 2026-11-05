package chat.simplex.common.veil.data

/**
 * Device-local lists of Veil (chat pins, per-chat settings) are kept in the app preferences under keys that start with
 * this prefix. Preferences (theme, switches) use CamelCase names and do not match, so only lists are wiped.
 */
const val VEIL_LIST_KEY_PREFIX = "veil_"

/** The preference keys to remove when the database is deleted: every key of a Veil device-local list. */
fun veilKeysToWipe(allKeys: Set<String>): List<String> = allKeys.filter { it.startsWith(VEIL_LIST_KEY_PREFIX) }.sorted()
