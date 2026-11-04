package chat.simplex.common.veil.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Veil "Tül" colour tokens. [Gece] is the dark-first palette taken from the design prototype;
 * a light "Sis" palette will be added later as another instance of this class.
 *
 * There is intentionally no "success green": confirmation is expressed with [mint].
 */
@Immutable
class VeilColors(
  val isLight: Boolean,
  /** Page background. */
  val night: Color,
  /** Raised surface (sheets, inputs when focused). */
  val night2: Color,
  /** Highest opaque surface (toasts, chip fill). */
  val night3: Color,
  /** Primary text. */
  val ink: Color,
  /** Secondary text (~68%). */
  val ink2: Color,
  /** Tertiary text and hints (~50%). */
  val ink3: Color,
  /** Translucent white layers, ascending elevation. */
  val sheet1: Color,
  val sheet2: Color,
  val sheet3: Color,
  /** Hairlines. */
  val line: Color,
  val line2: Color,
  val mint: Color,
  val iris: Color,
  val rose: Color,
  val warn: Color,
  val err: Color,
  /** Readable red for text on dark surfaces (danger button label). */
  val errText: Color,
) {
  /** Stops of the signature mint -> iris -> rose gradient, as (position, colour). */
  val gradientStops: Array<Pair<Float, Color>> = arrayOf(0f to mint, 0.52f to iris, 1f to rose)

  /** Signature gradient as a [Brush] (diagonal, size independent). Use [gradientFor] when the angle matters. */
  val gradient: Brush = Brush.linearGradient(colorStops = gradientStops)

  /** Colour of text and icons placed on top of the gradient. */
  val onGradient: Color get() = night

  /**
   * The signature gradient laid out like CSS `linear-gradient(<angle>deg, ...)` for an element of [size]
   * (gradient line length follows the CSS spec, so the end colours land exactly in the corners).
   */
  fun gradientFor(size: Size, angleDeg: Float = 120f): Brush {
    val (start, end) = cssGradientLine(size, angleDeg)
    return Brush.linearGradient(colorStops = gradientStops, start = start, end = end)
  }

  companion object {
    /** The dark "Gece" palette. */
    val Gece = VeilColors(
      isLight = false,
      night = Color(0xFF0B0F14),
      night2 = Color(0xFF111820),
      night3 = Color(0xFF16202A),
      ink = Color(0xFFEEF2F6),
      ink2 = Color(0xADEEF2F6),
      ink3 = Color(0x80EEF2F6),
      sheet1 = Color(0x0FFFFFFF),
      sheet2 = Color(0x17FFFFFF),
      sheet3 = Color(0x1FFFFFFF),
      line = Color(0x17FFFFFF),
      line2 = Color(0x29FFFFFF),
      mint = Color(0xFF7CF5D3),
      iris = Color(0xFF8A7CFF),
      rose = Color(0xFFFF8FB1),
      warn = Color(0xFFFFB547),
      err = Color(0xFFFF5D5D),
      errText = Color(0xFFFF8A8A),
    )
  }
}

/**
 * Start and end points of a CSS-style gradient line for a box of [size] and [angleDeg]
 * (0deg = towards the top, 90deg = towards the right, clockwise).
 */
fun cssGradientLine(size: Size, angleDeg: Float): Pair<Offset, Offset> {
  val a = Math.toRadians(angleDeg.toDouble())
  val dx = sin(a).toFloat()
  val dy = (-cos(a)).toFloat()
  val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
  val c = Offset(size.width / 2f, size.height / 2f)
  return Offset(c.x - dx * half, c.y - dy * half) to Offset(c.x + dx * half, c.y + dy * half)
}
