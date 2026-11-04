package chat.simplex.common.veil

/**
 * Routing flags for the Veil UI redesign. One flag per screen group; a group is switched to its Veil screens by
 * flipping its flag to true once those screens exist. All false = upstream screens (in the Veil theme).
 *
 * The single place that reads these flags is `MainScreen` in `App.kt` (look for "Veil: screen routing").
 * Groups not owned by `MainScreen` (chat, newChat, groups, settings, system) are reached from the upstream
 * views that open them; those views should branch on the matching flag at their entry point.
 */
object VeilUi {
  /** Onboarding stages (SimpleXInfo, CreateFirstProfile, network commitments...). Android only; desktop keeps the upstream onboarding. */
  var onboarding: Boolean = true
  /** Chat list (home) screen. */
  var chatList: Boolean = true
  /** Conversation screen. */
  var chat: Boolean = false
  /** New chat flows: create link, connect via link / scan / paste. */
  var newChat: Boolean = false
  /** Group screens: create, info, members, invite. */
  var groups: Boolean = false
  /** Settings screens. */
  var settings: Boolean = false
  /** System surfaces: alerts, lock screen, database error, progress, call overlays. */
  var system: Boolean = false
}
