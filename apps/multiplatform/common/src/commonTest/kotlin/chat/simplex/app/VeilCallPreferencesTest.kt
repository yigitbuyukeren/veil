package chat.simplex.app

import chat.simplex.common.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class VeilCallPreferencesTest {
  @Test
  fun disablingAdvertisesNoEvenWhenContactWasExplicitlyAllowed() {
    for (allow in listOf(FeatureAllowed.YES, FeatureAllowed.ALWAYS)) {
      val prefs = ChatPreferences.sampleData.copy(calls = SimpleChatPreference(allow))
      val off = veilAdvertisedCallPreferences(prefs, enabled = false, saved = null)
      assertEquals(FeatureAllowed.NO, off.calls?.allow)
      assertFalse(FeatureEnabled.enabled(ChatFeature.Calls.asymmetric, prefs.calls!!, off.calls!!).forUser)
    }
  }

  @Test
  fun turningCallsBackOnRestoresTheOriginalContactChoiceIncludingDefaultAndNo() {
    val choices = listOf(null) + FeatureAllowed.entries.map { SimpleChatPreference(it) }
    for (choice in choices) {
      val original = ChatPreferences.sampleData.copy(calls = choice)
      val saved = savedVeilCallPreference(choice)
      val off = veilAdvertisedCallPreferences(original, enabled = false, saved = null)
      // The persisted snapshot survives repeated reconciliation and a process restart.
      val offAgain = veilAdvertisedCallPreferences(off, enabled = false, saved = saved)
      assertEquals(original, veilAdvertisedCallPreferences(offAgain, enabled = true, saved = saved))
    }
  }

  @Test
  fun optInDoesNotRemoveAnExistingContactRestrictionWithoutASnapshot() {
    val restricted = ChatPreferences.sampleData.copy(calls = SimpleChatPreference(FeatureAllowed.NO))
    assertEquals(restricted, veilAdvertisedCallPreferences(restricted, enabled = true, saved = null))
  }

  @Test
  fun changingCallAvailabilityPreservesOtherContactPreferences() {
    val original = ChatPreferences.sampleData.copy(
      timedMessages = TimedMessagesPreference(FeatureAllowed.YES, 3600),
      voice = null,
      fullDelete = SimpleChatPreference(FeatureAllowed.ALWAYS),
    )
    val off = veilAdvertisedCallPreferences(original, enabled = false, saved = null)
    assertEquals(original.copy(calls = off.calls), off)
    assertEquals(original, veilAdvertisedCallPreferences(off, enabled = true, saved = savedVeilCallPreference(original.calls)))
  }

  @Test
  fun corruptSavedChoiceDoesNotAccidentallyAllowCalls() {
    assertEquals(FeatureAllowed.NO, restoredVeilCallPreference("invalid")?.allow)
    assertNull(restoredVeilCallPreference("default"))
  }

  @Test
  fun failedDisableAndRepeatedSyncKeepTheOriginalSnapshot() = runBlocking {
    val original = ChatPreferences.sampleData.copy(calls = null)
    val peer = PreferencePeer(original)
    peer.rejectWrites = true
    assertFalse(peer.sync(enabled = false))
    assertEquals("default", peer.saved)
    assertEquals(original, peer.current)
    peer.rejectWrites = false
    assertTrue(peer.sync(enabled = false))
    assertEquals(FeatureAllowed.NO, peer.current.calls?.allow)
    val writes = peer.writes
    assertTrue(peer.sync(enabled = false))
    assertEquals(writes, peer.writes)
    assertEquals("default", peer.saved)
    assertTrue(peer.sync(enabled = true))
    assertEquals(original, peer.current)
    assertNull(peer.saved)
  }

  @Test
  fun failedRestoreCanBeRetriedAfterRestartWithoutLosingContactChoice() = runBlocking {
    val original = ChatPreferences.sampleData.copy(calls = SimpleChatPreference(FeatureAllowed.ALWAYS))
    val peer = PreferencePeer(original)
    assertTrue(peer.sync(enabled = false))
    peer.rejectWrites = true
    assertFalse(peer.sync(enabled = true))
    assertEquals("ALWAYS", peer.saved)
    assertEquals(FeatureAllowed.NO, peer.current.calls?.allow)
    val restarted = PreferencePeer(peer.current).also { it.saved = peer.saved }
    assertTrue(restarted.sync(enabled = true))
    assertEquals(original, restarted.current)
    assertNull(restarted.saved)
  }

  @Test
  fun anExistingRefusalNeedsNoWritesAndRemainsRefusedAfterOptIn() = runBlocking {
    val original = ChatPreferences.sampleData.copy(calls = SimpleChatPreference(FeatureAllowed.NO))
    val peer = PreferencePeer(original)
    assertTrue(peer.sync(enabled = false))
    assertEquals("NO", peer.saved)
    assertTrue(peer.sync(enabled = true))
    assertEquals(original, peer.current)
    assertEquals(0, peer.writes)
    assertNull(peer.saved)
  }

  @Test
  fun interruptedWriteRetainsTheSnapshotForRetry() = runBlocking {
    var saved: String? = null
    assertFailsWith<IllegalStateException> {
      reconcileVeilCallPreferences(
        ChatPreferences.sampleData, enabled = false, saved = saved,
        remember = { saved = it }, forget = { saved = null },
        publish = { error("interrupted") },
      )
    }
    assertEquals("YES", saved)
  }

  private class PreferencePeer(var current: ChatPreferences) {
    var saved: String? = null
    var rejectWrites = false
    var writes = 0

    suspend fun sync(enabled: Boolean): Boolean = reconcileVeilCallPreferences(
      current, enabled, saved,
      remember = { saved = it },
      forget = { saved = null },
      publish = {
        writes++
        if (rejectWrites) false else {
          current = it
          true
        }
      },
    )
  }
}
