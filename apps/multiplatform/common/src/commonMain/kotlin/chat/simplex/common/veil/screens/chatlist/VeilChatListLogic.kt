package chat.simplex.common.veil.screens.chatlist

import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.views.chatlist.ActiveFilter
import chat.simplex.common.views.chatlist.PresetTagKind

/*
 * Non-UI logic of the Veil chat list. Kept free of Compose and of ChatModel types where possible so it can be unit tested.
 */

/** Interval between Tor proxy probes while the list is on screen. */
const val VEIL_TOR_PROBE_INTERVAL_MS = 15_000L

/**
 * The ring state shown in the header. The probe only proves that a SOCKS listener answers; it says nothing about
 * circuits, so the best state is "proxy answers", never "Tor is healthy".
 *  - probe failed -> [TorRingState.Unavailable] (nothing is sent)
 *  - probe answered and the chat core is running -> [TorRingState.ProxyAnswers]
 *  - anything else (no probe result yet, or the proxy answers but the core is not running) -> [TorRingState.Connecting]
 */
fun torRingStateFor(proxyAnswers: Boolean?, chatRunning: Boolean?): TorRingState = when (proxyAnswers) {
  false -> TorRingState.Unavailable
  true -> if (chatRunning == true) TorRingState.ProxyAnswers else TorRingState.Connecting
  null -> TorRingState.Connecting
}

/** The three filter pills. */
enum class VeilListFilter { All, Unread, Groups }

/** Upstream filter that implements [this], so the list reuses `filteredChats` / `presetTagMatchesChat` / `Chat.unreadTag`. */
fun VeilListFilter.toActiveFilter(): ActiveFilter? = when (this) {
  VeilListFilter.All -> null
  VeilListFilter.Unread -> ActiveFilter.Unread
  VeilListFilter.Groups -> ActiveFilter.PresetTag(PresetTagKind.GROUPS)
}

/**
 * Seed of the mask avatar of a chat: the stable chat id (`@12`, `#3`, `:7`...) plus the profile display name.
 * The local alias is deliberately not part of it, so renaming a contact locally does not change its mask.
 */
fun avatarSeed(chatId: String, displayName: String): String = "$chatId|$displayName"

/** Lower-cases and folds the Turkish dotted/dotless i so that "I", "ı", "İ" and "i" all match each other. */
fun foldForSearch(s: String): String =
  s.trim().lowercase().replace("̇", "").replace('ı', 'i')

/** True when the (plain text) chat preview contains the query. A blank query matches everything; no preview never matches. */
fun previewMatchesQuery(preview: String?, query: String): Boolean {
  val q = foldForSearch(query)
  if (q.isEmpty()) return true
  if (preview == null) return false
  return foldForSearch(preview).contains(q)
}

/**
 * Chat ids that stay in the list: those of [candidates] (already passed the filter) that match by name (the ids
 * upstream search returned) or by preview text. The order of [candidates] is kept.
 */
fun searchResultIds(candidates: List<String>, nameMatches: Set<String>, previewOf: (String) -> String?, query: String): List<String> {
  if (foldForSearch(query).isEmpty()) return candidates
  return candidates.filter { it in nameMatches || previewMatchesQuery(previewOf(it), query) }
}

/** What the list body shows when no chat is visible. */
enum class VeilListEmpty { None, NoChats, NoSearchResults, NoUnread, NoGroups }

fun listEmptyKind(totalChats: Int, visibleChats: Int, filter: VeilListFilter, query: String): VeilListEmpty = when {
  visibleChats > 0 -> VeilListEmpty.None
  foldForSearch(query).isNotEmpty() -> VeilListEmpty.NoSearchResults
  filter == VeilListFilter.Unread && totalChats > 0 -> VeilListEmpty.NoUnread
  filter == VeilListFilter.Groups && totalChats > 0 -> VeilListEmpty.NoGroups
  else -> VeilListEmpty.NoChats
}

/** The unread pill shows its count only when there is something unread. */
fun showUnreadCount(count: Int): Boolean = count > 0

/** Whether stored reveals (eye buttons) must be dropped: when the app leaves the foreground. */
fun shouldDropReveals(wasVisible: Boolean, isVisible: Boolean): Boolean = wasVisible && !isVisible

/** Most chats that can be pinned at once. */
const val VEIL_MAX_PINNED = 5

/** [chatIds] with the pinned ones first, in pin order; the rest keep their order. Ids in [pinned] that are not listed are dropped. */
fun orderWithPins(chatIds: List<String>, pinned: List<String>): List<String> {
  val present = chatIds.toHashSet()
  val first = pinned.distinct().filter { it in present }
  val firstSet = first.toHashSet()
  return first + chatIds.filter { it !in firstSet }
}

/** Pin list after pinning [chatId]: unchanged if already pinned, null when the limit is reached. */
fun pinnedAfterPin(pinned: List<String>, chatId: String): List<String>? = when {
  chatId in pinned -> pinned
  pinned.size >= VEIL_MAX_PINNED -> null
  else -> pinned + chatId
}
