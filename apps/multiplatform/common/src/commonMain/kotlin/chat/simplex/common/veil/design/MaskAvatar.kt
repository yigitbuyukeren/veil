package chat.simplex.common.veil.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val IncColor = Color(0xFFB9B4E6)
private val IncBadgeIcon = Color(0xFFCFCBF2)

/**
 * Deterministic generative avatar ("mask"). The same [seed] always draws the same mask; [incognito] draws the
 * dashed hexagon family and adds a small dashed badge at the bottom-right corner (the badge overflows the avatar
 * bounds by 3dp like in the prototype, so leave a little room around incognito avatars).
 *
 * The geometry comes from [generateMaskSpec], an exact port of the prototype's `hash` / `rng` / `avatar`.
 */
@Composable
fun MaskAvatar(seed: String, size: Dp, incognito: Boolean = false, modifier: Modifier = Modifier) {
  val spec = remember(seed, incognito) { generateMaskSpec(seed, incognito) }
  val colors = VeilTheme.colors
  val lineColor = colors.line2
  val night = colors.night
  Box(modifier.size(size)) {
    Canvas(Modifier.size(size)) {
      val s = this.size.minDimension
      val corner = CornerRadius(s * 0.36f)
      val clip = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, s, s), corner)) }
      clipPath(clip) {
        withTransform({ scale(s / 48f, s / 48f, Offset.Zero) }) {
          if (spec.incognito) drawIncognito(spec) else drawNormal(spec)
        }
      }
      // inset 1px hairline
      drawRoundRect(lineColor, topLeft = Offset(0.5f, 0.5f), size = Size(s - 1f, s - 1f), cornerRadius = corner, style = Stroke(1f))
    }
    if (incognito) {
      val badge = maxOf(size * 0.42f, 14.dp)
      Canvas(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(badge)) {
        drawIncognitoBadge(night)
      }
    }
  }
}

/** Linear gradient on a bounding box with CSS/SVG `gradientTransform="rotate(deg .5 .5)"` semantic, default (0,0)->(1,0). */
private fun bboxGradient(x: Float, y: Float, w: Float, h: Float, rotDeg: Int, stops: Array<Pair<Float, Color>>): Brush {
  val a = Math.toRadians(rotDeg.toDouble())
  fun rot(px: Double, py: Double): Offset {
    val dx = px - 0.5
    val dy = py - 0.5
    val rx = 0.5 + dx * cos(a) - dy * sin(a)
    val ry = 0.5 + dx * sin(a) + dy * cos(a)
    return Offset((x + rx * w).toFloat(), (y + ry * h).toFloat())
  }
  return Brush.linearGradient(colorStops = stops, start = rot(0.0, 0.0), end = rot(1.0, 0.0))
}

private val GradMain = arrayOf(0f to Color(0xFF7CF5D3), 0.52f to Color(0xFF8A7CFF), 1f to Color(0xFFFF8FB1))
private val GradBack = arrayOf(0f to Color(0xFFFF8FB1), 1f to Color(0xFF7CF5D3))

private fun DrawScope.drawNormal(spec: MaskSpec) {
  drawRect(Color(0xFF121A24), size = Size(48f, 48f))
  val bx = spec.blobX.toFloat()
  val by = spec.blobY.toFloat()
  val br = spec.blobR.toFloat()
  drawCircle(bboxGradient(bx - br, by - br, 2 * br, 2 * br, spec.gradRotB, GradBack), radius = br, center = Offset(bx, by), alpha = 0.16f)
  for (l in spec.layers) when (l) {
    is MaskLayer.Arc -> {
      val cx = l.cx.toFloat()
      val cy = l.cy.toFloat()
      val r = l.r.toFloat()
      val brush = bboxGradient(cx - r, cy - r, 2 * r, 2 * r, spec.gradRotA, GradMain)
      // SVG dash starts at 3 o'clock and runs clockwise; the whole element is then rotated by `rotation`.
      drawArc(
        brush = brush, startAngle = l.rotation.toFloat(), sweepAngle = 360f * l.dashFraction.toFloat(), useCenter = false,
        topLeft = Offset(cx - r, cy - r), size = Size(2 * r, 2 * r), style = Stroke(l.strokeWidth.toFloat(), cap = StrokeCap.Round)
      )
    }
    is MaskLayer.Triangle -> {
      val xs = l.points.map { it.first.toFloat() }
      val ys = l.points.map { it.second.toFloat() }
      val x0 = xs.min()
      val y0 = ys.min()
      val brush = bboxGradient(x0, y0, maxOf(xs.max() - x0, 0.01f), maxOf(ys.max() - y0, 0.01f), spec.gradRotA, GradMain)
      val p = Path().apply {
        moveTo(xs[0], ys[0]); lineTo(xs[1], ys[1]); lineTo(xs[2], ys[2]); close()
      }
      drawPath(p, brush, alpha = l.opacity.toFloat())
    }
    is MaskLayer.Orbit -> {
      val cx = l.cx.toFloat()
      val cy = l.cy.toFloat()
      val r = l.r.toFloat()
      drawCircle(bboxGradient(cx - r, cy - r, 2 * r, 2 * r, spec.gradRotB, GradBack), radius = r, center = Offset(cx, cy), alpha = 0.85f, style = Stroke(1.6f))
      val r2 = r * 0.55f
      drawCircle(bboxGradient(cx - r2, cy - r2, 2 * r2, 2 * r2, spec.gradRotA, GradMain), radius = r2, center = Offset(cx, cy), alpha = 0.6f, style = Stroke(1.6f))
    }
  }
}

