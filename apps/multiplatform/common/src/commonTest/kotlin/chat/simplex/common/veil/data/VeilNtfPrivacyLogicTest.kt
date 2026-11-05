package chat.simplex.common.veil.data

import chat.simplex.common.model.NotificationPreviewMode
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.*'
class VeilNtfPrivacyLogicTest {
  @Test
  fun toggleAddsAndRemoves() {
    assertEquals(listOf("a"), hiddenAfterToggle(emptyList(), "a"))
    assertEquals(listOf("a", "b"), hiddenAfterToggle(listOf("a"), "b"))
    assertEquals(listOf("b"), hiddenAfterToggle(listOf("a", "b"), "a"))
  }

  @Test
  fun hiddenChatNeverShowsContent() {
    assertEquals(NotificationPreviewMode.CONTACT, effectivePreviewMode(NotificationPreviewMode.MESSAGE, true))
  }

  @Test
  fun stricterGlobalModeWins() {
    assertEquals(NotificationPreviewMode.HIDDEN, effectivePreviewMode(NotificationPreviewMode.HIDDEN, true))
    assertEquals(NotificationPreviewMode.HIDDEN, effectivePreviewMode(NotificationPreviewMode.HIDDEN, false))
    assertEquals(NotificationPreviewMode.CONTACT, effectivePreviewMode(NotificationPreviewMode.CONTACT, true))
  }

  @Test
  fun chatNotHiddenKeepsGlobalMode() {
    for (m in NotificationPreviewMode.values()) assertEquals(m, effectivePreviewMode(m, false))
  }

  @Test
  fun unknownStoredModeIsTreatedAsHidden() {
    assertEquals(NotificationPreviewMode.HIDDEN, effectivePreviewModeName(null, false))
    assertEquals(NotificationPreviewMode.HIDDEN, effectivePreviewModeName("nonsense", true))
    assertEquals(NotificationPreviewMode.CONTACT, effectivePreviewModeName("MESSAGE", true))
  }
}
