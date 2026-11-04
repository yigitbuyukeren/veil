package chat.simplex.common.veil.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** One step of a bubble outline, kept as plain data so the geometry is unit-testable without a graphics backend. */
sealed class BubbleOp {
  data class MoveTo(val x: Float, val y: Float) : BubbleOp()
  data class LineTo(val x: Float, val y: Float) : BubbleOp()
  /** Quarter-circle corner of radius [r]; [x]/[y] is where the arc ends, ([cx],[cy]) its centre. */
  data class Corner(val cx: Float, val cy: Float, val r: Float, val startDeg: Float, val x: Float, val y: Float) : BubbleOp()
  object Close : BubbleOp()
}

/**
 * Outline of a chat bubble with one cut ("folded") corner, as in the prototype's clip-paths:
 *  - incoming: top-left corner is cut by [fold]; the other three corners are rounded with [radius];
 *  - outgoing: top-right corner is cut; the other three are rounded.
 * Radii and fold are clamped so tiny bubbles stay valid.
 */
fun foldBubbleOps(size: Size, outgoing: Boolean, radius: Float, fold: Float): List<BubbleOp> {
  val w = size.width
  val h = size.height
  val r = radius.coerceAtMost(minOf(w, h) / 2f).coerceAtLeast(0f)
  val f = fold.coerceAtMost(minOf(w, h) / 2f).coerceAtLeast(0f)
  val ops = ArrayList<BubbleOp>()
  if (!outgoing) {
    // start after the cut on the top edge, go clockwise
    ops += BubbleOp.MoveTo(f, 0f)
    ops += BubbleOp.LineTo(w - r, 0f)
    ops += BubbleOp.Corner(w - r, r, r, 270f, w, r)
    ops += BubbleOp.LineTo(w, h - r)
    ops += BubbleOp.Corner(w - r, h - r, r, 0f, w - r, h)
    ops += BubbleOp.LineTo(r, h)
    ops += BubbleOp.Corner(r, h - r, r, 90f, 0f, h - r)
    ops += BubbleOp.LineTo(0f, f)
    ops += BubbleOp.Close // closes with the diagonal (0,f) -> (f,0): the cut
  } else {
    ops += BubbleOp.MoveTo(r, 0f)
    ops += BubbleOp.LineTo(w - f, 0f)
    ops += BubbleOp.LineTo(w, f) // the cut
    ops += BubbleOp.LineTo(w, h - r)
    ops += BubbleOp.Corner(w - r, h - r, r, 0f, w - r, h)
    ops += BubbleOp.LineTo(r, h)
    ops += BubbleOp.Corner(r, h - r, r, 90f, 0f, h - r)
    ops += BubbleOp.LineTo(0f, r)
    ops += BubbleOp.Corner(r, r, r, 180f, r, 0f)
    ops += BubbleOp.Close
  }
  return ops
}

/** Builds a Compose [Path] from [foldBubbleOps]. */
fun foldBubblePath(size: Size, outgoing: Boolean, radius: Float, fold: Float): Path = Path().apply {
  for (op in foldBubbleOps(size, outgoing, radius, fold)) when (op) {
    is BubbleOp.MoveTo -> moveTo(op.x, op.y)
    is BubbleOp.LineTo -> lineTo(op.x, op.y)
    is BubbleOp.Corner -> if (op.r > 0f) arcTo(
      androidx.compose.ui.geometry.Rect(op.cx - op.r, op.cy - op.r, op.cx + op.r, op.cy + op.r), op.startDeg, 90f, false
    ) else lineTo(op.x, op.y)
    BubbleOp.Close -> close()
  }
}

/** Bubble outline with the folded corner (see [foldBubbleOps]). */
class FoldBubbleShape(
  val outgoing: Boolean,
  val radius: Dp = 18.dp,
  val fold: Dp = 15.dp,
) : Shape {
  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
    Outline.Generic(foldBubblePath(size, outgoing, with(density) { radius.toPx() }, with(density) { fold.toPx() }))
}

/**
 * Chat bubble container: [FoldBubbleShape] clip, incoming = white 12% layer, outgoing = diagonal mint/iris/rose wash,
 * plus the shaded flap that sells the fold. [content] is laid out inside 13dp/9dp padding.
 */
@Composable
fun VeilBubble(
  outgoing: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val colors = VeilTheme.colors
  val shapes = VeilTheme.shapes
  val shape = shapes.bubble(outgoing)
  val fill: Brush = if (outgoing) {
    Brush.linearGradient(
      colorStops = arrayOf(
        0f to colors.mint.copy(alpha = 0.30f),
        0.55f to colors.iris.copy(alpha = 0.40f),
        1f to colors.rose.copy(alpha = 0.30f)
      )
    )
  } else SolidColor(colors.sheet3)
  val foldPx = with(androidx.compose.ui.platform.LocalDensity.current) { shapes.bubbleFold.toPx() }
  Box(
    modifier
      .clip(shape)
      .background(fill)
      .drawWithContent {
        drawContent()
        // the flap: a small triangle in the cut corner, shaded darker than the bubble
        val f = foldPx.coerceAtMost(minOf(size.width, size.height) / 2f)
        if (!outgoing) {
          val p = Path().apply { moveTo(f, 0f); lineTo(0f, f); lineTo(f, f); close() }
          drawPath(p, colors.flapIncoming)
          drawPath(p, colors.flapIncomingHi)
        } else {
          val x = size.width
          val p = Path().apply { moveTo(x - f, 0f); lineTo(x - f, f); lineTo(x, f); close() }
          drawPath(p, colors.flapOutgoing)
        }
      }
      .padding(start = 13.dp, end = 13.dp, top = 9.dp, bottom = 7.dp)
  ) { content() }
}
