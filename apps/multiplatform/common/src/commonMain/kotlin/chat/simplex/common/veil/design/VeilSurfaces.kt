package chat.simplex.common.veil.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Elevation of a translucent layer: the overlay (white in Gece, dark ink in Sis) gets stronger the higher the level. */
enum class VeilLayer { Low, Mid, High }

@Composable
internal fun VeilColors.layerColor(layer: VeilLayer): Color = when (layer) {
  VeilLayer.Low -> sheet1
  VeilLayer.Mid -> sheet2
  VeilLayer.High -> sheet3
}

/**
 * Basic translucent surface: a white layer ([layer]) over whatever is behind it, clipped to [shape] with an
 * optional hairline border. Prefer [VeilCard] / [VeilSheet] for the common cases.
 */
@Composable
fun VeilSurface(
  modifier: Modifier = Modifier,
  shape: Shape = VeilTheme.shapes.card,
  layer: VeilLayer = VeilLayer.Low,
  border: Boolean = true,
  content: @Composable BoxScope.() -> Unit,
) {
  val colors = VeilTheme.colors
  Box(
    modifier
      .clip(shape)
      .background(colors.layerColor(layer))
      .then(if (border) Modifier.border(1.dp, colors.line, shape) else Modifier),
    content = content
  )
}

/**
 * Card: [VeilSurface] with the 20dp card radius and 16dp padding. Clickable when [onClick] is set.
 */
@Composable
fun VeilCard(
  modifier: Modifier = Modifier,
  layer: VeilLayer = VeilLayer.Low,
  onClick: (() -> Unit)? = null,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  VeilSurface(
    modifier = modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    shape = VeilTheme.shapes.card,
    layer = layer,
  ) {
    Column(Modifier.padding(contentPadding), content = content)
  }
}

/**
 * Bottom sheet body: 28dp top radius, a faint top-lit wash over an opaque base, hairline top edge and a
 * grabber. It does not position or animate itself; place it at the bottom of a scrim / modal host.
 * (Sheets are opaque underneath so content behind them never shows through text.)
 */
@Composable
fun VeilSheet(
  modifier: Modifier = Modifier,
  showGrabber: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.sheet
  Column(
    modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.sheetBase)
      .background(Brush.verticalGradient(listOf(colors.wash.copy(alpha = if (colors.isLight) 0.05f else 0.10f), colors.wash.copy(alpha = if (colors.isLight) 0.03f else 0.06f))))
      .border(1.dp, colors.line2, shape)
      .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 26.dp),
  ) {
    if (showGrabber) {
      Box(
        Modifier
          .align(Alignment.CenterHorizontally)
          .padding(bottom = 14.dp)
          .size(width = 38.dp, height = 4.dp)
          .clip(VeilTheme.shapes.pill)
          .background(colors.line2)
      )
    }
    content()
  }
}

/** Dp helper used by sheet/list code for the standard 18dp screen gutter. */
val VeilGutter: Dp = 18.dp
