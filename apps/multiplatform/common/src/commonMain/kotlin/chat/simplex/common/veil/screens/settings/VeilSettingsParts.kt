package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.stringResource

/*
 * Building blocks shared by the Veil settings screens: scaffold with a back button, grouped cards, rows.
 */

/** Full-screen settings page: night background with grain, top bar with back, scrolling content. */
@Composable
fun VeilSettingsScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
  val colors = VeilTheme.colors
  BackHandler(onBack = onBack)
  Box(Modifier.fillMaxSize().background(colors.night).veilGrain()) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
      VeilTopBar(
        title = title,
        navigation = {
          VeilIconButton(onClick = onBack, contentDescription = stringResource(MR.strings.veil_set_back)) {
            VeilIcon(MR.images.ic_arrow_back_ios_new, colors.ink2, size = 20.dp)
          }
        }
      )
      Column(
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = VeilGutter)
          .padding(top = 6.dp, bottom = 30.dp)
          .navigationBarsPadding(),
        content = content
      )
    }
  }
}

/** Small uppercase label above a group. */
@Composable
fun VeilSectionLabel(text: String) {
  BasicText(
    text.uppercase(),
    Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
    style = VeilTheme.type.chip.copy(color = VeilTheme.colors.ink3, letterSpacing = 0.1.em),
  )
}

/** Rounded card holding rows. Rows draw their own dividers via [VeilRowDivider]. */
@Composable
fun VeilGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  VeilCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), content = content)
}

@Composable
fun VeilRowDivider() {
  Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(1.dp).background(VeilTheme.colors.line))
}

/** Explanatory paragraph under a group. */
@Composable
fun VeilNote(text: String, modifier: Modifier = Modifier) {
  BasicText(text, modifier.padding(horizontal = 6.dp, vertical = 10.dp), style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink2))
}

/** "Salt okunur" tag of a locked setting. */
@Composable
fun VeilReadOnlyTag() {
  val colors = VeilTheme.colors
  BasicText(
    stringResource(MR.strings.veil_set_readonly),
    Modifier.clip(RoundedCornerShape(50)).background(colors.sheet2).padding(horizontal = 10.dp, vertical = 4.dp),
    style = VeilTheme.type.chip.copy(color = colors.ink3),
  )
}

/** Navigation row: icon tile, title, optional one-line summary, chevron. */
@Composable
fun VeilNavRow(icon: ImageResource, title: String, summary: String?, onClick: () -> Unit, enabled: Boolean = true) {
  val colors = VeilTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.sheet2), contentAlignment = Alignment.Center) {
      VeilIcon(icon, colors.ink2, size = 20.dp)
    }
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.rowTitle.copy(color = if (enabled) colors.ink else colors.ink3), maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (!summary.isNullOrEmpty()) {
        BasicText(summary, style = VeilTheme.type.caption.copy(color = colors.ink3), maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
    }
    VeilIcon(MR.images.ic_chevron_right, colors.ink3, size = 20.dp)
  }
}

/** Row with a title, an optional description and a switch (or a read-only tag when [locked]). */
@Composable
fun VeilSwitchRow(title: String, description: String? = null, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
  val colors = VeilTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
      if (description != null) BasicText(description, style = VeilTheme.type.caption.copy(color = colors.ink3))
    }
    VeilSwitch(checked, onCheckedChange = null, enabled = enabled)
  }
}

/** Row showing a fixed value: title with an optional small line under it, and a read-only tag or a value. */
@Composable
fun VeilValueRow(title: String, sub: String?, readOnly: Boolean, value: String? = null, onClick: (() -> Unit)? = null) {
  val colors = VeilTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
      if (sub != null) BasicText(sub, style = VeilTheme.type.caption.copy(color = colors.ink3))
    }
    if (value != null) BasicText(value, style = VeilTheme.type.body.copy(color = colors.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (readOnly) VeilReadOnlyTag()
    if (onClick != null) VeilIcon(MR.images.ic_chevron_right, colors.ink3, size = 20.dp)
  }
}
