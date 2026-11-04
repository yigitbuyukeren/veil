package chat.simplex.common.veil.design

import kotlin.test.*

class VeilThemeModeTest {
  @Test
  fun modeByOsSetting() {
    // (mode, OS dark) -> light?
    assertFalse(VeilThemeMode.Gece.isLight(systemDark = true))
    assertFalse(VeilThemeMode.Gece.isLight(systemDark = false))
    assertTrue(VeilThemeMode.Sis.isLight(systemDark = true))
    assertTrue(VeilThemeMode.Sis.isLight(systemDark = false))
    assertFalse(VeilThemeMode.Sistem.isLight(systemDark = true))
    assertTrue(VeilThemeMode.Sistem.isLight(systemDark = false))
  }

  @Test
  fun paletteFollowsMode() {
    assertSame(VeilColors.Gece, VeilThemeMode.Gece.colors(false))
    assertSame(VeilColors.Sis, VeilThemeMode.Sis.colors(true))
    assertSame(VeilColors.Gece, VeilThemeMode.Sistem.colors(true))
    assertSame(VeilColors.Sis, VeilThemeMode.Sistem.colors(false))
  }

  @Test
  fun preferenceRoundTripAndDefault() {
    for (m in VeilThemeMode.values()) assertEquals(m, VeilThemeMode.fromPref(m.prefValue))
    assertEquals(VeilThemeMode.Gece, VeilThemeMode.Default)
    assertEquals(VeilThemeMode.Gece, VeilThemeMode.fromPref(null))
    assertEquals(VeilThemeMode.Gece, VeilThemeMode.fromPref(""))
    assertEquals(VeilThemeMode.Gece, VeilThemeMode.fromPref("SYSTEM")) // an upstream value is not a Veil value
    assertEquals(listOf("gece", "sis", "sistem"), VeilThemeMode.values().map { it.prefValue })
  }
}
