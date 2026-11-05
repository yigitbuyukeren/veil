package chat.simplex.common.veil.data

import androidx.compose.runtime.mutableStateOf
import chat.simplex.common.platform.settings

/**
 * Chats whose notifications never show message content. The core has no per-chat preview setting, so the list lives on
 * this device, per profile, as chat ids (modelled on [VeilPins]). It is not part of a database export.
 */
object VeilNtfPrivacy {
  /** Bumped on every change so that composables reading the list recompose. */
  val version = mutableStateOf(0)

  private fun key(userId: Long) = "veil_ntf_content_hidden_$userId"

  fun hiddenIds(userId: Long): List<String> {
    version.value
    return settings.getString(key(userId), "").split('\n').filter { it.isNotEmpty() }
  }

  fun isContentHidden(userId: Long, chatId: String): Boolean = chatId in hiddenIds(userId)

  fun toggle(userId: Long, chatId: String) {
    settings.putString(key(userId), hiddenAfterToggle(hiddenIds(userId), chatId).joinToString("\n"))
    version.value++
  }

  /** Forgets chats that no longer exist. */
  fun prune(userId: Long, existing: Set<String>) {
    val ids = hiddenIds(userId)
    val kept = ids.filter { it in existing }
    if (kept.size != ids.size) {
      settings.putString(key(userId), kept.joinToString("\n"))
      version.value++
    }
  }
}
