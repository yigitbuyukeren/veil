package chat.simplex.common.veil.screens.system

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.veil.design.VeilButton
import chat.simplex.common.veil.design.VeilButtonStyle
import chat.simplex.common.veil.design.VeilTheme
import kotlinx.coroutines.delay

/**
 * Veil look of `AlertManager`'s dialogs. `AlertManager` calls these through its `// Veil:` hooks, so every alert in
 * the app (upstream and Veil) changes at once. Behaviour (dismissal, button order, focus) stays in `AlertManager`.
 */

/** Dialog shell: night surface, 28dp radius, hairline, Material scrim. Android and desktop. */
@Composable
internal fun VeilAlertShell(
  onDismissRequest: () -> Unit,
  title: (@Composable () -> Unit)?,
  buttons: @Composable () -> Unit,
) {
  val colors = VeilTheme.colors
  val shape = androidx.compose.foundation.shape.RoundedCornerShape(VeilTheme.shapes.sheetRadius)
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = title,
    buttons = buttons,
    shape = shape,
    backgroundColor = colors.night2,
    contentColor = colors.ink,
    modifier = Modifier.border(1.dp, colors.line2, shape),
  )
}

@Composable
internal fun VeilAlertTitle(title: String) {
  BasicText(
    title,
    Modifier.fillMaxWidth(),
    style = VeilTheme.type.sheetTitle.copy(color = VeilTheme.colors.ink, textAlign = TextAlign.Center),
  )
}

/** One button of an alert. [onClick] must also hide the alert (as upstream's handlers do). */
class VeilAlertButton(
  val label: String,
  val role: VeilAlertRole,
  /** Gets keyboard focus 200 ms after showing (upstream does this so a held Enter key does not auto-confirm). */
  val focus: Boolean = false,
  val onClick: () -> Unit,
)

/**
 * Alert buttons in the Veil look. [buttons] are given in column order, main action first. Two short labels are shown
 * in a row, with the main action on the right (as upstream's text buttons); otherwise they are stacked.
 */
@Composable
internal fun VeilAlertButtons(buttons: List<VeilAlertButton>) {
  val layout = veilAlertLayout(buttons.map { it.label })
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    delay(200)
    if (buttons.any { it.focus }) runCatching { focusRequester.requestFocus() }
  }
  @Composable
  fun Btn(b: VeilAlertButton, modifier: Modifier) {
    VeilButton(
      b.label,
      b.onClick,
      modifier = if (b.focus) modifier.focusRequester(focusRequester) else modifier,
      style = when (b.role) {
        VeilAlertRole.Primary -> VeilButtonStyle.Primary
        VeilAlertRole.Secondary -> VeilButtonStyle.Secondary
        VeilAlertRole.Danger -> VeilButtonStyle.Danger
      },
    )
  }
  if (layout == VeilAlertLayout.Row) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      for (b in buttons.reversed()) Btn(b, Modifier.weight(1f))
    }
  } else {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      for (b in buttons) Btn(b, Modifier)
    }
  }
}
