package chat.simplex.common.veil.data

import kotlin.test.*

class VeilWipeLogicTest {
  @Test
  fun everyVeilListKeyIsWiped() {
    val keys = setOf("veil_pinned_chats_1", "veil_pinned_chats_7", "veil_ntf_hidden_chats_1")
    assertEquals(listOf("veil_ntf_hidden_chats_1", "veil_pinned_chats_1", "veil_pinned_chats_7"), veilKeysToWipe(keys))
  }

  @Test
  fun preferencesAndUpstreamKeysAreKept() {
    val keys = setOf("VeilTheme", "VeilCallsOptIn", "PerformLA", "LALockDelay", "SelfDestruct", "veil", "xveil_a")
    assertEquals(emptyList(), veilKeysToWipe(keys))
  }

  @Test
  fun theUserIdIsPartOfThePinKey() {
    // a new profile after the wipe is user 1 again: the old key must be among those removed
    assertTrue("veil_pinned_chats_1" in veilKeysToWipe(setOf("veil_pinned_chats_1", "Other")))
  }

  @Test
  fun noKeysNothingToWipe() {
    assertEquals(emptyList(), veilKeysToWipe(emptySet()))
  }
}
