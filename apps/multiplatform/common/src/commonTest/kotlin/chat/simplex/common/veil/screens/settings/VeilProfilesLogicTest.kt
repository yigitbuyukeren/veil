package chat.simplex.common.veil.screens.settings

import kotlin.test.*

class VeilProfilesLogicTest {
  @Test
  fun hiddenProfileCanOnlyBeUnhiddenOrDeleted() {
    assertEquals(
      listOf(ProfileMenuAction.Unhide, ProfileMenuAction.Delete),
      profileMenuActions(hidden = true, showNtfs = true, visibleProfiles = 3)
    )
    assertEquals(
      listOf(ProfileMenuAction.Unhide, ProfileMenuAction.Delete),
      profileMenuActions(hidden = true, showNtfs = false, visibleProfiles = 1)
    )
  }

  @Test
  fun visibleProfileCanBeHiddenOnlyWhileAnotherVisibleOneExists() {
    assertEquals(
      listOf(ProfileMenuAction.Hide, ProfileMenuAction.Mute, ProfileMenuAction.Delete),
      profileMenuActions(hidden = false, showNtfs = true, visibleProfiles = 2)
    )
    assertEquals(
      listOf(ProfileMenuAction.Mute, ProfileMenuAction.Delete),
      profileMenuActions(hidden = false, showNtfs = true, visibleProfiles = 1)
    )
  }

  @Test
  fun mutedProfileOffersUnmute() {
    assertEquals(
      listOf(ProfileMenuAction.Hide, ProfileMenuAction.Unmute, ProfileMenuAction.Delete),
      profileMenuActions(hidden = false, showNtfs = false, visibleProfiles = 2)
    )
  }

  @Test
  fun deleteIsAlwaysOfferedLast() {
    for (hidden in listOf(true, false)) for (ntfs in listOf(true, false)) for (n in 0..3) {
      assertEquals(ProfileMenuAction.Delete, profileMenuActions(hidden, ntfs, n).last())
    }
  }

  @Test
  fun activeProfileShowsNoUnreadCountAndNoMutedMark() {
    val m = profileMarks(active = true, hidden = false, showNtfs = false, unreadCount = 7)
    assertTrue(m.active)
    assertEquals(0, m.unread)
    assertFalse(m.muted)
  }

  @Test
  fun otherProfileShowsUnreadLockAndMuted() {
    val m = profileMarks(active = false, hidden = true, showNtfs = false, unreadCount = 4)
    assertFalse(m.active)
    assertEquals(4, m.unread)
    assertTrue(m.unreadMuted)
    assertTrue(m.locked)
    assertTrue(m.muted)
    val plain = profileMarks(active = false, hidden = false, showNtfs = true, unreadCount = 0)
    assertEquals(ProfileMarks(false, 0, false, false, false), plain)
  }

  @Test
  fun negativeUnreadCountIsClamped() {
    assertEquals(0, profileMarks(false, false, true, -2).unread)
  }

  @Test
  fun onlyOtherProfilesAreSwitchedTo() {
    assertFalse(profileTapSwitches(active = true))
    assertTrue(profileTapSwitches(active = false))
  }

  @Test
  fun addProfileOnlyWhileSearchIsEmpty() {
    assertTrue(showAddProfile(""))
    assertTrue(showAddProfile("   "))
    assertFalse(showAddProfile("ali"))
    assertFalse(showAddProfile(" gizli parola "))
  }
}
