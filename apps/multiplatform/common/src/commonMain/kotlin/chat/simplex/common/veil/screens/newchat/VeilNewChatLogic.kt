package chat.simplex.common.veil.screens.newchat

import chat.simplex.common.veil.design.TorRingState

/*
 * Pure state of the Veil new-chat sheet. Everything that talks to the core (creating the invitation, planning and
 * connecting) stays upstream's; this file only decides what the sheet shows.
 */

/** Pages of the sheet. */
enum class NewChatPage { Menu, Invite, Paste, Scan }

/** Rows of the menu, in display order. */
enum class NewChatEntry { Invite, Paste, Scan, Group }

val newChatEntries: List<NewChatEntry> = listOf(NewChatEntry.Invite, NewChatEntry.Paste, NewChatEntry.Scan, NewChatEntry.Group)

/** The page a menu row opens; null = the row leaves the sheet (group creation is upstream's screen for now). */
fun NewChatEntry.page(): NewChatPage? = when (this) {
  NewChatEntry.Invite -> NewChatPage.Invite
  NewChatEntry.Paste -> NewChatPage.Paste
  NewChatEntry.Scan -> NewChatPage.Scan
  NewChatEntry.Group -> null
}

/** What the invitation page shows. */
enum class InviteViewState {
  /** No Tor: nothing can be created. Shown with the amber note. */
  TorUnavailable,
  /** The core is creating the invitation. */
  Creating,
  /** Creation failed (the core already showed its alert); the page offers a retry. */
  Failed,
  /** A link exists: QR, link, copy and share. */
  Ready,
}

/**
 * State of the invitation page. An existing link is always shown (it was made before Tor went away and showing it
 * sends nothing from this device); creating a new one needs [ring] to be anything but [TorRingState.Unavailable].
 */
fun inviteViewState(ring: TorRingState, hasLink: Boolean, creating: Boolean, failed: Boolean): InviteViewState = when {
  hasLink -> InviteViewState.Ready
  ring == TorRingState.Unavailable -> InviteViewState.TorUnavailable
  creating -> InviteViewState.Creating
  failed -> InviteViewState.Failed
  else -> InviteViewState.Creating
}

/** Whether opening the invitation page should start creating a link now. */
fun shouldCreateInvitation(ring: TorRingState, hasLink: Boolean, creating: Boolean, failed: Boolean): Boolean =
  !hasLink && !creating && !failed && ring != TorRingState.Unavailable

/** Whether the retry button of a failed creation may be used (never while Tor is unavailable). */
fun canRetryInvitation(ring: TorRingState, creating: Boolean): Boolean = !creating && ring != TorRingState.Unavailable

/** Copy and share are offered only for a ready link. */
fun canShareInvitation(state: InviteViewState): Boolean = state == InviteViewState.Ready

/**
 * Incognito state shown for an invitation: the pending connection's own flag when it exists (it is what the link
 * was created with), otherwise the preference the next creation will use.
 */
fun invitationIncognito(connIncognito: Boolean?, pref: Boolean): Boolean = connIncognito ?: pref

/** The pasted text without surrounding whitespace and line breaks (what is handed to the link parser). */
fun cleanPastedLink(text: String): String = text.trim()

/** Whether the paste page's connect button is enabled. Whether the text is a valid link is decided by upstream's parser on tap. */
fun pasteInputReady(text: String, connecting: Boolean): Boolean = !connecting && cleanPastedLink(text).isNotEmpty()
