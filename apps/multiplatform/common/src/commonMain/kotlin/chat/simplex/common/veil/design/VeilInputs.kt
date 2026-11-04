package chat.simplex.common.veil.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Veil switch: 46x28 pill, white thumb. Off = white 12 % track, on = iris. The thumb slides in the lift duration
 * (instant with reduce-motion).
 */
@Composable
fun VeilSwitch(
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val colors = VeilTheme.colors
  val motion = VeilTheme.motion
  val ms = motion.lift(VeilTheme.reduceMotion)
  val track by animateColorAsState(if (checked) colors.iris else colors.sheet3, tween(ms, easing = motion.easeOut))
  val thumbX by animateDpAsState(if (checked) 18.dp else 0.dp, tween(ms, easing = motion.easeOut))
  Box(
    modifier
      .size(width = 46.dp, height = 28.dp)
      .alpha(if (enabled) 1f else 0.38f)
      .clip(CircleShape)
      .background(track)
      .then(
        if (onCheckedChange != null) Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        else Modifier
      )
      .padding(3.dp)
  ) {
    Box(Modifier.offset(x = thumbX).size(22.dp).clip(CircleShape).background(Color.White))
  }
}

/**
 * Veil text field: 54dp tall, 18dp radius, translucent fill and hairline. When focused the border becomes the
 * mint-iris-rose gradient on a night2 fill. [placeholder] shows in ink3 while empty.
 */
@Composable
fun VeilTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  enabled: Boolean = true,
  singleLine: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  trailing: (@Composable () -> Unit)? = null,
) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.input
  val interaction = remember { MutableInteractionSource() }
  val focused by interaction.collectIsFocusedAsState()
  val frame = if (focused) {
    Modifier.clip(shape).background(colors.night2).border(1.dp, colors.gradient, shape)
  } else {
    Modifier.clip(shape).background(colors.sheet1).border(1.dp, colors.line2, shape)
  }
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth().heightIn(min = 54.dp).then(frame),
    enabled = enabled,
    singleLine = singleLine,
    textStyle = VeilTheme.type.bodyLarge.copy(color = colors.ink),
    cursorBrush = SolidColor(colors.iris),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    visualTransformation = visualTransformation,
    interactionSource = interaction,
    decorationBox = { inner ->
      Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
          if (value.isEmpty() && placeholder != null) {
            BasicText(placeholder, style = VeilTheme.type.bodyLarge.copy(color = colors.ink3))
          }
          inner()
        }
        trailing?.invoke()
      }
    }
  )
}
