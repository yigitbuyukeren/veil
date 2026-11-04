package chat.simplex.common.veil.data

import android.content.Intent
import chat.simplex.common.platform.androidAppContext

actual fun launchOrbot(): OrbotLaunchResult = try {
  val intent = androidAppContext.packageManager.getLaunchIntentForPackage(ORBOT_PACKAGE)
  if (intent == null) {
    OrbotLaunchResult.NotInstalled
  } else {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    androidAppContext.startActivity(intent)
    OrbotLaunchResult.Launched
  }
} catch (e: Exception) {
  OrbotLaunchResult.Failed
}
