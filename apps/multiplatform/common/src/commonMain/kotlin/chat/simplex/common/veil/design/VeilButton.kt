package chat.simplex.common.veil.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicText

enum class VeilButtonStyle {
  /** Gradient fill, dark label. One per screen, the main action. */
  Primary,
  /** Translucent fill with a visible hairline. */
  Secondary,
  /** Red tint and red hairline; destructive actions. */
  Danger,
}

/**
 * Veil button: at least 52dp tall (grows with the font scale), 18dp radius. Disabled = 38% opacity, no glow, not clickable. Pressing scales to 98%.
 * [leading] is an optional icon slot drawn before the label (tint it with the colour you get from the style).
 */
@Composable
fun VeilButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: VeilButtonStyle = VeilButtonStyle.Primary,
  enabled: Boolean = true,
  fillMaxWidth: Boolean = true,
  leading: (@Composable () -> Unit)? = null,
) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.button
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val reduce = VeilTheme.reduceMotion
  val scale = if (pressed && !reduce) 0.98f else 1f
  val labelColor = when (style) {
    VeilButtonStyle.Primary -> colors.onGradient
    VeilButtonStyle.Secondary -> colors.ink
    VeilButtonStyle.Danger -> colors.errText
  }
  val base = modifier
    .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
    .heightIn(min = 52.dp)
    .graphicsLayer { scaleX = scale; scaleY = scale }
    .alpha(if (enabled) 1f else 0.38f)
  val styled = when (style) {
    VeilButtonStyle.Primary -> base
      .then(if (enabled) Modifier.shadow(10.dp, shape, ambientColor = colors.accent, spotColor = colors.accent) else Modifier)
      .clip(shape)
      .drawBehind { drawRect(colors.gradientFor(size, 120f)) }
    VeilButtonStyle.Secondary -> base
      .clip(shape)
      .background(colors.sheet2)
      .border(BorderStroke(1.dp, colors.line2), shape)
    VeilButtonStyle.Danger -> base
      .clip(shape)
      .background(colors.err.copy(alpha = 0.10f))
      .border(BorderStroke(1.dp, colors.err.copy(alpha = 0.40f)), shape)
  }
  Row(
    styled
      .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
      .padding(horizontal = 22.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    leading?.invoke()
    BasicText(text, style = VeilTheme.type.button.copy(color = labelColor, textAlign = TextAlign.Center))
  }
}
