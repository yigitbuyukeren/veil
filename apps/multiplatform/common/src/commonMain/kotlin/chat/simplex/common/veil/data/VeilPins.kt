package chat.simplex.common.veil.data

import androidx.compose.runtime.mutableStateOf
import chat.simplex.common.platform.settings
import chat.simplex.common.veil.screens.chatlist.pinnedAfterPin

/**
 * Pinned chats of the chat list. The core has no pinning (only favorites), so the pins live on this device, per profile,
 * as an ordered list of chat ids. They are not part of a database export.
 */
object VeilPins {
  /** Bumped on every change so that composables reading the pins recompose. */
  val version = mutableStateOf(0)

  private fun key(userId: Long) = "veil_pinned_chats_$userId"

  fun pinnedIds(userId: Long): List<String> {
    version.value
    return settings.getString(key(userId), "").split('\n').filter { it.isNotEmpty() }
  }

  private fun save(userId: Long, ids: List<String>) = {
    settings.putString(key(userId), ids.joinToString("\n"))
    version.value++
  }

  /** False when the limit is reached and nothing was pinned. */
  fun pin(userId: Long, chatId: String): Boolean {
    val next = pinnedAfterPin(pinnedIds(userId), chatId) ?: return false
    save(userId, next)
    return true
  }

  fun unpin(userId: Long, chatId: String) = save(userId, pinnedIds(userId) - chatId)

  fun isPinned(userId: Long, chatId: String): Boolean = chatId in pinnedIds(userId)

  /** Forgets pins of chats that no longer exist. */
  fun prune(userId: Long, existing: Set<String>) {
    val ids = pinnedIds(userId)
    val kept = ids.filter { it in existing }
    if (kept.size != ids.size) save(userId, kept)
  }
}
