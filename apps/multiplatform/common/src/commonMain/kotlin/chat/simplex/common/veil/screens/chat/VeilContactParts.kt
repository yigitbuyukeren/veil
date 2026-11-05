package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import boofcv.alg.fiducial.qrcode.QrCode
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.newchat.qrCodeBitmap
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

/*
 * Pieces shared by the Veil contact detail screens (advanced, preferences, pending connection).
 */

/**
 * A titled row of choices shown as chips (one selected), for settings that upstream shows as a drop-down. [custom] adds a
 * last chip (e.g. "custom") that opens upstream's own picker.
 */
@Composable
internal fun <T> VeilChoiceRow(
  title: String,
  options: List<Pair<T, String>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  custom: Pair<String, () -> Unit>? = null,
) {
  val colors = VeilTheme.colors
  Column(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f).padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    BasicText(title, style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      options.forEach { (value, label) ->
        val isSelected = value == selected
        VeilChip(
          label,
          Modifier.semantics { this.selected = isSelected },
          style = if (isSelected) VeilChipStyle.Gradient else VeilChipStyle.Default,
          onClick = if (enabled) ({ onSelect(value) }) else null,
        )
      }
      if (custom != null) {
        VeilChip(custom.first, style = VeilChipStyle.Dashed, onClick = if (enabled) custom.second else null)
      }
    }
  }
}

/** Plain explanation under a card or row. */
@Composable
internal fun VeilCaption(text: String, modifier: Modifier = Modifier) {
  BasicText(text, modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink3))
}

/** A QR code on white with a quiet zone, like the invitation page: dark modules, no logo and no tint, so any scanner reads it. */
@Composable
internal fun VeilQr(content: String, small: Boolean, description: String, modifier: Modifier = Modifier) {
  val colors = VeilTheme.colors
  val bitmap = remember(content) { qrCodeBitmap(content, 1024, if (small) QrCode.ErrorLevel.M else QrCode.ErrorLevel.L) }
  val frame = VeilTheme.shapes.card
  Box(modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
    Box(
      Modifier
        .size(220.dp)
        .clip(frame)
        .background(Color.White)
        .border(1.dp, colors.line2, frame)
        .padding(16.dp)
    ) {
      Image(bitmap, contentDescription = description, Modifier.fillMaxSize())
    }
  }
}

/**
 * A local name field saved a moment after typing stops and once more when the screen closes, like upstream's
 * `LocalAliasEditor`. [save] is the same call upstream makes. [focus] puts the cursor in the field when the screen opens.
 */
@Composable
internal fun VeilAliasField(key: Any, initial: String, placeholder: String, focus: Boolean, save: (String) -> Unit) {
  var text by remember(key) { mutableStateOf(initial) }
  var saved by remember(key) { mutableStateOf(initial) }
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(key, text) {
    if (text == saved) return@LaunchedEffect
    delay(500)
    saved = text
    save(text)
  }
  DisposableEffect(key) {
    onDispose { if (text != saved) save(text) }
  }
  if (focus) LaunchedEffect(key) { runCatching { focusRequester.requestFocus() } }
  VeilTextField(
    value = text,
    onValueChange = { text = it },
    modifier = Modifier.padding(bottom = 14.dp).focusRequester(focusRequester),
    placeholder = placeholder,
  )
}
