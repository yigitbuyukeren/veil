package chat.simplex.common.veil.design

/** Whether `Modifier.blur` actually blurs on this platform (Android API 31+, desktop always). */
expect val veilBlurSupported: Boolean

/** True in debuggable builds only; gates developer-only screens such as [VeilGalleryScreen]. */
expect val veilIsDebugBuild: Boolean
