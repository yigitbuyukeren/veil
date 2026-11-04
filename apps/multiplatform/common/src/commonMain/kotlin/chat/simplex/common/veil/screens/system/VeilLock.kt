package chat.simplex.common.veil.screens.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.chat.group.ProgressIndicator
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Veil passcode screen: title, reason, dots, a round keypad, cancel and submit. Replaces upstream's `PasscodeView`
 * UI (Android) and is used by app unlock, passcode setup and every other passcode prompt; the logic (what submit
 * and cancel do, which codes are allowed) stays in the callers.
 */
@Composable
fun VeilPasscodeView(
  passcode: MutableState<String>,
  title: String,
  reason: String?,
  submitLabel: String,
  submitEnabled: ((String) -> Boolean)?,
  buttonsEnabled: State<Boolean>,
  submit: () -> Unit,
  cancel: () -> Unit,
) {
  val colors = VeilTheme.colors
  val focusRequester = remember { FocusRequester() }
  val canSubmit = veilPasscodeSubmitEnabled(passcode.value, submitEnabled?.invoke(passcode.value), buttonsEnabled.value)

  val numbers = remember {
    arrayOf(
      Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine,
      Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4, Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8, Key.NumPad9
    )
  }
  // Hardware keyboard: same keys as upstream.
  val keys = Modifier.onPreviewKeyEvent {
    if (it.key in numbers && it.type == KeyEventType.KeyDown) {
      passcode.value = veilPasscodeAppend(passcode.value, numbers.indexOf(it.key) % 10)
      true
    } else if (it.key == Key.Backspace && it.type == KeyEventType.KeyDown && (it.isCtrlPressed || it.isMetaPressed)) {
      passcode.value = ""
      true
    } else if (it.key == Key.Backspace && it.type == KeyEventType.KeyDown) {
      passcode.value = passcode.value.dropLast(1)
      true
    } else if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
      if (canSubmit) submit()
      true
    } else false
  }

  Box(Modifier.fillMaxSize().background(colors.night).then(keys).focusRequester(focusRequester)) {
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
      val landscape = maxWidth > maxHeight
      if (!landscape) {
        val key = minOf(maxWidth / 4, (maxHeight - 260.dp) / 4, 84.dp).coerceAtLeast(48.dp)
        Column(
          Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceEvenly,
        ) {
          Header(title, reason, passcode)
          Keypad(passcode, key)
          Buttons(submitLabel, canSubmit, buttonsEnabled.value, submit, cancel, Modifier.fillMaxWidth())
        }
      } else {
        val key = ((maxHeight - 32.dp) / 4).coerceIn(44.dp, 72.dp)
        Row(
          Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Header(title, reason, passcode)
            Buttons(submitLabel, canSubmit, buttonsEnabled.value, submit, cancel, Modifier.fillMaxWidth())
          }
          Keypad(passcode, key)
        }
      }
    }
    if (!buttonsEnabled.value) ProgressIndicator()
  }
  val view = LocalMultiplatformView()
  LaunchedEffect(Unit) {
    hideKeyboard(view, true)
    focusRequester.requestFocus()
    // Same as upstream: keep the focus so that Tab / clicks do not steal key events.
    focusRequester.captureFocus()
  }
}

