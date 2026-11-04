package chat.simplex.common.veil.design

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSystemReduceMotion(): Boolean = false

actual val veilBlurSupported: Boolean = true

/** Desktop has no debug/release split; enable the gallery with `-Dveil.debug=true`. */
actual val veilIsDebugBuild: Boolean = System.getProperty("veil.debug") == "true"
