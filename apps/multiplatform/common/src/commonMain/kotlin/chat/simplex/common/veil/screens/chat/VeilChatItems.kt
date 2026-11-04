package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import chat.simplex.common.model.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.chat.item.MarkdownText
import chat.simplex.common.views.chat.item.isShortEmoji
import chat.simplex.common.views.chat.item.sendCommandMsg
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

/*
 * Item rendering of the Veil chat. Only plain text messages (with replies, reactions, the edited marker) and the
 * deleted / moderated placeholders are drawn here. Everything else (images, files, voice, video, link previews, forwarded
 * items, disappearing messages, calls, group and connection events, invitations...) stays with upstream's item composables,
 * which `ChatItemView` still calls; see `veilBubbleEligible`.
 */

/**
 * True when the Veil bubble draws [ci]: a text message, not forwarded, not live, not timed, no attachment, not a report,
 * not deleted, and not the big-emoji layout (upstream draws that one without a bubble).
 */
fun veilBubbleEligible(ci: ChatItem): Boolean {
  val c = ci.content
  if (c !is CIContent.SndMsgContent && c !is CIContent.RcvMsgContent) return false
  val mc = c.msgContent
  return mc is MsgContent.MCText &&
    ci.file == null &&
    ci.meta.itemForwarded == null &&
    !ci.meta.isLive &&
    ci.meta.itemTimed == null &&
    ci.meta.itemDeleted == null &&
    ci.meta.msgVerified == null &&
    ci.id >= 0 &&
    !(ci.quotedItem == null && isShortEmoji(ci.content.text))
}

/** The deleted / moderated placeholder replaces upstream's marked-deleted view for items that were removed. */
fun veilDeletedPlaceholderApplies(ci: ChatItem): Boolean = ci.meta.itemDeleted != null && ci.content.msgContent != null

@Composable
private fun deliveryLabel(state: VeilDeliveryState): String = stringResource(
  when (state) {
    VeilDeliveryState.Sending -> MR.strings.veil_chat_state_sending
    VeilDeliveryState.Sent -> MR.strings.veil_chat_state_sent
    VeilDeliveryState.SentPartly -> MR.strings.veil_chat_state_sent_partly
    VeilDeliveryState.Delivered -> MR.strings.veil_chat_state_delivered
    VeilDeliveryState.DeliveredPartly -> MR.strings.veil_chat_state_delivered_partly
    VeilDeliveryState.DeliveredIntegrityWarning -> MR.strings.veil_chat_state_delivered_integrity
    VeilDeliveryState.Retrying -> MR.strings.veil_chat_state_retrying
    VeilDeliveryState.Failed -> MR.strings.veil_chat_state_failed
    VeilDeliveryState.Unknown -> MR.strings.veil_chat_state_unknown
  }
)

