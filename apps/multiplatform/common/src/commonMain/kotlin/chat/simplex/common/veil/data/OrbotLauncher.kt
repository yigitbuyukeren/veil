package chat.simplex.common.veil.data

/** Package name of Orbot, the Tor client Veil relies on. */
const val ORBOT_PACKAGE = "org.torproject.android"

/** Where Orbot can be installed from. Shown as plain text; opening one goes through the external-link warning. */
const val ORBOT_FDROID_URL = "https://f-droid.org/packages/org.torproject.android/"
const val ORBOT_PLAY_URL = "https://play.google.com/store/apps/details?id=org.torproject.android"

enum class OrbotLaunchResult {
  /** Orbot's launch intent was started. */
  Launched,
  /** Orbot is not installed (or not visible to this app). */
  NotInstalled,
  /** Orbot is installed but could not be started. */
  Failed,
}

/**
 * Brings Orbot to the foreground through its launch intent. This starts another local app; it is not network traffic
 * of Veil. Android only; other platforms report [OrbotLaunchResult.NotInstalled].
 */
expect fun launchOrbot(): OrbotLaunchResult
