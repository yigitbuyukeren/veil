package chat.simplex.common.veil.design

import android.content.pm.ApplicationInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import chat.simplex.common.platform.androidAppContext

private fun animatorScaleReduced(): Boolean = try {
  val scale = Settings.Global.getFloat(androidAppContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
  VeilMotion.reduceMotionForAnimatorScale(scale)
} catch (e: Exception) {
  false
}

@Composable
actual fun rememberSystemReduceMotion(): Boolean {
  var reduced by remember { mutableStateOf(animatorScaleReduced()) }
  DisposableEffect(Unit) {
    val resolver = androidAppContext.contentResolver
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        reduced = animatorScaleReduced()
      }
    }
    try {
      resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
    } catch (e: Exception) {
    }
    onDispose { resolver.unregisterContentObserver(observer) }
  }
  return reduced
}

/** RenderEffect blur exists from API 31; below that [Perde] falls back to an opaque tulle. */
actual val veilBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual val veilIsDebugBuild: Boolean
  get() = try {
    (androidAppContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
  } catch (e: Exception) {
    false
  }
