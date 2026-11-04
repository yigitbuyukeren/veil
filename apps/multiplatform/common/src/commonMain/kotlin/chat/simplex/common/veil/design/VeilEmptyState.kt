package chat.simplex.common.veil.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Veil mark: three nested rings in the signature gradient, fading outwards like layers of tulle.
 * Decorative (no semantics); used by the splash screen and as the default empty-state illustration.
 */
@Composable
fun VeilMark(size: Dp = 72.dp, modifier: Modifier = Modifier) {
  val colors = VeilTheme.colors
  Canvas(modifier.size(size)) {
    val s = this.size.minDimension
    val stroke = (s * 0.045f).coerceAtLeast(1.5f)
    val center = Offset(s / 2f, s / 2f)
    val brush = colors.gradientFor(this.size, 120f)
    val radii = floatArrayOf(0.46f, 0.32f, 0.18f)
    val alphas = floatArrayOf(0.35f, 0.65f, 1f)
    for (i in radii.indices) {
      drawCircle(brush, radius = s * radii[i] - stroke / 2f, center = center, alpha = alphas[i], style = Stroke(stroke))
    }
  }
}

/**
 * The one Veil empty state: optional [illustration] (for example [VeilMark] or a MaskAvatar), a short [title], one
 * line of [text] and an optional action. Fills the space it is given and centres its content.
 */
@Composable
fun VeilEmptyState(
  text: String,
  modifier: Modifier = Modifier,
  title: String? = null,
  illustration: (@Composable () -> Unit)? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  Box(modifier.fillMaxSize().padding(horizontal = 30.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (illustration != null) {
        illustration()
        Spacer(Modifier.height(8.dp))
      }
      if (title != null) {
        BasicText(title, style = type.sheetTitle.copy(color = colors.ink, textAlign = TextAlign.Center))
      }
      BasicText(text, style = type.body.copy(color = colors.ink3, textAlign = TextAlign.Center))
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(8.dp))
        VeilButton(actionLabel, onAction, style = VeilButtonStyle.Secondary, fillMaxWidth = false)
      }
    }
  }
}

/** Amber note for text that warns (the warning card of the prototype's group-link delete sheet). */
@Composable
fun VeilWarningNote(text: String, modifier: Modifier = Modifier) {
  val colors = VeilTheme.colors
  val shape = RoundedCornerShape(16.dp)
  Box(
    modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.warn.copy(alpha = 0.08f))
      .border(1.dp, colors.warn.copy(alpha = 0.35f), shape)
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    BasicText(text, style = VeilTheme.type.caption.copy(color = colors.warn, fontSize = 13.sp, lineHeight = 18.sp))
  }
}