/** Folded-corner bubble for a text message. Long-press and the upstream menu are handled by the caller (`ChatItemView`). */
@Composable
fun VeilTextBubble(
  chatsCtx: ChatModel.ChatsContext,
  chat: Chat,
  ci: ChatItem,
  linkMode: SimplexLinkMode,
  onLinkLongClick: (String) -> Unit,
  onQuoteClick: () -> Unit,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val sent = ci.chatDir.sent
  val chatInfo = chat.chatInfo
  val itemContext = LocalVeilItemContext.current
  VeilBubble(outgoing = sent, modifier = Modifier.widthIn(min = 64.dp)) {
    Column {
      val dir = ci.chatDir
      if (dir is CIDirection.GroupRcv && itemContext.firstInRun) {
        val member = dir.groupMember
        BasicText(
          member.displayName,
          modifier = Modifier.padding(bottom = 2.dp),
          style = type.label.copy(color = Color(SENDER_NAME_COLORS[senderColorIndex(member.memberId)]), fontSize = 12.5.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      val quote = ci.quotedItem
      if (quote != null) {
        VeilQuote(quote, chatInfo, onQuoteClick)
      }
      MarkdownText(
        ci.text,
        if (ci.text.isEmpty()) emptyList() else ci.formattedText,
        toggleSecrets = true,
        sendCommandMsg = if (chatInfo.useCommands && chatInfo.sndReady) { { msg -> sendCommandMsg(chatsCtx, chat, msg) } } else null,
        mentions = ci.mentions,
        userMemberId = (chatInfo as? ChatInfo.Group)?.groupInfo?.membership?.memberId,
        style = type.body.copy(color = colors.ink),
        linkMode = linkMode,
        onLinkLongClick = onLinkLongClick,
        showTimestamp = false,
      )
      VeilMetaLine(ci, sent)
    }
  }
}

@Composable
private fun VeilQuote(quote: CIQuote, chatInfo: ChatInfo, onClick: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val sender = veilQuoteSender(quote, chatInfo)
  Column(
    Modifier
      .padding(bottom = 6.dp)
      .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
      .background(Color.Black.copy(alpha = 0.18f))
      .clickable(onClick = onClick)
      .drawBehind { drawRect(colors.iris, size = Size(2.dp.toPx(), size.height)) }
      .padding(start = 9.dp, top = 3.dp, bottom = 3.dp, end = 8.dp)
  ) {
    if (sender != null) {
      BasicText(sender, style = type.label.copy(color = colors.ink, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    BasicText(
      quote.text.ifEmpty { " " },
      style = type.caption.copy(color = colors.ink2, fontSize = 13.sp),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** Name above a quoted message: "Sen" for the user's own, the member in groups, the contact's name in direct chats. */
@Composable
private fun veilQuoteSender(quote: CIQuote, chatInfo: ChatInfo): String? = when (val d = quote.chatDir) {
  is CIDirection.DirectSnd, is CIDirection.GroupSnd, is CIDirection.LocalSnd -> stringResource(MR.strings.veil_chat_you)
  is CIDirection.GroupRcv -> d.groupMember.displayName
  is CIDirection.DirectRcv -> chatInfo.displayName
  else -> null
}

/** "14:32 · düzenlendi · Teslim edildi": time, edited marker and, for outgoing items, the delivery state in words. */
@Composable
private fun VeilMetaLine(ci: ChatItem, sent: Boolean) {
  val colors = VeilTheme.colors
  val state = if (sent) deliveryStateOf(ci.meta.itemStatus) else null
  val edited = ci.meta.itemEdited
  Row(
    Modifier.fillMaxWidth().padding(top = 2.dp),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val base = if (sent) colors.ink.copy(alpha = 0.62f) else colors.ink3
    val style = VeilTheme.type.caption.copy(color = base, fontSize = 11.sp)
    val stateColor = when (state) {
      VeilDeliveryState.Failed -> colors.errText
      VeilDeliveryState.Retrying, VeilDeliveryState.DeliveredIntegrityWarning -> colors.warn
      else -> base
    }
    val time = ci.timestampText
    val editedText = if (edited) stringResource(MR.strings.veil_chat_edited) else null
    BasicText(buildString {
      append(time)
      if (editedText != null) append(" · ").append(editedText)
    }, style = style, maxLines = 1)
    if (state != null) {
      BasicText(" · ", style = style, maxLines = 1)
      BasicText(deliveryLabel(state), style = style.copy(color = stateColor), maxLines = 1)
    }
  }
}

/** Dashed placeholder for a message that was deleted, blocked or moderated. */
@Composable
fun VeilDeletedBubble(ci: ChatItem) {
  val colors = VeilTheme.colors
  val res = when (val d = ci.meta.itemDeleted) {
    is CIDeleted.Moderated -> MR.strings.veil_chat_moderated
    is CIDeleted.Blocked -> MR.strings.veil_chat_blocked
    is CIDeleted.BlockedByAdmin -> MR.strings.veil_chat_blocked_admin
    else -> if (ci.chatDir.sent) MR.strings.veil_chat_deleted_own else MR.strings.veil_chat_deleted
  }
  val shape = RoundedCornerShape(14.dp)
  Row(
    Modifier
      .clip(shape)
      .drawBehind {
        val w = 1.dp.toPx()
        drawRoundRect(
          colors.line2, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
          cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
        )
      }
      .padding(horizontal = 13.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    BasicText(stringResource(res), style = VeilTheme.type.body.copy(color = colors.ink3, fontStyle = FontStyle.Italic), maxLines = 2)
    BasicText(" · " + ci.timestampText, style = VeilTheme.type.caption.copy(color = colors.ink3, fontSize = 11.sp), maxLines = 1)
  }
}

/** Reactions row, restyled as the prototype's pill that overlaps the bubble's lower edge. Mechanics stay upstream's. */
@Composable
fun Modifier.veilReactionPill(): Modifier {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.pill
  return this
    .offset(y = (-8).dp)
    .padding(horizontal = 8.dp)
    .clip(shape)
    .background(colors.night3)
    .border(1.dp, colors.line2, shape)
}

/** Centered date divider ("Bugün", "Dün", "3 Mart 2026"). */
@Composable
fun VeilDateDivider(date: Instant) {
  val colors = VeilTheme.colors
  val zone = remember { TimeZone.currentSystemDefault() }
  val label = remember(date) { dateLabelFor(localDateOf(date, zone), localDateOf(Clock.System.now(), zone)) }
  val text = when (label) {
    VeilDateLabel.Today -> stringResource(MR.strings.veil_chat_today)
    VeilDateLabel.Yesterday -> stringResource(MR.strings.veil_chat_yesterday)
    is VeilDateLabel.Date -> "${label.day} ${stringResource(MONTHS[label.month - 1])} ${label.year}"
  }
  Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
    BasicText(
      text,
      modifier = Modifier.clip(VeilTheme.shapes.pill).background(colors.sheet1).padding(horizontal = 12.dp, vertical = 3.dp),
      style = VeilTheme.type.caption.copy(color = colors.ink3),
      maxLines = 1,
    )
  }
}

private val MONTHS: List<StringResource> = listOf(
  MR.strings.veil_chat_month_1, MR.strings.veil_chat_month_2, MR.strings.veil_chat_month_3, MR.strings.veil_chat_month_4,
  MR.strings.veil_chat_month_5, MR.strings.veil_chat_month_6, MR.strings.veil_chat_month_7, MR.strings.veil_chat_month_8,
  MR.strings.veil_chat_month_9, MR.strings.veil_chat_month_10, MR.strings.veil_chat_month_11, MR.strings.veil_chat_month_12,
)

/** Small mask avatar for a group member next to the first message of a run. Seed rule as in the chat list: id plus profile name. */
@Composable
fun VeilMemberAvatar(member: GroupMember, size: androidx.compose.ui.unit.Dp) {
  MaskAvatar(seed = chatAvatarSeed("m${member.memberId}", member.memberProfile.displayName), size = size)
}
