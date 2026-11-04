package chat.simplex.common.veil.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.tan

/**
 * "Perde" (curtain): shows [content] behind a tulle veil until [revealed] is true.
 *
 * Veiled look: blurred content under a translucent tulle (white wash 13 % -> 6 %), a fine 45 degree weave, grain
 * and, when motion is allowed, a sheen drifting across every 5.5 s. Revealing lifts the veil (fades, slides up 6dp
 * and squashes to 35 % height) while the blur relaxes, over `VeilMotion.liftMillis` (280 ms, instant with
 * reduce-motion).
 *
 * Privacy: the veil never relies on blur alone. On Android API 26-30, where `Modifier.blur` does nothing, the
 * tulle is an almost opaque night layer, so the content stays unreadable. While veiled, the content is also hidden
 * from accessibility services. Use [perdeHold] to reveal while a finger is down.
 *
 * @param blurRadius content blur at full veil (API 31+ / desktop).
 */
@Composable
fun Perde(
  revealed: Boolean,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(10.dp),
  blurRadius: Dp = 6.dp,
  content: @Composable () -> Unit,
) {
  val motion = VeilTheme.motion
  val reduce = VeilTheme.reduceMotion
  val veil by animateFloatAsState(
    targetValue = if (revealed) 0f else 1f,
    animationSpec = tween(motion.lift(reduce), easing = motion.easeOut),
    label = "perde"
  )
  val colors = VeilTheme.colors
  val blurSupported = veilBlurSupported
  Box(modifier.clip(shape)) {
    Box(
      Modifier
        .then(if (veil > 0f && blurSupported) Modifier.blur(blurRadius * veil, BlurredEdgeTreatment.Unbounded) else Modifier)
        .then(if (!revealed) Modifier.clearAndSetSemantics { } else Modifier)
    ) { content() }
    if (veil > 0f) {
      val density = androidx.compose.ui.platform.LocalDensity.current
      val liftPx = with(density) { 6.dp.toPx() }
      Box(
        Modifier
          .matchParentSize()
          .graphicsLayer {
            alpha = veil
            translationY = -liftPx * (1f - veil)
            scaleY = 0.35f + 0.65f * veil
            transformOrigin = TransformOrigin(0.5f, 0f)
          }
          .drawBehind { drawTulle(colors, opaque = !blurSupported) }
          .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
      ) {
        if (!reduce) Sheen(Modifier.matchParentSize(), motion.sheenMillis)
      }
    }
  }
}

private fun DrawScope.drawTulle(colors: VeilColors, opaque: Boolean) {
  if (opaque) {
    // No blur on this device: hide the content with an (almost) opaque layer instead.
    drawRect(colors.night2.copy(alpha = 0.97f))
  }
  drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.06f))))
  // weave: two families of 1px diagonal lines, 3px apart
  val step = 3.dp.toPx() * 1.4142135f
  val line = 1.dp.toPx()
  var x = -size.height
  while (x < size.width) {
    drawLine(Color.White.copy(alpha = 0.06f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = line)
    drawLine(Color.White.copy(alpha = 0.045f), Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = line)
    x += step
  }
  drawRect(brush = veilGrainBrush, alpha = 0.2f, blendMode = BlendMode.Overlay)
}

/** Light band that slides across once per cycle (idle for the first half). */
@Composable
private fun Sheen(modifier: Modifier, periodMillis: Int) {
  val t = rememberInfiniteTransition(label = "sheen")
  val p by t.animateFloat(
    initialValue = 0f, targetValue = 1f,
    animationSpec = infiniteRepeatable(
      keyframes {
        durationMillis = periodMillis
        0f at 0
        0f at periodMillis / 2
        1f at periodMillis using FastOutSlowInEasing
      }
    ),
    label = "sheenPos"
  )
  Box(
    modifier.drawBehind {
      val bandW = size.width * 0.55f
      val left = -size.width * 0.6f + (size.width * 1.8f) * p
      val skew = tan(Math.toRadians(14.0)).toFloat() * size.height
      val path = Path().apply {
        moveTo(left + skew, -size.height * 0.1f)
        lineTo(left + bandW + skew, -size.height * 0.1f)
        lineTo(left + bandW - skew, size.height * 1.1f)
        lineTo(left - skew, size.height * 1.1f)
        close()
      }
      drawPath(
        path,
        Brush.horizontalGradient(
          listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
          startX = left - skew, endX = left + bandW + skew
        )
      )
    }
  )
}

/**
 * Press-and-hold helper for [Perde]: calls [onHoldChange] with true once the finger has been down for
 * [holdDelayMillis] (so scrolling a list does not flash content), and with false on release or cancel
 * (e.g. when the parent starts scrolling). [onTap] fires for a normal tap.
 */
fun Modifier.perdeHold(
  holdDelayMillis: Long = 150,
  onTap: (() -> Unit)? = null,
  onHoldChange: (held: Boolean) -> Unit,
): Modifier = pointerInput(holdDelayMillis, onTap, onHoldChange) {
  detectTapGestures(
    onTap = { onTap?.invoke() },
    onPress = {
      val releasedEarly = withTimeoutOrNull(holdDelayMillis) { tryAwaitRelease() }
      if (releasedEarly == null) {
        onHoldChange(true)
        try {
          tryAwaitRelease()
        } finally {
          onHoldChange(false)
        }
      }
    }
  )
}
