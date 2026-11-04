package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * Full-screen frame of the Veil chat info and security-code screens: night background, top bar with a back arrow,
 * scrolling body with the 18dp gutter. Shown inside a custom modal, so back is handled here.
 */
@Composable
fun VeilScreenScaffold(
  title: String,
  onBack: () -> Unit,
  subtitle: String? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = VeilTheme.colors
  BackHandler(onBack = onBack)
  Column(Modifier.fillMaxSize().background(colors.night).statusBarsPadding()) {
    VeilTopBar(
      title = title,
      navigation = {
        VeilIconButton(onClick = onBack, contentDescription = stringResource(MR.strings.veil_chat_back)) {
          VeilIcon(MR.images.ic_arrow_back_ios_new, colors.ink2, size = 20.dp)
        }
      },
      subtitle = subtitle?.let { s ->
        {
          BasicText(s, style = VeilTheme.type.caption.copy(color = colors.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      },
    )
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = VeilGutter)
        .navigationBarsPadding()
        .padding(bottom = 24.dp),
      content = content,
    )
  }
}

/** One row of a Veil list card: icon, title (+ optional [value] line), trailing slot. Clickable when [onClick] is set. */
@Composable
fun VeilInfoRow(
  icon: ImageResource,
  title: String,
  modifier: Modifier = Modifier,
  value: String? = null,
  onClick: (() -> Unit)? = null,
  trailing: (@Composable () -> Unit)? = null,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  Row(
    modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clip(VeilTheme.shapes.input)
      .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    VeilIcon(icon, colors.ink2, size = 22.dp)
    Column(Modifier.weight(1f)) {
      BasicText(title, style = type.bodyLarge.copy(color = colors.ink))
      if (value != null) BasicText(value, style = type.caption.copy(color = colors.ink2, fontSize = 12.5.sp))
    }
    if (trailing != null) trailing()
    else if (onClick != null) VeilIcon(MR.images.ic_chevron_right, colors.ink3, size = 20.dp)
  }
}
