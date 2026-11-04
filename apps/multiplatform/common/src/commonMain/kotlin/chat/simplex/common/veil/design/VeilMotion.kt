package chat.simplex.common.veil.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/** Motion tokens. All durations should go through [duration] so reduce-motion is honoured in one place. */
@Immutable
class VeilMotion(
  /** Reveal / lift duration in ms. */
  val liftMillis: Int = 280,
  /** Tor ring breathing period in ms. */
  val ringBreatheMillis: Int = 2400,
  /** Dashed "connecting" ring rotation period in ms. */
  val ringSpinMillis: Int = 2600,
  /** Sheen drift period of the veil in ms. */
  val sheenMillis: Int = 5500,
) {
  /** cubic-bezier(0.2, 0.7, 0.2, 1): fast start, long soft landing. */
  val easeOut: Easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

  /** [baseMillis] unless [reduceMotion], in which case state changes become instant (0 ms). */
  fun duration(baseMillis: Int, reduceMotion: Boolean): Int = if (reduceMotion) 0 else baseMillis

  /** Lift duration for the given reduce-motion state. */
  fun lift(reduceMotion: Boolean): Int = duration(liftMillis, reduceMotion)

  companion object {
    val Default = VeilMotion()

    /** Android animator duration scale 0 means "remove animations". */
    fun reduceMotionForAnimatorScale(scale: Float): Boolean = scale == 0f
  }
}

/** True when the user asked to reduce motion. Android: system animator duration scale == 0. Desktop: always false. */
val LocalVeilReduceMotion = compositionLocalOf { false }

/** Observes the platform reduce-motion setting. */
@Composable
expect fun rememberSystemReduceMotion(): Boolean
