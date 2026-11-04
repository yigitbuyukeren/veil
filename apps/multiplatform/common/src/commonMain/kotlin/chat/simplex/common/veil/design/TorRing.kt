package chat.simplex.common.veil.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** State of the Tor proxy as shown by [TorRing]. */
enum class TorRingState {
  /** The local proxy answers. Gradient ring, breathing. */
  ProxyAnswers,
  /** Waiting for the proxy. Dashed ring, rotating. */
  Connecting,
  /** No Tor: nothing is sent. Amber static ring with a slash. */
  Unavailable,
}

// TODO(veil): move these to strings.xml together with the other Turkish UI texts.
const val TOR_RING_LABEL_PROXY_ANSWERS = "Proxy yanıt veriyor"
const val TOR_RING_LABEL_CONNECTING = "Bağlanıyor"
const val TOR_RING_LABEL_UNAVAILABLE = "Tor yok — hiçbir şey gönderilmiyor"

/** The exact Turkish status text for a ring state. */
fun torRingLabel(state: TorRingState): String = when (state) {
  TorRingState.ProxyAnswers -> TOR_RING_LABEL_PROXY_ANSWERS
  TorRingState.Connecting -> TOR_RING_LABEL_CONNECTING
  TorRingState.Unavailable -> TOR_RING_LABEL_UNAVAILABLE
}

/**
 * Tor status ring (prototype `ring()`): 32-unit viewBox, outer ring r=12 / stroke 2.6, inner dot r=5.
 *  - [TorRingState.ProxyAnswers]: gradient ring + dot, breathing scale .88..1.1 and opacity .82..1 over 2.4s, soft glow.
 *  - [TorRingState.Connecting]: dashed ring (3.2 / 5.2) rotating, dot at 35%.
 *  - [TorRingState.Unavailable]: static amber ring, dot at 35%, slash from bottom-left to top-right.
 * With reduce-motion on, nothing moves (the ring shows its steady frame).
 */
@Composable
fun TorRing(state: TorRingState, size: Dp = 24.dp, modifier: Modifier = Modifier) {
  val colors = VeilTheme.colors
  val motion = VeilTheme.motion
  val reduce = VeilTheme.reduceMotion
  val transition = rememberInfiniteTransition(label = "torRing")
  val breathe by transition.animateFloat(
    initialValue = 0f, targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(motion.ringBreatheMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "breathe"
  )
  val spin by transition.animateFloat(
    initialValue = 0f, targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(motion.ringSpinMillis, easing = LinearEasing)),
    label = "spin"
  )
  val label = torRingLabel(state)
  Canvas(modifier.size(size).semantics { contentDescription = label }) {
    val u = this.size.minDimension / 32f
    val c = Offset(16f * u, 16f * u)
    val grad = Brush.linearGradient(colorStops = colors.strokeStops, start = Offset(4f * u, 4f * u), end = Offset(28f * u, 28f * u))
    when (state) {
      TorRingState.ProxyAnswers -> {
        val t = if (reduce) 0.5f else breathe
        val scale = 0.88f + (1.1f - 0.88f) * t
        val alpha = if (reduce) 1f else 0.82f + (1f - 0.82f) * t
        withTransform({ scale(scale, scale, c) }) {
          drawCircle(grad, radius = 12f * u, center = c, alpha = alpha * 0.22f, style = Stroke(6f * u)) // glow
          drawCircle(grad, radius = 12f * u, center = c, alpha = alpha, style = Stroke(2.6f * u))
          drawCircle(grad, radius = 5f * u, center = c, alpha = alpha * 0.9f)
        }
      }
      TorRingState.Connecting -> {
        val angle = if (reduce) 0f else spin
        withTransform({ rotate(angle, c) }) {
          drawCircle(
            grad, radius = 12f * u, center = c,
            style = Stroke(2.6f * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.2f * u, 5.2f * u)))
          )
        }
        drawCircle(grad, radius = 5f * u, center = c, alpha = 0.35f)
      }
      TorRingState.Unavailable -> {
        drawCircle(colors.warn, radius = 12f * u, center = c, style = Stroke(2.6f * u))
        drawCircle(colors.warn, radius = 5f * u, center = c, alpha = 0.35f)
        drawLine(colors.warn, Offset(6f * u, 26f * u), Offset(26f * u, 6f * u), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
      }
    }
  }
}
