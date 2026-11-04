package chat.simplex.common.veil.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Veil shape tokens. Bubble shapes with the folded corner live in [FoldBubbleShape]. */
@Immutable
class VeilShapes(
  /** Top corner radius of bottom sheets. */
  val sheetRadius: Dp = 28.dp,
  val cardRadius: Dp = 20.dp,
  val bubbleRadius: Dp = 18.dp,
  /** Size of the cut (folded) corner of a bubble. */
  val bubbleFold: Dp = 15.dp,
  val buttonRadius: Dp = 18.dp,
  val inputRadius: Dp = 18.dp,
) {
  val sheet = RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius)
  val card = RoundedCornerShape(cardRadius)
  val button = RoundedCornerShape(buttonRadius)
  val input = RoundedCornerShape(inputRadius)
  val pill = RoundedCornerShape(percent = 50)
  fun bubble(outgoing: Boolean) = FoldBubbleShape(outgoing, bubbleRadius, bubbleFold)

  companion object {
    val Default = VeilShapes()
  }
}

/** Material shapes for upstream widgets (dialogs, menus, cards) so they match the Veil radii. */
val VeilMaterialShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(0.dp)
)
