package chat.simplex.common.veil.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class VeilChipStyle {
  /** Translucent fill, hairline. */
  Default,
  /** Gradient outline on night3 fill; for the active / highlighted state. */
  Gradient,
  /** Dashed outline, no fill; for read-only or optional states. */
  Dashed,
  /** Amber tint; for warnings. */
  Warning,
}

/** Small pill label (22dp). Clickable when [onClick] is set. [leading] is an optional icon slot. */
@Composable
fun VeilChip(
  text: String,
  modifier: Modifier = Modifier,
  style: VeilChipStyle = VeilChipStyle.Default,
  onClick: (() -> Unit)? = null,
  leading: (@Composable () -> Unit)? = null,
) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.pill
  val textColor = when (style) {
    VeilChipStyle.Default, VeilChipStyle.Dashed -> colors.ink2
    VeilChipStyle.Gradient -> colors.ink
    VeilChipStyle.Warning -> colors.warn
  }
  val styled = when (style) {
    VeilChipStyle.Default -> Modifier.clip(shape).background(colors.sheet2).border(1.dp, colors.line2, shape)
    VeilChipStyle.Gradient -> Modifier.clip(shape).background(colors.night3).border(1.dp, colors.gradient, shape)
    VeilChipStyle.Dashed -> Modifier.clip(shape).drawBehind {
      val w = 1.dp.toPx()
      drawRoundRect(
        colors.line2, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(size.height / 2), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
      )
    }
    VeilChipStyle.Warning -> Modifier.clip(shape).background(colors.warn.copy(alpha = 0.08f)).border(1.dp, colors.warn.copy(alpha = 0.45f), shape)
  }
  Row(
    modifier
      .height(22.dp)
      .then(styled)
      .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
      .padding(horizontal = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    leading?.invoke()
    BasicText(text, style = VeilTheme.type.chip.copy(color = textColor), maxLines = 1)
  }
}
