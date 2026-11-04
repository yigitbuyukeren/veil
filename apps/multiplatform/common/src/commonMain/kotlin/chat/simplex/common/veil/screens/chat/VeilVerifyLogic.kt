package chat.simplex.common.veil.screens.chat

/**
 * Splits a security code into the groups shown in the grid. The core gives the code as groups of five digits
 * separated by spaces (12 groups); a code without separators is cut into groups of five. Anything that is not a
 * group separator (spaces, newlines) is kept as is, so an unexpected code is still shown in full.
 */
fun securityCodeGroups(code: String, groupSize: Int = 5): List<String> {
  val tokens = code.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
  return if (tokens.size == 1 && tokens[0].length > groupSize) tokens[0].chunked(groupSize) else tokens
}

/** The groups laid out in rows of [columns] (the prototype shows 12 groups as 3 rows of 4). */
fun securityCodeRows(code: String, columns: Int = 4): List<List<String>> =
  securityCodeGroups(code).chunked(columns)
