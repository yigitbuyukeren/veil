package chat.simplex.common.veil.design

/**
 * The user's theme choice. Stored as [prefValue] in the Veil preference `appPrefs.veilTheme` (not upstream's
 * `currentTheme`: upstream's value space is SYSTEM / LIGHT / DARK / SIMPLEX / BLACK and is exported and imported with
 * the app settings, so Veil keeps its own three values and never has to translate between the two).
 *
 * [Gece] is dark, [Sis] is light, [Sistem] follows the operating system. The default is [Gece].
 */
enum class VeilThemeMode(val prefValue: String) {
  Gece("gece"),
  Sis("sis"),
  Sistem("sistem");

  /** True when this mode draws the light palette, given whether the OS is currently in dark mode. */
  fun isLight(systemDark: Boolean): Boolean = when (this) {
    Gece -> false
    Sis -> true
    Sistem -> !systemDark
  }

  /** The palette to draw for this mode. */
  fun colors(systemDark: Boolean): VeilColors = if (isLight(systemDark)) VeilColors.Sis else VeilColors.Gece

  companion object {
    val Default = Gece

    /** Parses a stored value; unknown, empty or missing values give [Default]. */
    fun fromPref(value: String?): VeilThemeMode = values().firstOrNull { it.prefValue == value } ?: Default
  }
}
