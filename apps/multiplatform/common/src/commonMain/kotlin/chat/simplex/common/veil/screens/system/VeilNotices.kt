package chat.simplex.common.veil.screens.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.veil.design.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Card shown by the app shell (under the top bar of the chat list) when the chat is stopped. Honest text: while
 * stopped nothing is received or sent. Starting is done the same way as upstream (Settings, Database, or restart).
 */
@Composable
fun VeilStoppedCard(modifier: Modifier = Modifier) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  VeilCard(
    modifier.padding(start = VeilGutter, end = VeilGutter, bottom = 8.dp).fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    BasicText(stringResource(MR.strings.veil_sys_stopped_title), style = type.rowTitle.copy(color = colors.ink))
    Spacer(Modifier.height(4.dp))
    BasicText(stringResource(MR.strings.veil_sys_stopped_text), style = type.caption.copy(color = colors.ink2, fontSize = 13.sp, lineHeight = 18.sp))
  }
}
