package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.views.helpers.fontSizeSqrtMultiplier
import chat.simplex.common.veil.data.rememberTorRingState
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.chatlist.veilAvatarName
import chat.simplex.common.views.helpers.AppBarHeight
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Top bar of the Veil chat: back, mask avatar, name with verification / member chips, search, Tor ring.
 * Same height as upstream's app bar (the message list's top padding is computed from it). Calls are not offered.
 *
 * Tapping the name area runs [VeilChatHooks.onChatInfo], which by default calls [openInfo] (upstream's chat info).
 */
@Composable
fun VeilChatTopBar(
  chatInfo: ChatInfo,
  back: () -> Unit,
  openInfo: () -> Unit,
  openSearch: () -> Unit,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  BackHandler(onBack = back)
  val ring = rememberTorRingState()
  val isDirect = chatInfo is ChatInfo.Direct
  val group = (chatInfo as? ChatInfo.Group)?.groupInfo
  val subtitles = remember(chatInfo) {
    barSubtitles(
      isDirect = isDirect,
      isGroup = group != null,
      verified = (chatInfo as? ChatInfo.Direct)?.contact?.verified == true,
      memberCount = group?.groupSummary?.currentMembers ?: 0L,
      incognito = chatInfo.incognito,
    )
  }
  val hasInfo = chatInfo !is ChatInfo.Local
  Row(
    Modifier
      .fillMaxWidth()
      .background(colors.night.copy(alpha = 0.94f))
      .statusBarsPadding()
      .height(AppBarHeight * fontSizeSqrtMultiplier)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    VeilIconButton(onClick = back, contentDescription = stringResource(MR.strings.veil_chat_back)) {
      VeilIcon(MR.images.ic_arrow_back_ios_new, colors.ink2, size = 20.dp)
    }
    val infoDescription = stringResource(MR.strings.veil_chat_open_info)
    Row(
      Modifier
        .weight(1f)
        .then(
          if (hasInfo) Modifier
            .clickable(role = Role.Button) { VeilChatHooks.onChatInfo(openInfo) }
            .semantics { contentDescription = infoDescription }
          else Modifier
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
      MaskAvatar(
        seed = chatAvatarSeed(chatInfo.id, veilAvatarName(chatInfo)),
        size = 38.dp,
        incognito = chatInfo.incognito,
        modifier = Modifier.padding(end = if (chatInfo.incognito) 3.dp else 0.dp),
      )
      Column(Modifier.weight(1f)) {
        BasicText(
          chatInfo.displayName,
          style = type.title.copy(color = colors.ink, fontSize = 18.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (subtitles.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for (s in subtitles) when (s) {
              is VeilBarSubtitle.Verified ->
                if (s.verified) VeilChip(stringResource(MR.strings.veil_chat_verified), style = VeilChipStyle.Gradient, onClick = { VeilChatHooks.onVerifyCode(chatInfo, openInfo) })
                else VeilChip(stringResource(MR.strings.veil_chat_unverified), onClick = { VeilChatHooks.onVerifyCode(chatInfo, openInfo) })
              is VeilBarSubtitle.Members ->
                BasicText(
                  stringResource(MR.strings.veil_chat_members, s.count.toInt()),
                  style = type.caption.copy(color = colors.ink2),
                  maxLines = 1,
                )
              VeilBarSubtitle.Incognito -> VeilChip(stringResource(MR.strings.veil_chat_incognito), style = VeilChipStyle.Dashed)
            }
          }
        }
      }
    }
    if (chatInfo !is ChatInfo.Local) {
      VeilIconButton(onClick = openSearch, contentDescription = stringResource(MR.strings.veil_chat_search)) {
        VeilIcon(MR.images.ic_search, colors.ink2, size = 22.dp)
      }
    }
    Box(Modifier.padding(horizontal = 8.dp)) { TorRing(ring, 26.dp) }
  }
}
