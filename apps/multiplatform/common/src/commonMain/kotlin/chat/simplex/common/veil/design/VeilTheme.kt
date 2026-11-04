package chat.simplex.common.veil.design

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import chat.simplex.common.ui.theme.AppColors
import chat.simplex.common.ui.theme.AppWallpaper
import chat.simplex.common.ui.theme.DefaultTheme
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.ui.theme.ThemeManager
import chat.simplex.common.ui.theme.systemInDarkThemeCurrently

val LocalVeilColors = staticCompositionLocalOf { VeilColors.Gece }
val LocalVeilType = staticCompositionLocalOf { VeilType.Default }
val LocalVeilShapes = staticCompositionLocalOf { VeilShapes.Default }
val LocalVeilMotion = staticCompositionLocalOf { VeilMotion.Default }

/**
 * Entry point of the Veil design system.
 *
 * Wiring:
 *  - `ThemeManager.currentColors` returns [activeTheme] (the Gece or Sis palette, chosen by the user's [VeilThemeMode],
 *    mapped into upstream `Colors`/`AppColors`) while [forceVeilTheme] is true, so every not-yet-redesigned upstream
 *    screen renders in the Veil look. Upstream's theme choice and per-user/per-chat overrides are ignored.
 *  - `SimpleXTheme` wraps its content in [VeilTheme] (the composable below) which provides the CompositionLocals
 *    for the palette that matches the active Material colors,
 *    and uses [VeilMaterialTypography] / [VeilMaterialShapes] for Material.
 *
 * Read tokens with `VeilTheme.colors`, `.type`, `.shapes`, `.motion`; `VeilTheme.reduceMotion` for the motion flag.
 */
object VeilTheme {
  /** The whole app uses the Veil palettes (Gece / Sis) instead of upstream's themes. Flip to false to restore upstream themes. */
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

  /** The user's theme choice (preference `veilTheme`), [VeilThemeMode.Gece] by default. */
  fun mode(): VeilThemeMode = VeilThemeMode.fromPref(appPrefs.veilTheme.get())

  /** Stores [mode] and re-applies the theme: colours, Android night mode and status / navigation bar icon colours. */
  fun setMode(mode: VeilThemeMode) {
    appPrefs.veilTheme.set(mode.prefValue)
    ThemeManager.applyTheme(appPrefs.currentTheme.get()!!)
  }

  /** Maps Veil tokens into Material colors. Contrast on `background` is listed in `VeilContrastTest`. */
  fun materialColors(c: VeilColors = VeilColors.Gece): Colors {
    val secondary = c.ink2Solid // opaque, so it is safe as text / icon colour everywhere
    return if (c.isLight) lightColors(
      primary = c.accent,
      primaryVariant = c.mintInk,
      secondary = secondary,
      secondaryVariant = c.night3,
      background = c.night,
      surface = c.night2,
      error = c.err,
      onPrimary = c.onAccent,
      onSecondary = c.night,
      onBackground = c.ink,
      onSurface = c.ink,
      onError = Color.White,
    ) else darkColors(
      primary = c.accent,
      primaryVariant = c.mintInk,
      secondary = secondary,
      secondaryVariant = c.night3,
      background = c.night,
      surface = c.night2,
      error = c.err,
      onPrimary = c.onAccent,
      onSecondary = c.night,
      onBackground = c.ink,
      onSurface = c.ink,
      onError = c.night,
    )
  }

  fun appColors(c: VeilColors = VeilColors.Gece): AppColors =
    if (c.isLight) AppColors(
      title = c.ink,
      primaryVariant2 = c.night3,
      // iris over the light page at 28% / 40% (solid so that text contrast does not depend on what is behind)
      sentMessage = Color(0xFFD3D1F7),
      sentQuote = Color(0xFFC4C1F4),
      // wash over the light page at 10% / 15%
      receivedMessage = Color(0xFFD9DCE7),
      receivedQuote = Color(0xFFCDD0DD),
    ) else AppColors(
      title = c.ink,
      primaryVariant2 = c.night3,
      // iris over night at 28% / 40% (solid so that text contrast does not depend on what is behind)
      sentMessage = Color(0xFF2F2E56),
      sentQuote = Color(0xFF3E3B72),
      // white over night2 at 12% / 16%
      receivedMessage = Color(0xFF2E343B),
      receivedQuote = Color(0xFF3B4149),
    )

  /**
   * The theme that `ThemeManager.currentColors` returns when [forceVeilTheme] is on: the palette of [mode] mapped into
   * Material `Colors` / `AppColors`. Its name is `SYSTEM` for [VeilThemeMode.Sistem] so that Android night mode stays
   * automatic, otherwise the base theme's name (which forces night mode on or off). The light / dark base keeps the
   * status and navigation bar icon colours right (`ThemeManager.applyTheme` reads `colors.isLight`).
   */
  fun activeTheme(mode: VeilThemeMode = mode(), systemDark: Boolean = systemInDarkThemeCurrently): ThemeManager.ActiveTheme {
    val palette = mode.colors(systemDark)
    val base = if (palette.isLight) DefaultTheme.LIGHT else DefaultTheme.DARK
    return ThemeManager.ActiveTheme(
      name = if (mode == VeilThemeMode.Sistem) DefaultTheme.SYSTEM_THEME_NAME else base.themeName,
      base = base,
      colors = materialColors(palette),
      appColors = appColors(palette),
      wallpaper = AppWallpaper(), // no preset wallpaper: the background is drawn by the screens
    )
  }
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
