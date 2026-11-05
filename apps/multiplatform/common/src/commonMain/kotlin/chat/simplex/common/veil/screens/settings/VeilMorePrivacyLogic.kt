package chat.simplex.common.veil.screens.settings

/** Decisions of the "Diğer gizlilik ayarları" screen that do not need the UI. */

/** Media blur radii offered by upstream's privacy screen, in the order they are shown. */
val BLUR_RADII: List<Int> = listOf(0, 12, 24, 48)

enum class BlurLevel(val radius: Int) { Off(0), Soft(12), Medium(24), Strong(48) }

/** The levels to show: the four standard ones; [current] is not a standard radius only when it was imported, then it is kept as a fifth choice. */
data class BlurChoice(val level: BlurLevel?, val radius: Int)

fun blurChoices(current: Int): List<BlurChoice> {
  val standard = BlurLevel.values().map { BlurChoice(it, it.radius) }
  return if (BLUR_RADII.contains(current)) standard else standard + BlurChoice(null, current)
}

/** Turning the draft off also drops the draft that is open now (as upstream does); turning it on changes nothing else. */
fun draftToggleClearsDraft(saveDraft: Boolean): Boolean = !saveDraft

/** Files: which footer text goes under the relay-approval switch. */
enum class RelayNote { Asks, DoesNotAsk }

fun relayNote(askToApprove: Boolean): RelayNote = if (askToApprove) RelayNote.Asks else RelayNote.DoesNotAsk

/** The sections that act on the current profile appear only when there is a profile to act on (not on a desktop that has none). */
fun profileSectionsShown(hasUser: Boolean, desktopNoUserNoRemote: Boolean): Boolean = hasUser && !desktopNoUserNoRemote

/**
 * Chats whose own delivery-receipt choice differs from the new global [enable]. [perChat] holds one entry per chat of the
 * kind (null = the chat follows the global choice).
 */
fun receiptOverrides(perChat: List<Boolean?>, enable: Boolean): Int = perChat.count { it != null && it != enable }

enum class ReceiptsStep { Apply, AskAboutOverrides }

/** With no differing chats the change is applied directly; otherwise the user is asked whether to keep or replace them. */
fun receiptsStep(overrides: Int): ReceiptsStep = if (overrides == 0) ReceiptsStep.Apply else ReceiptsStep.AskAboutOverrides
