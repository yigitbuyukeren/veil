package chat.simplex.common.veil.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.pow
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.design.*'
/**
 * WCAG 2.x contrast of every text / surface token pair, in both themes. Text and icons that carry meaning need 4.5:1
 * (AA), strokes and UI parts 3:1. Measured ratios (Gece / Sis) are in `docs/veil/DESIGN.md`.
 */
class VeilContrastTest {
  private fun lin(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
  private fun luminance(c: Color): Double = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
  private fun ratio(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
  }

  private val themes = listOf("Gece" to VeilColors.Gece, "Sis" to VeilColors.Sis)

  private fun Color.on(bg: Color) = compositeOver(bg)
  private fun Color.alpha(a: Float) = copy(alpha = a)

  private fun gradientAt(c: VeilColors, t: Float): Color {
    val s = c.gradientStops
    for (i in 0 until s.size - 1) {
      val (p0, c0) = s[i]
      val (p1, c1) = s[i + 1]
      if (t <= p1) {
        val f = (t - p0) / (p1 - p0)
        return Color(c0.red + (c1.red - c0.red) * f, c0.green + (c1.green - c0.green) * f, c0.blue + (c1.blue - c0.blue) * f)
      }
    }
    return s.last().second
  }

  private fun assertAa(name: String, fg: Color, bg: Color, min: Double = 4.5) {
    val r = ratio(fg.on(bg), bg)
    assertTrue(r >= min, "$name: contrast ${"%.2f".format(r)} < $min")
  }

  @Test
  fun bodyTextOnSurfaces() {
    for ((n, c) in themes) {
      val surfaces = mapOf(
        "night" to c.night, "night2" to c.night2, "night3" to c.night3,
        "sheet1/night" to c.sheet1.on(c.night), "sheet2/night" to c.sheet2.on(c.night), "sheet3/night" to c.sheet3.on(c.night),
        "sheet3/night2" to c.sheet3.on(c.night2),
      )
      for ((sn, s) in surfaces) {
        assertAa("$n ink on $sn", c.ink, s)
        assertAa("$n ink2 on $sn", c.ink2, s)
        assertAa("$n ink3 on $sn", c.ink3, s)
      }
    }
  }

  @Test
  fun statusAndAccentText() {
    for ((n, c) in themes) {
      for ((sn, s) in listOf("night" to c.night, "night2" to c.night2)) {
        assertAa("$n warn on $sn", c.warn, s)
        assertAa("$n warn on warning chip over $sn", c.warn, c.warn.alpha(0.08f).on(s))
        assertAa("$n errText on $sn", c.errText, s)
        assertAa("$n errText on danger button over $sn", c.errText, c.err.alpha(0.10f).on(s))
        assertAa("$n accent on $sn", c.accent, s)
        assertAa("$n mintInk on $sn", c.mintInk, s)
      }
      assertAa("$n onAccent on accent", c.onAccent, c.accent)
    }
  }

  @Test
  fun labelOnGradientFill() {
    for ((n, c) in themes) {
      for (i in 0..20) assertAa("$n onGradient at ${i / 20f}", c.onGradient, gradientAt(c, i / 20f))
    }
  }

  @Test
  fun textOnBubbles() {
    for ((n, c) in themes) {
      // outgoing bubble: mint/iris/rose wash at 30% / 40% / 30% over the page; ink and ink2 (meta line) must read on it
      for ((t, a) in listOf(0f to 0.30f, 0.52f to 0.40f, 1f to 0.30f)) {
        val fill = gradientAt(c, t).alpha(a).on(c.night)
        assertAa("$n ink on outgoing bubble at $t", c.ink, fill)
        assertAa("$n ink2 on outgoing bubble at $t", c.ink2, fill)
      }
      val app = VeilTheme.appColors(c)
      for ((bn, b) in listOf("sent" to app.sentMessage, "sentQuote" to app.sentQuote, "received" to app.receivedMessage, "receivedQuote" to app.receivedQuote)) {
        assertAa("$n ink on $bn", c.ink, b)
        assertAa("$n ink2 on $bn", c.ink2, b)
      }
    }
  }

  @Test
  fun strokesAndUiParts() {
    for ((n, c) in themes) {
      for ((sn, s) in listOf("night" to c.night, "night3" to c.night3)) {
        for ((p, col) in c.strokeStops) assertAa("$n stroke stop $p on $sn", col, s, 3.0)
      }
      assertAa("$n err icon on night", c.err, c.night, 3.0)
      assertAa("$n warn ring on night", c.warn, c.night, 3.0)
      assertAa("$n accent track on night", c.accent, c.night, 3.0)
    }
    // Sis: the off track must stand out from the light page (Gece's off track is a faint white layer, as in the prototype)
    assertAa("Sis switch off track on night", VeilColors.Sis.switchOff, VeilColors.Sis.night, 3.0)
  }

  @Test
  fun materialMappingIsLightOnlyForSis() {
    assertFalse(VeilTheme.materialColors(VeilColors.Gece).isLight)
    assertTrue(VeilTheme.materialColors(VeilColors.Sis).isLight)
    for ((n, c) in themes) {
      val m = VeilTheme.materialColors(c)
      assertAa("$n material onBackground", m.onBackground, m.background)
      assertAa("$n material secondary on background", m.secondary, m.background)
      assertAa("$n material primary on background", m.primary, m.background)
      assertAa("$n material onPrimary on primary", m.onPrimary, m.primary)
      assertAa("$n material onSurface", m.onSurface, m.surface)
      assertAa("$n material error on background", m.error, m.background, 3.0)
    }
  }

  @Test
  fun lightPaletteIsNotAnInversion() {
    val s = VeilColors.Sis
    // misty: cool bias (blue above red), not pure grey, not warm cream
    assertTrue(s.night.blue > s.night.red && s.night.blue > s.night.green)
    assertTrue(s.night.blue - s.night.red > 0.02f)
    // same gradient identity for fills
    assertEquals(VeilColors.Gece.mint, s.mint)
    assertEquals(VeilColors.Gece.iris, s.iris)
    assertEquals(VeilColors.Gece.rose, s.rose)
    // layers are dark tints, not white ones
    assertTrue(luminance(s.wash) < 0.1)
    assertTrue(s.grainScale < VeilColors.Gece.grainScale)
  }
}
