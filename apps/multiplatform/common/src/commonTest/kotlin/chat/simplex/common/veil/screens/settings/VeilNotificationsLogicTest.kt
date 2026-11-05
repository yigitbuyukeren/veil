package chat.simplex.common.veil.screens.settings

import chat.simplex.common.model.NotificationsMode
import kotlin.test.*

class VeilNotificationsLogicTest {
  @Test
  fun modesAreOfferedOnAndroidOnlyAndCoverEveryMode() {
    assertEquals(
      listOf(NotificationsMode.SERVICE, NotificationsMode.PERIODIC, NotificationsMode.OFF),
      notificationModeOptions(true)
    )
    assertEquals(NotificationsMode.values().toSet(), notificationModeOptions(true).toSet())
    assertTrue(notificationModeOptions(false).isEmpty())
  }

  @Test
  fun xiaomiHintOnlyForBackgroundModes() {
    assertTrue(showXiaomiHint(true, NotificationsMode.SERVICE))
    assertTrue(showXiaomiHint(true, NotificationsMode.PERIODIC))
    assertFalse(showXiaomiHint(true, NotificationsMode.OFF))
    assertFalse(showXiaomiHint(false, NotificationsMode.SERVICE))
  }

  @Test
  fun pickingTheActiveModeChangesNothing() {
    assertFalse(shouldChangeNotificationsMode(NotificationsMode.OFF, NotificationsMode.OFF))
    assertTrue(shouldChangeNotificationsMode(NotificationsMode.OFF, NotificationsMode.SERVICE))
  }
}