@Composable
private fun Header(title: String, reason: String?, passcode: MutableState<String>) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  var show by rememberSaveable { mutableStateOf(false) }
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    BasicText(title, style = type.title.copy(color = colors.ink, textAlign = TextAlign.Center))
    if (reason != null) {
      BasicText(reason, style = type.body.copy(color = colors.ink2, textAlign = TextAlign.Center))
    }
    Spacer(Modifier.height(8.dp))
    // Tap on the dots shows or hides the digits (as upstream).
    val dotsDescription = String.format(stringResource(MR.strings.veil_a11y_pass_entered), passcode.value.length)
    val toggleLabel = stringResource(if (show) MR.strings.veil_a11y_pass_hide else MR.strings.veil_a11y_pass_show)
    Box(
      Modifier
        .heightIn(min = 48.dp)
        .clickable(onClickLabel = toggleLabel, role = Role.Button) { show = !show }
        .then(if (show && passcode.value.isNotEmpty()) Modifier else Modifier.semantics { contentDescription = dotsDescription }),
      contentAlignment = Alignment.Center
    ) {
      if (show && passcode.value.isNotEmpty()) {
        BasicText(passcode.value.chunked(4).joinToString(" "), style = type.title.copy(color = colors.ink))
      } else {
        val slots = veilPasscodeSlots(passcode.value.length)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          for (i in 0 until slots) {
            val filled = i < passcode.value.length
            Box(
              Modifier.size(14.dp).clip(CircleShape)
                .then(if (filled) Modifier.background(colors.gradient) else Modifier.border(1.5.dp, colors.line2, CircleShape))
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Keypad(passcode: MutableState<String>, key: Dp) {
  val gap = 12.dp
  Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
    for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
      Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (d in row) DigitKey(d, key) { passcode.value = veilPasscodeAppend(passcode.value, d) }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
      IconKey(MR.images.ic_close, stringResource(MR.strings.veil_sys_pass_clear), key) { passcode.value = "" }
      DigitKey(0, key) { passcode.value = veilPasscodeAppend(passcode.value, 0) }
      IconKey(MR.images.ic_backspace, stringResource(MR.strings.veil_sys_pass_delete), key) { passcode.value = passcode.value.dropLast(1) }
    }
  }
}

@Composable
private fun DigitKey(d: Int, size: Dp, onClick: () -> Unit) {
  val colors = VeilTheme.colors
  Box(
    Modifier.size(size).clip(CircleShape).background(colors.sheet1).border(1.dp, colors.line, CircleShape)
      .clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    BasicText(d.toString(), style = VeilTheme.type.title.copy(color = colors.ink))
  }
}

@Composable
private fun IconKey(res: dev.icerock.moko.resources.ImageResource, description: String, size: Dp, onClick: () -> Unit) {
  Box(
    Modifier.size(size).clip(CircleShape).semantics { contentDescription = description }
      .clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    VeilIcon(res, VeilTheme.colors.ink2, size = 24.dp)
  }
}

@Composable
private fun Buttons(submitLabel: String, canSubmit: Boolean, buttonsEnabled: Boolean, submit: () -> Unit, cancel: () -> Unit, modifier: Modifier) {
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    VeilButton(
      stringResource(MR.strings.cancel_verb), cancel,
      modifier = Modifier.weight(1f), style = VeilButtonStyle.Secondary, enabled = buttonsEnabled
    )
    VeilButton(submitLabel, submit, modifier = Modifier.weight(1f), style = VeilButtonStyle.Primary, enabled = canSubmit)
  }
}

/**
 * Lock screen shown when the app is locked and the passcode or system prompt was dismissed or failed. The one
 * statement it makes is that the app lock is on; the unlock button runs the same authentication as upstream.
 */
@Composable
fun VeilLockScreen(onUnlock: () -> Unit) {
  val colors = VeilTheme.colors
  Box(Modifier.fillMaxSize().background(colors.night), contentAlignment = Alignment.Center) {
    Column(
      Modifier.padding(horizontal = 30.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      VeilMark(88.dp)
      Spacer(Modifier.height(6.dp))
      BasicText(
        stringResource(MR.strings.veil_sys_lock_title),
        style = VeilTheme.type.title.copy(color = colors.ink, textAlign = TextAlign.Center)
      )
      Spacer(Modifier.height(6.dp))
      VeilButton(
        stringResource(MR.strings.auth_unlock), onUnlock,
        fillMaxWidth = false, modifier = Modifier.widthIn(min = 200.dp),
        leading = { VeilIcon(MR.images.ic_lock, colors.onGradient, size = 20.dp) }
      )
    }
  }
}

/** Splash: night background and the Veil mark. */
@Composable
fun VeilSplash() {
  Box(Modifier.fillMaxSize().background(VeilTheme.colors.night), contentAlignment = Alignment.Center) {
    VeilMark(88.dp)
  }
}
