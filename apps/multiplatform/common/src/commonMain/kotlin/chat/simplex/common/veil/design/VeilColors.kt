package chat.simplex.common.veil.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Veil "Tül" colour tokens. [Gece] is the dark-first palette taken from the design prototype; [Sis] is the light
 * palette (misty off-white with a faint iris bias, ink near-black, the same gradient identity).
 *
 * Token names keep their dark-theme origin (`night` = page background, `night2` / `night3` = raised surfaces); in [Sis]
 * they are the light equivalents. Translucent layers are built from [wash]: white in Gece, a dark iris-ink in Sis.
 *
 * There is intentionally no "success green": confirmation is expressed with [mint].
 *
 * Every text / surface pair is checked against WCAG AA in `VeilContrastTest`.
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
  /** Secondary text (~72%). */
  val ink2: Color,
  /** Tertiary text and hints (~59%). */
  val ink3: Color,
  /** Translucent layers (of [wash]), ascending elevation. */
  val sheet1: Color,
  val sheet2: Color,
  val sheet3: Color,
  /** Hairlines. */
  val line: Color,
  val line2: Color,
  val mint: Color,
  val iris: Color,
  val rose: Color,
  /** Warning colour for text and outlines on this theme's surfaces. */
  val warn: Color,
  val err: Color,
  /** Readable red for text on surfaces (danger button label, failed state). */
  val errText: Color,
  /** Colour of text and icons placed on top of the gradient. */
  val onGradient: Color,
  /** Accent for text, cursors, switch tracks and thin lines on surfaces (readable; [iris] itself in Gece). */
  val accent: Color,
  /** Colour of text on a solid [accent] fill. */
  val onAccent: Color,
  /** Mint as text or icon colour on surfaces (links, "on" state). */
  val mintInk: Color,
  /** Stops of the gradient used for thin strokes (rings, outlines): same as [gradientStops] in Gece, deeper in Sis. */
  val strokeStops: Array<Pair<Float, Color>>,
  /** Base colour of translucent overlays: white on dark, iris-ink on light. */
  val wash: Color,
  /** Opaque base of bottom sheets. */
  val sheetBase: Color,
  /** Scrim behind sheets and dialogs. */
  val scrim: Color,
  /** Off track of switches (must be visible against [night]). */
  val switchOff: Color,
  /** Multiplier of grain alpha (grain is lighter on light themes). */
  val grainScale: Float,
  /** Tulle wash laid over the blurred content of a veil (top and bottom of the gradient), and its sheen band. */
  val tulleTop: Color,
  val tulleBottom: Color,
  val sheen: Color,
  /** Shading of the folded corner of incoming / outgoing bubbles, and of a quote inside a bubble. */
  val flapIncoming: Color,
  val flapIncomingHi: Color,
  val flapOutgoing: Color,
  val quoteShade: Color,
  /** Mask avatar tile backgrounds (normal and incognito) and the incognito hatch / badge-icon colours. */
  val maskBg: Color,
  val maskBgIncognito: Color,
  val incognitoInk: Color,
  val incognitoBadgeIcon: Color,
) {
  /** Stops of the signature mint -> iris -> rose gradient (fills), as (position, colour). */
  val gradientStops: Array<Pair<Float, Color>> = arrayOf(0f to mint, 0.52f to iris, 1f to rose)

  /** Signature gradient for strokes and small marks ([strokeStops]) as a [Brush] (diagonal, size independent). Use [gradientFor] for fills. */
  val gradient: Brush = Brush.linearGradient(colorStops = strokeStops)

  /** [ink2] flattened onto [night]: an opaque colour safe as text / icon colour everywhere (Material `secondary`). */
  val ink2Solid: Color get() = ink2.compositeOver(night)

  /**
   * The signature gradient laid out like CSS `linear-gradient(<angle>deg, ...)` for an element of [size]
   * (gradient line length follows the CSS spec, so the end colours land exactly in the corners).
   */
  fun gradientFor(size: Size, angleDeg: Float = 120f): Brush {
    val (start, end) = cssGradientLine(size, angleDeg)
    return Brush.linearGradient(colorStops = gradientStops, start = start, end = end)
  }

  /** Like [gradientFor] but with the stroke stops (for thin lines and rings, readable on this theme's surfaces). */
  fun strokeGradientFor(size: Size, angleDeg: Float = 120f): Brush {
    val (start, end) = cssGradientLine(size, angleDeg)
    return Brush.linearGradient(colorStops = strokeStops, start = start, end = end)
  }

  companion object {
    private val MintFill = Color(0xFF7CF5D3)
    private val IrisFill = Color(0xFF8A7CFF)
    private val RoseFill = Color(0xFFFF8FB1)

    /** The dark "Gece" palette. */
    val Gece = VeilColors(
      isLight = false,
      night = Color(0xFF0B0F14),
      night2 = Color(0xFF111820),
      night3 = Color(0xFF16202A),
      ink = Color(0xFFEEF2F6),
      ink2 = Color(0xB8EEF2F6),
      ink3 = Color(0x96EEF2F6),
      sheet1 = Color(0x0FFFFFFF),
      sheet2 = Color(0x17FFFFFF),
      sheet3 = Color(0x1FFFFFFF),
      line = Color(0x17FFFFFF),
      line2 = Color(0x29FFFFFF),
      mint = MintFill,
      iris = IrisFill,
      rose = RoseFill,
      warn = Color(0xFFFFB547),
      err = Color(0xFFFF5D5D),
      errText = Color(0xFFFF8A8A),
      onGradient = Color(0xFF0B0F14),
      accent = IrisFill,
      onAccent = Color(0xFF0B0F14),
      mintInk = MintFill,
      strokeStops = arrayOf(0f to MintFill, 0.52f to IrisFill, 1f to RoseFill),
      wash = Color.White,
      sheetBase = Color(0xFF10161D),
      scrim = Color.Black.copy(alpha = 0.55f),
      switchOff = Color(0x1FFFFFFF),
      grainScale = 1f,
      tulleTop = Color.White.copy(alpha = 0.13f),
      tulleBottom = Color.White.copy(alpha = 0.06f),
      sheen = Color.White.copy(alpha = 0.28f),
      flapIncoming = Color.Black.copy(alpha = 0.28f),
      flapIncomingHi = Color.White.copy(alpha = 0.07f),
      flapOutgoing = Color(0xFF0B0F14).copy(alpha = 0.45f),
      quoteShade = Color.Black.copy(alpha = 0.18f),
      maskBg = Color(0xFF121A24),
      maskBgIncognito = Color(0xFF141922),
      incognitoInk = Color(0xFFB9B4E6),
      incognitoBadgeIcon = Color(0xFFCFCBF2),
    )

    /**
     * The light "Sis" (mist) palette. Backgrounds are cool off-white with a faint iris bias; ink is near-black blue;
     * layers are dark-iris alpha tints. Fills (buttons, bubbles, badges) keep the bright mint -> iris -> rose gradient
     * with dark labels; thin strokes, accents and links use deeper stops because the bright ones do not reach 3:1 on
     * a light page.
     */
    val Sis = VeilColors(
      isLight = true,
      night = Color(0xFFECEFF7),
      night2 = Color(0xFFF5F6FB),
      night3 = Color(0xFFFFFFFF),
      ink = Color(0xFF0D1121),
      ink2 = Color(0xC80D1121),
      ink3 = Color(0xA80D1121),
      sheet1 = Color(0x0A1B2140),
      sheet2 = Color(0x111B2140),
      sheet3 = Color(0x1A1B2140),
      line = Color(0x141B2140),
      line2 = Color(0x291B2140),
      mint = MintFill,
      iris = IrisFill,
      rose = RoseFill,
      warn = Color(0xFF8A4B00),
      err = Color(0xFFC42B3A),
      errText = Color(0xFFB3202F),
      onGradient = Color(0xFF0D1121),
      accent = Color(0xFF5A4BE6),
      onAccent = Color.White,
      mintInk = Color(0xFF00725A),
      strokeStops = arrayOf(0f to Color(0xFF0A9275), 0.52f to Color(0xFF5A4BE6), 1f to Color(0xFFD63E78)),
      wash = Color(0xFF1B2140),
      sheetBase = Color(0xFFF5F6FB),
      scrim = Color(0xFF0D1121).copy(alpha = 0.42f),
      switchOff = Color(0xFF1B2140).copy(alpha = 0.50f),
      grainScale = 0.5f,
      tulleTop = Color(0xFFFFFFFF).copy(alpha = 0.62f),
      tulleBottom = Color(0xFFFFFFFF).copy(alpha = 0.46f),
      sheen = Color.White.copy(alpha = 0.70f),
      flapIncoming = Color(0xFF1B2140).copy(alpha = 0.16f),
      flapIncomingHi = Color.Transparent,
      flapOutgoing = Color(0xFF1B2140).copy(alpha = 0.18f),
      quoteShade = Color(0xFF1B2140).copy(alpha = 0.07f),
      maskBg = Color(0xFFE2E6F2),
      maskBgIncognito = Color(0xFFE4E3F1),
      incognitoInk = Color(0xFF5F58A8),
      incognitoBadgeIcon = Color(0xFF4B4590),
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
