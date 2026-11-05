package chat.simplex.common.veil.screens.groups

/*
 * Pure rules of the Veil welcome message screen, the same checks upstream's `GroupWelcomeView` makes. The length in
 * bytes comes from the chat core (`chatJsonLength`) and is passed in.
 */

const val VEIL_WELCOME_MAX_BYTES = 1200

fun welcomeUnchanged(text: String, saved: String?): Boolean = text == saved || (text == "" && saved == null)

fun welcomeFits(jsonLength: Int): Boolean = jsonLength <= VEIL_WELCOME_MAX_BYTES

/** The text sent to the core: newlines and spaces trimmed at both ends, empty as null (removes the message). */
fun welcomeToSave(text: String): String? = text.trim('\n', ' ').ifEmpty { null }

/** Only the owner of a normal group edits; business chats and everybody else only read and copy. */
fun canEditWelcome(isOwner: Boolean, isBusinessChat: Boolean): Boolean = isOwner && !isBusinessChat

fun canSaveWelcome(unchanged: Boolean, fits: Boolean): Boolean = !unchanged && fits

/** The edit/preview switch is off while the text is empty. */
fun canTogglePreview(text: String): Boolean = text.isNotEmpty()

enum class VeilWelcomeClose { Close, TooLong, AskToSave }

fun welcomeCloseAction(unchanged: Boolean, fits: Boolean): VeilWelcomeClose = when {
  unchanged -> VeilWelcomeClose.Close
  !fits -> VeilWelcomeClose.TooLong
  else -> VeilWelcomeClose.AskToSave
}
