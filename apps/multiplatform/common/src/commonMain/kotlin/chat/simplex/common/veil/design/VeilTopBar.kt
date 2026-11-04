package chat.simplex.common.veil.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Screen top bar: display-face [title] (22 bold), optional [subtitle] slot (12sp caption area, e.g. a [TorRing] plus
 * status text), and [actions] on the right. [navigation] is an optional leading slot (back arrow).
 * Does not draw a background; the screen behind shows through.
 */
@Composable
fun VeilTopBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: (@Composable RowScope.() -> Unit)? = null,
  navigation: (@Composable () -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  val colors = VeilTheme.colors
  Row(
    modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    navigation?.invoke()
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.title.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (subtitle != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { subtitle() }
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
  }
}

/** 40dp round-rect icon button for top bars and composers. [content] is the icon (tint it with `ink2`). */
@Composable
fun VeilIconButton(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  Box(
    modifier
      .size(40.dp)
      .clip(RoundedCornerShapeIcon)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
      .semantics { this.contentDescription = contentDescription },
    contentAlignment = Alignment.Center,
  ) { content() }
}

private val RoundedCornerShapeIcon = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
