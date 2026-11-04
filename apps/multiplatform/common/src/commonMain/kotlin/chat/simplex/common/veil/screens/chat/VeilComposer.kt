package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.veil.data.rememberTorRingState
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/*
 * Composer pieces of the Veil chat. The text field, voice, live message, attachments and the send function are
 * upstream's (`ComposeView` / `SendMsgView`, `ComposeState`); this file only frames them and draws the reply/edit bar
 * and the locked state.
 */

/** True while no Tor proxy answers (the same probe and rule as the chat list). Not "Connecting": that only waits. */
@Composable
fun veilTorDown(): Boolean = composerLocked(rememberTorRingState() == TorRingState.Unavailable)

/** Row with upstream's attachment buttons ([attach]) and a Veil frame around upstream's text field and send button ([field]). */
@Composable
fun VeilComposerRow(attach: @Composable () -> Unit, field: @Composable () -> Unit) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.input
  Row(
    Modifier.fillMaxWidth().padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    attach()
    Box(
      Modifier
        .weight(1f)
        .heightIn(min = 46.dp)
        .clip(shape)
        .background(colors.sheet1)
        .border(1.dp, colors.line2, shape)
        .padding(start = 12.dp, end = 2.dp)
    ) { field() }
  }
}

/** Replaces the composer while there is no Tor: it cannot be typed into and nothing is sent. The draft is kept. */
@Composable
fun VeilComposerLocked() {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.input
  Box(
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .heightIn(min = 46.dp)
      .clip(shape)
      .background(colors.warn.copy(alpha = 0.07f))
      .border(1.dp, colors.warn.copy(alpha = 0.55f), shape)
      .padding(horizontal = 15.dp, vertical = 12.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    BasicText(
      stringResource(MR.strings.veil_chat_tor_down),
      style = VeilTheme.type.body.copy(color = colors.warn),
      maxLines = 2,
    )
  }
}

/** Reply / edit bar above the field (the prototype's `cbar`): iris edge, what is being replied to or edited, close. */
@Composable
fun VeilContextBar(chatInfo: ChatInfo, item: ChatItem, editing: Boolean, onClose: () -> Unit) {
  val colors = VeilTheme.colors
  val title = if (editing) stringResource(MR.strings.veil_chat_editing) else {
    val who = when (val d = item.chatDir) {
      is CIDirection.GroupRcv -> d.groupMember.displayName
      is CIDirection.DirectRcv -> chatInfo.displayName
      else -> stringResource(MR.strings.veil_chat_you)
    }
    stringResource(MR.strings.veil_chat_replying_to, who)
  }
  Row(
    Modifier
      .fillMaxWidth()
      .padding(start = 12.dp, end = 12.dp, top = 8.dp)
      .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
      .background(colors.sheet2)
      .drawBehind { drawRect(colors.iris, size = Size(2.dp.toPx(), size.height)) }
      .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.label.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
      BasicText(item.text, style = VeilTheme.type.caption.copy(color = colors.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    VeilIconButton(onClick = onClose, contentDescription = stringResource(MR.strings.veil_chat_close_bar)) {
      VeilIcon(MR.images.ic_close, colors.ink2, size = 18.dp)
    }
  }
}

/** The Veil bar draws plain text items; others (images, files, voice, reports) keep upstream's context view. */
fun veilContextBarApplies(item: ChatItem): Boolean = item.content.msgContent is MsgContent.MCText
