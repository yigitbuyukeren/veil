package chat.simplex.common.veil.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.veil.design.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared frame of the onboarding screens: night background with grain, system bar and keyboard insets, an optional back
 * button with [title], a scrolling [content] area with the 18dp gutter, and a pinned [bottom] area for the main action.
 */
@Composable
internal fun OnbScaffold(
  onBack: (() -> Unit)?,
  title: String? = null,
  bottom: @Composable ColumnScope.() -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = VeilTheme.colors
  Column(
    Modifier
      .fillMaxSize()
      .background(colors.night)
      .veilGrain(0.04f)
      .systemBarsPadding()
      .imePadding()
  ) {
    if (title != null) {
      VeilTopBar(title = title, navigation = onBack?.let { { OnbBackButton(it) } })
    } else if (onBack != null) {
      Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 8.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        OnbBackButton(onBack)
      }
    } else {
      Spacer(Modifier.height(14.dp))
    }
    Column(
      Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = VeilGutter),
      content = content
    )
    Column(Modifier.fillMaxWidth().padding(start = VeilGutter, end = VeilGutter, top = 12.dp, bottom = 26.dp), content = bottom)
  }
}

@Composable
internal fun OnbBackButton(onClick: () -> Unit) {
  VeilIconButton(onClick, stringResource(MR.strings.veil_onb_back)) {
    Icon(painterResource(MR.images.ic_arrow_back_ios_new), null, Modifier.size(20.dp), tint = VeilTheme.colors.ink2)
  }
}

@Composable
internal fun OnbHeadline(text: String, modifier: Modifier = Modifier) {
  BasicText(
    text,
    modifier,
    style = VeilTheme.type.display.copy(fontSize = 34.sp, lineHeight = 36.sp, color = VeilTheme.colors.ink)
  )
}

@Composable
internal fun OnbBody(text: String, modifier: Modifier = Modifier) {
  BasicText(text, modifier, style = VeilTheme.type.bodyLarge.copy(color = VeilTheme.colors.ink2))
}

@Composable
internal fun OnbNote(text: String, modifier: Modifier = Modifier) {
  BasicText(text, modifier, style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink3, lineHeight = 17.sp))
}
