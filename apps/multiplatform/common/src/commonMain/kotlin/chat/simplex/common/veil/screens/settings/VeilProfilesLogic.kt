package chat.simplex.common.veil.screens.settings

/** What the profile menu can do to one profile. Each maps to one upstream action of `UserProfilesView`. */
enum class ProfileMenuAction { Hide, Unhide, Mute, Unmute, Delete }

/**
 * Menu of one profile, same rules as upstream's `UserView` dropdown: a hidden profile can only be unhidden; a visible one
 * can be hidden (only while more than one visible profile exists, so the last visible profile cannot vanish) and muted or
 * unmuted; every profile can be deleted.
 */
fun profileMenuActions(hidden: Boolean, showNtfs: Boolean, visibleProfiles: Int): List<ProfileMenuAction> = buildList {
  if (hidden) {
    add(ProfileMenuAction.Unhide)
  } else {
    if (visibleProfiles > 1) add(ProfileMenuAction.Hide)
    add(if (showNtfs) ProfileMenuAction.Mute else ProfileMenuAction.Unmute)
  }
  add(ProfileMenuAction.Delete)
}

/** What a profile row shows next to the name. [unread] is 0 when there is nothing to show. */
data class ProfileMarks(val active: Boolean, val unread: Int, val unreadMuted: Boolean, val locked: Boolean, val muted: Boolean)

/**
 * The active profile gets the "active" mark and no unread count (its chats are the ones on screen). Other profiles show
 * their unread count (greyed when muted); hidden ones a lock; muted ones a muted mark.
 */
fun profileMarks(active: Boolean, hidden: Boolean, showNtfs: Boolean, unreadCount: Int): ProfileMarks = ProfileMarks(
  active = active,
  unread = if (active) 0 else maxOf(unreadCount, 0),
  unreadMuted = !showNtfs,
  locked = hidden,
  muted = !active && !showNtfs,
)

/** Tapping the active profile does nothing (upstream opens no settings from this list); others are switched to. */
fun profileTapSwitches(active: Boolean): Boolean = !active

/**
 * The search field of the list also takes the password of a hidden profile, so "add profile" is offered only while the
 * field is empty (as upstream) and a typed word never creates anything by accident.
 */
fun showAddProfile(searchOrPassword: String): Boolean = searchOrPassword.trim().isEmpty()