private fun DrawScope.drawIncognito(spec: MaskSpec) {
  val bg = Color(0xFF141922)
  drawRect(bg, size = Size(48f, 48f))
  // hatch pattern: vertical hairlines every 5 units, rotated by the pattern angle about the origin
  withTransform({ rotate(spec.patternAngle.toFloat(), Offset.Zero) }) {
    var x = -80f
    while (x < 80f) {
      drawLine(IncColor.copy(alpha = 0.28f), Offset(x, -80f), Offset(x, 80f), strokeWidth = 0.7f)
      x += 5f
    }
  }
  val dash = PathEffect.dashPathEffect(floatArrayOf(2f, 2.5f))
  for (k in 0 until 4) {
    val rr = incognitoHexRadius(k)
    val path = Path()
    for (j in 0 until 6) {
      val (px, py) = hexVertex(spec.hexCx, spec.hexCy, rr, spec.hexRot, j)
      if (j == 0) path.moveTo(px.toFloat(), py.toFloat()) else path.lineTo(px.toFloat(), py.toFloat())
    }
    path.close()
    drawPath(path, IncColor, alpha = (0.9f - k * 0.17f), style = Stroke(1.3f, pathEffect = if (k % 2 == 1) dash else null))
  }
  withTransform({ rotate(spec.barRot.toFloat(), Offset(24f, 24f)) }) {
    drawRect(bg, topLeft = Offset(spec.barX.toFloat(), -4f), size = Size(spec.barW.toFloat(), 60f), alpha = 0.82f)
  }
  for (d in spec.dots) drawCircle(IncColor, radius = 1.1f, center = Offset(d.first.toFloat(), d.second.toFloat()), alpha = 0.6f)
}

/** The small dashed "mask" badge: night disc, 1.5px dashed ring, mask glyph at 62%. */
private fun DrawScope.drawIncognitoBadge(night: Color) {
  val s = size.minDimension
  drawCircle(night, radius = s / 2f)
  val ring = 1.5.dp.toPx()
  drawCircle(
    IncColor, radius = s / 2f - ring / 2f, style = Stroke(ring, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ring * 1.6f, ring * 1.6f)))
  )
  val g = s * 0.62f
  val o = (s - g) / 2f
  withTransform({ translate(o, o); scale(g / 24f, g / 24f, Offset.Zero) }) {
    drawPath(maskGlyph, IncBadgeIcon, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

/** Prototype icon `mask` (24x24 viewBox): M3 9c3-2 6-2 9 0 3-2 6-2 9 0 0 5-3 8-5 8-2 0-3-1.5-4-1.5S9 17 7 17c-2 0-4-3-4-8z */
private val maskGlyph: Path by lazy {
  Path().apply {
    moveTo(3f, 9f)
    cubicTo(6f, 7f, 9f, 7f, 12f, 9f)
    cubicTo(15f, 7f, 18f, 7f, 21f, 9f)
    cubicTo(21f, 14f, 18f, 17f, 16f, 17f)
    cubicTo(14f, 17f, 13f, 15.5f, 12f, 15.5f)
    cubicTo(11f, 15.5f, 9f, 17f, 7f, 17f)
    cubicTo(5f, 17f, 3f, 14f, 3f, 9f)
    close()
  }
}
