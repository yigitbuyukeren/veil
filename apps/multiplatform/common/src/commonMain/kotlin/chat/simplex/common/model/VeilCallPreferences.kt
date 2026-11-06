package chat.simplex.common.model

import chat.simplex.common.model.ChatModel.changingActiveUserMutex
import chat.simplex.common.platform.Log
import chat.simplex.common.platform.TAG
import chat.simplex.common.platform.chatModel
import chat.simplex.common.platform.settings
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.helpers.withBGApi
import chat.simplex.res.MR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A missing override is different from an explicit NO; keep it when temporarily disabling calls. */
internal fun savedVeilCallPreference(calls: SimpleChatPreference?): String = calls?.allow?.name ?: "default"

internal fun restoredVeilCallPreference(saved: String): SimpleChatPreference? =
  if (saved == "default") null
  else SimpleChatPreference(FeatureAllowed.entries.firstOrNull { it.name == saved } ?: FeatureAllowed.NO)

internal fun veilAdvertisedCallPreferences(prefs: ChatPreferences, enabled: Boolean, saved: String?): ChatPreferences =
  when {
    !enabled -> prefs.copy(calls = SimpleChatPreference(FeatureAllowed.NO))
    saved != null -> prefs.copy(calls = restoredVeilCallPreference(saved))
    else -> prefs
  }

/** Keep a durable snapshot before publishing; failed writes must remain retryable. */
internal suspend fun reconcileVeilCallPreferences(
  prefs: ChatPreferences,
  enabled: Boolean,
  saved: String?,
  remember: (String) -> Unit,
  forget: () -> Unit,
  publish: suspend (ChatPreferences) -> Boolean,
): Boolean {
  val advertised = veilAdvertisedCallPreferences(prefs, enabled, saved)
  if (!enabled && saved == null) remember(savedVeilCallPreference(prefs.calls))
  if (advertised != prefs && !publish(advertised)) return false
  if (enabled) forget()
  return true
}

/**
 * Publish the local opt-in using the existing encrypted contact-preferences protocol, including incognito contacts.
 * The core scopes preference writes to the active profile. Other profiles are reconciled when selected.
 * Only the calls override is changed; the original override is kept locally until it has been restored successfully.
 */
object VeilCallPreferences {
  private fun key(rhId: Long?, userId: Long, contactId: Long) = "veil_calls_saved_${rhId ?: "local"}_${userId}_$contactId"

  /** Also guard ordinary preference saves so editing another feature cannot re-enable calls while opted out. */
  fun advertised(rhId: Long?, userId: Long, contactId: Long, prefs: ChatPreferences, enabled: Boolean): ChatPreferences {
    if (enabled) return prefs
    val key = key(rhId, userId, contactId)
    if (!settings.hasKey(key)) settings.putString(key, savedVeilCallPreference(prefs.calls))
    return veilAdvertisedCallPreferences(prefs, enabled = false, saved = null)
  }

  fun preferenceSaved(rhId: Long?, userId: Long, contactId: Long) {
    if (VeilNetworkPolicy.callsAllowed()) settings.remove(key(rhId, userId, contactId))
  }

  suspend fun sync(controller: ChatController, rhId: Long?, userId: Long): Boolean = changingActiveUserMutex.withLock {
    val user = chatModel.currentUser.value
    // A profile switch already schedules reconciliation for the newly selected profile.
    if (user?.userId != userId || user.remoteHostId != rhId) return@withLock true
    val result = controller.sendCmd(rhId, CC.ApiGetChats(userId)).result
    if (result !is CR.ApiChats) return@withLock false
    var success = true
    val enabled = VeilNetworkPolicy.callsAllowed()
    for (chat in result.chats) {
      val contact = (chat.chatInfo as? ChatInfo.Direct)?.contact ?: continue
      if (!contact.ready || !contact.active) continue
      val key = key(rhId, userId, contact.contactId)
      val saved = settings.getStringOrNull(key)
      val synced = reconcileVeilCallPreferences(
        contact.userPreferences, enabled, saved,
        remember = { settings.putString(key, it) },
        forget = { settings.remove(key) },
        publish = { prefs ->
          // Deliberately bypass apiSetContactPrefs: this is restoring an override, not editing one.
          val updated = controller.sendCmd(rhId, CC.ApiSetContactPrefs(contact.contactId, prefs)).result
          if (updated is CR.ContactPrefsUpdated) {
            withContext(Dispatchers.Main) { chatModel.chatsContext.updateContact(rhId, updated.toContact) }
            true
          } else {
            false
          }
        },
      )
      if (!synced) success = false
    }
    success
  }

  suspend fun syncCurrent(controller: ChatController, rhId: Long?) {
    val user = chatModel.currentUser.value ?: return
    val success = try {
      sync(controller, rhId, user.userId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Call availability update failed: ${e.message}")
      false
    }
    if (!success) {
      AlertManager.shared.showAlertMsg(
        generalGetString(MR.strings.veil_calls_sync_failed_title),
        generalGetString(MR.strings.veil_calls_sync_failed_text),
        confirmText = generalGetString(MR.strings.veil_tor_retry),
        onConfirm = { withBGApi { syncCurrent(controller, rhId) } }
      )
    }
  }
}
