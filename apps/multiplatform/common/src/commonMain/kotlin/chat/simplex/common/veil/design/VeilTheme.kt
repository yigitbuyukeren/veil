package chat.simplex.common.veil.design

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import chat.simplex.common.ui.theme.AppColors
import chat.simplex.common.ui.theme.AppWallpaper
import chat.simplex.common.ui.theme.DefaultTheme
import chat.simplex.common.ui.theme.ThemeManager

val LocalVeilColors = staticCompositionLocalOf { VeilColors.Gece }
val LocalVeilType = staticCompositionLocalOf { VeilType.Default }
val LocalVeilShapes = staticCompositionLocalOf { VeilShapes.Default }
val LocalVeilMotion = staticCompositionLocalOf { VeilMotion.Default }

/**
 * Entry point of the Veil design system.
 *
 * Wiring:
 *  - `ThemeManager.currentColors` returns [activeTheme] (a Gece palette mapped into upstream
 *    `Colors`/`AppColors`) while [forceVeilTheme] is true, so every not-yet-redesigned upstream screen
 *    renders in the Veil dark look. Light/system selection and per-user/per-chat overrides are ignored for now.
 *  - `SimpleXTheme` wraps its content in [VeilTheme] (the composable below) which provides the CompositionLocals,
 *    and uses [VeilMaterialTypography] / [VeilMaterialShapes] for Material.
 *
 * Read tokens with `VeilTheme.colors`, `.type`, `.shapes`, `.motion`; `VeilTheme.reduceMotion` for the motion flag.
 */
object VeilTheme {
  /** Until the light "Sis" theme exists, the whole app is forced into Gece. Flip to false to restore upstream themes. */
  const val forceVeilTheme: Boolean = true

  val colors: VeilColors
    @Composable @ReadOnlyComposable get() = LocalVeilColors.current
  val type: VeilType
    @Composable @ReadOnlyComposable get() = LocalVeilType.current
  val shapes: VeilShapes
    @Composable @ReadOnlyComposable get() = LocalVeilShapes.current
  val motion: VeilMotion
    @Composable @ReadOnlyComposable get() = LocalVeilMotion.current
  val reduceMotion: Boolean
    @Composable @ReadOnlyComposable get() = LocalVeilReduceMotion.current

  /** Maps Veil tokens into Material colors. Contrast on `background`: ink 17:1, secondary (ink2) ~8:1, primary (iris) ~6:1. */
  fun materialColors(c: VeilColors = VeilColors.Gece): Colors = darkColors(
    primary = c.iris,
    primaryVariant = c.mint,
    secondary = Color(0xFFA5A9AE), // ink2 composited on night: opaque so it is safe as text/icon colour everywhere
    secondaryVariant = c.night3,
    background = c.night,
    surface = c.night2,
    error = c.err,
    onPrimary = c.night,
    onSecondary = c.night,
    onBackground = c.ink,
    onSurface = c.ink,
    onError = c.night,
  )

  fun appColors(c: VeilColors = VeilColors.Gece): AppColors = AppColors(
    title = c.ink,
    primaryVariant2 = c.night3,
    // iris over night at 28% / 40% (solid so that text contrast does not depend on what is behind)
    sentMessage = Color(0xFF2F2E56),
    sentQuote = Color(0xFF3E3B72),
    // white over night2 at 12% / 16%
    receivedMessage = Color(0xFF2E343B),
    receivedQuote = Color(0xFF3B4149),
  )

  /** The theme that `ThemeManager.currentColors` returns when [forceVeilTheme] is on. */
  fun activeTheme(): ThemeManager.ActiveTheme = ThemeManager.ActiveTheme(
    name = DefaultTheme.DARK.themeName, // dark base keeps Android night mode / status bar icons correct
    base = DefaultTheme.DARK,
    colors = materialColors(),
    appColors = appColors(),
    wallpaper = AppWallpaper(), // no preset wallpaper: the Gece background is drawn by the screens
  )
}

/** Provides all Veil CompositionLocals. Call once near the root (done by `SimpleXTheme`). */
@Composable
fun VeilTheme(
  colors: VeilColors = VeilColors.Gece,
  reduceMotion: Boolean = rememberSystemReduceMotion(),
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalVeilColors provides colors,
    LocalVeilType provides VeilType.Default,
    LocalVeilShapes provides VeilShapes.Default,
    LocalVeilMotion provides VeilMotion.Default,
    LocalVeilReduceMotion provides reduceMotion,
    content = content
  )
}
