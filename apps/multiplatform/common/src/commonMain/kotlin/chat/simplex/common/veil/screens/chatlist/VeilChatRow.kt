package chat.simplex.common.veil.screens.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.chatlist.unreadCountStr
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** How long a tapped chat is waited for to open before the veil is put back (e.g. when the tap only opened a dialog). */
private const val OPEN_TIMEOUT_MS = 3000L

@Composable
internal fun VeilIcon(res: ImageResource, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, description: String? = null) {
  Icon(painterResource(res), description, modifier.size(size), tint = tint)
}

/**
 * One chat row. Preview text sits in a [Perde]: veiled by default, revealed while a finger is held on the row, while
 * the eye is on, and while the chat is being opened (the veil lifts first, then the chat opens).
 *
 * Gestures: tap opens the chat; press and hold (after 150 ms) reveals until release and the release does not open the
 * chat; tap on the avatar opens the chat actions; the eye button toggles the reveal.
 */
@Composable
internal fun VeilChatRow(
  chat: Chat,
  preview: VeilPreview,
  eyeOn: Boolean,
  onToggleEye: () -> Unit,
  onOpen: () -> Unit,
  onAvatarTap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val motion = VeilTheme.motion
  val reduce = VeilTheme.reduceMotion
  var held by remember { mutableStateOf(false) }
  var opening by remember { mutableStateOf(false) }
  // set when a hold started during the current press, so that the release of a hold does not count as a tap
  val holdUsed = remember { BooleanArray(1) }
  val onOpenState by rememberUpdatedState(onOpen)

  LaunchedEffect(opening) {
    if (!opening) return@LaunchedEffect
    delay(motion.lift(reduce).toLong())
    onOpenState()
    val opened = withTimeoutOrNull(OPEN_TIMEOUT_MS) { snapshotFlow { chatModel.chatId.value != null }.first { it } }
    if (opened != null) snapshotFlow { chatModel.chatId.value == null }.first { it }
    opening = false
  }

  // Stable lambdas: perdeHold re-creates its pointer handler when they change, which would cancel a hold in progress.
  val onHold = remember { { h: Boolean -> held = h; if (h) holdUsed[0] = true } }
  val onTap = remember { { if (holdUsed[0]) holdUsed[0] = false else opening = true } }

  val cInfo = chat.chatInfo
  val name = cInfo.chatViewName
  val canReveal = preview.sensitive
  val revealed = !canReveal || held || eyeOn || opening
  val rowDesc = String.format(stringResource(MR.strings.veil_list_row_desc), name)
  val avatarDesc = String.format(stringResource(MR.strings.veil_list_avatar_actions), name)

  Row(
    modifier
      .fillMaxWidth()
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val e = awaitPointerEvent(PointerEventPass.Initial)
            if (e.type == PointerEventType.Press) holdUsed[0] = false
          }
        }
      }
      .perdeHold(onTap = onTap, onHoldChange = onHold)
      .semantics { contentDescription = rowDesc; role = Role.Button; onClick { opening = true; true } }
      .padding(horizontal = 18.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier
        .padding(end = 3.dp, bottom = 3.dp)
        .clip(VeilTheme.shapes.button)
        .clickable(role = Role.Button, onClick = onAvatarTap)
        .semantics { contentDescription = avatarDesc }
    ) {
      MaskAvatar(veilAvatarSeed(chat), 52.dp, incognito = cInfo.incognito)
    }
    Column(Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (cInfo is ChatInfo.Group) {
            VeilIcon(MR.images.ic_group, colors.ink3, size = 16.dp, description = stringResource(MR.strings.veil_list_group_desc))
          }
          BasicText(name, Modifier.weight(1f, fill = false), style = type.rowTitle.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BasicText(
          getTimestampText(chat.chatItems.lastOrNull()?.meta?.itemTs ?: cInfo.chatTs),
          style = type.caption.copy(color = colors.ink3), maxLines = 1
        )
      }
      Spacer(Modifier.height(3.dp))
      Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (canReveal) {
          Perde(revealed = revealed, modifier = Modifier.weight(1f).heightIn(min = 24.dp)) {
            BasicText(
              preview.text,
              Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
              style = type.body.copy(color = colors.ink2, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
          }
        } else {
          BasicText(
            preview.text,
            Modifier.weight(1f).padding(horizontal = 5.dp),
            style = type.body.copy(color = colors.ink3, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis
          )
        }
        Indicators(chat)
        if (canReveal) {
          // 48dp touch target; the label says what a tap does ("Önizlemeyi göster" / "Önizlemeyi gizle")
          Box(
            Modifier
              .size(48.dp)
              .clip(VeilTheme.shapes.input)
              .toggleable(value = eyeOn, role = Role.Switch, onValueChange = { onToggleEye() }),
            contentAlignment = Alignment.Center
          ) {
            VeilIcon(
              if (eyeOn) MR.images.ic_visibility_filled else MR.images.ic_visibility,
              if (eyeOn) colors.mintInk else colors.ink3,
              size = 18.dp,
              description = stringResource(if (eyeOn) MR.strings.veil_list_eye_hide else MR.strings.veil_list_eye_show)
            )
          }
        }
      }
    }
  }
}

/** Muted / favorite markers and the unread badge. */
@Composable
private fun Indicators(chat: Chat) {
  val colors = VeilTheme.colors
  val cInfo = chat.chatInfo
  val n = chat.chatStats.unreadCount
  val muted = !cInfo.ntfsEnabled(false) && (cInfo is ChatInfo.Direct || cInfo is ChatInfo.Group)
  if (muted) {
    VeilIcon(MR.images.ic_notifications_off_filled, colors.ink3, size = 15.dp, description = stringResource(MR.strings.veil_list_muted_desc))
  }
  if (cInfo.chatSettings?.favorite == true) {
    VeilIcon(MR.images.ic_star_filled, colors.ink3, size = 15.dp, description = stringResource(MR.strings.veil_list_favorite_desc))
  }
  if (n > 0 || chat.chatStats.unreadChat) {
    val desc = if (n > 0) String.format(stringResource(MR.strings.veil_list_unread_desc), n) else stringResource(MR.strings.veil_list_marked_unread_desc)
    val shape = CircleShape
    val badge = Modifier
      .semantics { contentDescription = desc }
      .clip(shape)
      .then(if (muted) Modifier.background(colors.sheet3) else Modifier.drawBehind { drawRect(colors.gradientFor(size, 120f)) })
    if (n > 0) {
      Box(badge.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
        BasicText(unreadCountStr(n), style = VeilTheme.type.chip.copy(color = if (muted) colors.ink else colors.onGradient, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
      }
    } else {
      Box(badge.size(12.dp))
    }
  }
}
