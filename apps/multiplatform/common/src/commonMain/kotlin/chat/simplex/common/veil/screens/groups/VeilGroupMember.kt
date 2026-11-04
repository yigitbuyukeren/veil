package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.VeilInfoRow
import chat.simplex.common.veil.screens.chat.VeilScreenScaffold
import chat.simplex.common.veil.screens.chat.chatAvatarSeed
import chat.simplex.common.veil.screens.chat.veilShowMemberSecurityCode
import chat.simplex.common.veil.screens.chat.veilShowSheet
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.chat.group.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/*
 * Veil member screen: who, role, security code, and the actions the user's role allows (change role, remove, block).
 * Every action is upstream's dialog and API call (`updateMemberRoleDialog` + `updateMembersRole`, `removeMemberDialog`,
 * `blockMemberAlert`, `blockForAllAlert`, ...); [memberPermissions] decides which are offered, with upstream's rules.
 * Everything else about a member (direct message, connection details, address switch) is in upstream's own member
 * info, behind the "Ayrıntılar" row.
 */

/** Opens the screen for one member of the open group. */
fun veilShowMember(rhId: Long?, groupId: Long, groupMemberId: Long) {
  withBGApi {
    val groupInfo = (chatModel.getGroupChat(groupId)?.chatInfo as? ChatInfo.Group)?.groupInfo ?: return@withBGApi
    val member = chatModel.getGroupMember(groupMemberId) ?: return@withBGApi
    // same preload as upstream's member info: the security code only exists for an active, connected member
    val code = if (member.memberActive && member.memberRole != GroupMemberRole.Relay) {
      chatModel.controller.apiGetGroupMemberCode(rhId, groupInfo.apiId, member.groupMemberId)?.second
    } else null
    val canRemove = memberPermissions(memberFacts(groupInfo.membership.memberRole, groupInfo.membership.memberActive, member)).canRemove
    val hasLink = if (canRemove) chatModel.controller.apiGetGroupLink(rhId, groupInfo.groupId) != null else false
    ModalManager.end.showCustomModal { close ->
      VeilMemberScreen(rhId, groupId, groupMemberId, code, hasLink, close)
    }
  }
}

@Composable
fun VeilMemberScreen(rhId: Long?, groupId: Long, groupMemberId: Long, code: String?, linkActive: Boolean, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val groupInfo = remember(groupId) { derivedStateOf { (chatModel.getGroupChat(groupId)?.chatInfo as? ChatInfo.Group)?.groupInfo } }.value
  val member = remember(groupMemberId) { derivedStateOf { chatModel.getGroupMember(groupMemberId) } }.value
  if (groupInfo == null || member == null) {
    // removed, or the group went away, while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  val view = LocalMultiplatformView()
  LaunchedEffect(Unit) { hideKeyboard(view) }
  val perms = memberPermissions(memberFacts(groupInfo.membership.memberRole, groupInfo.membership.memberActive, member))
  val canVerify = code != null && member.memberActive && member.memberRole != GroupMemberRole.Relay

  VeilScreenScaffold(title = stringResource(MR.strings.veil_grp_member_title), onBack = close) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      MaskAvatar(
        seed = chatAvatarSeed("m${member.memberId}", member.memberProfile.displayName),
        size = 88.dp,
        incognito = member.memberIncognito,
        modifier = Modifier.padding(bottom = 10.dp),
      )
      BasicText(
        member.chatViewName,
        style = type.sheetTitle.copy(color = colors.ink, textAlign = TextAlign.Center),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      BasicText(
        memberSubtitle(member),
        Modifier.padding(top = 2.dp, bottom = 8.dp),
        style = type.caption.copy(color = colors.ink2, fontSize = 13.sp),
      )
      RoleChip(member.memberRole)
    }

    if (canVerify || perms.roles.isNotEmpty()) {
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        if (canVerify) {
          VeilInfoRow(
            icon = MR.images.ic_verified_user,
            title = stringResource(MR.strings.veil_verify_title),
            value = stringResource(if (member.verified) MR.strings.veil_chat_verified else MR.strings.veil_verify_unverified),
            onClick = { veilShowMemberSecurityCode(rhId, groupInfo, member, code!!) },
          )
        }
        if (perms.roles.isNotEmpty()) {
          VeilInfoRow(
            icon = MR.images.ic_group,
            title = stringResource(MR.strings.veil_grp_change_role),
            value = stringResource(roleLabel(member.memberRole)),
            onClick = { veilShowRoleSheet(rhId, groupInfo, member, perms.roles) },
          )
        }
      }
    }

    if (perms.blockedByAdminNote) {
      WarnBox(stringResource(MR.strings.member_blocked_by_admin))
      Spacer(Modifier.height(14.dp))
    }

    if (perms.canBlockForAll) {
      VeilButton(
        stringResource(MR.strings.veil_grp_block_all),
        onClick = { blockForAllAlert(rhId, groupInfo, member) },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Secondary,
      )
    }
    if (perms.canUnblockForAll) {
      VeilButton(
        stringResource(MR.strings.veil_grp_unblock_all),
        onClick = { unblockForAllAlert(rhId, groupInfo, member) },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Secondary,
      )
    }
    if (perms.canBlockLocal) {
      VeilButton(
        stringResource(MR.strings.veil_grp_block),
        onClick = { blockMemberAlert(rhId, groupInfo, member) },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Secondary,
      )
    }
    if (perms.canUnblockLocal) {
      VeilButton(
        stringResource(MR.strings.veil_grp_unblock),
        onClick = { unblockMemberAlert(rhId, groupInfo, member) },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Secondary,
      )
    }

    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      // upstream's full member info: direct message, connection details, address switch
      VeilInfoRow(
        icon = MR.images.ic_settings_backup_restore,
        title = stringResource(MR.strings.veil_grp_member_details),
        onClick = { openUpstreamMemberInfo(rhId, groupInfo, member, code, close) },
      )
    }

    if (perms.canRemove) {
      VeilButton(
        stringResource(MR.strings.veil_grp_remove),
        onClick = { removeMemberDialog(rhId, groupInfo, member, chatModel, close) },
        style = VeilButtonStyle.Danger,
      )
      if (linkActive) {
        BasicText(
          stringResource(MR.strings.veil_grp_rejoin_note),
          Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
          style = type.caption.copy(color = colors.warn, fontSize = 12.5.sp, lineHeight = 17.sp),
        )
      }
    }
    if (perms.canDeleteMessages) {
      VeilButton(
        stringResource(MR.strings.veil_grp_delete_messages),
        onClick = { deleteMemberMessagesDialog(rhId, groupInfo, member, chatModel, close) },
        style = VeilButtonStyle.Danger,
      )
    }
  }
}

/** Role picker as a sheet; the choice goes through upstream's confirmation dialog and API call. */
private fun veilShowRoleSheet(rhId: Long?, groupInfo: GroupInfo, member: GroupMember, roles: List<GroupMemberRole>) {
  veilShowSheet { close ->
    val colors = VeilTheme.colors
    val type = VeilTheme.type
    VeilSheet {
      BasicText(stringResource(MR.strings.veil_grp_change_role), Modifier.padding(bottom = 8.dp), style = type.sheetTitle.copy(color = colors.ink))
      for (role in roles.reversed()) {
        val current = role == member.memberRole
        Row(
          Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(VeilTheme.shapes.input)
            .clickable(role = Role.Button) {
              close()
              if (!current) {
                updateMemberRoleDialog(role, groupInfo, member.memberCurrent, onDismiss = {}) {
                  updateMembersRole(role, rhId, groupInfo, listOf(member.groupMemberId))
                }
              }
            }
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Column(Modifier.weight(1f)) {
            BasicText(stringResource(roleLabel(role)), style = type.bodyLarge.copy(color = colors.ink))
          }
          if (current) VeilIcon(MR.images.ic_check, colors.mintInk, size = 20.dp)
        }
      }
    }
  }
}

/** Upstream's `GroupMemberInfoView` in its own modal, with the same preload and wiring as `GroupChatInfoView` gives it. */
private fun openUpstreamMemberInfo(rhId: Long?, groupInfo: GroupInfo, member: GroupMember, code: String?, closeScreen: () -> Unit) {
  val scrollToItemId: MutableState<Long?> = mutableStateOf(null)
  withBGApi {
    val stats = chatModel.controller.apiGroupMemberInfo(rhId, groupInfo.groupId, member.groupMemberId)?.second
    ModalManager.end.showModalCloseable(showClose = true, cardScreen = true) { closeCurrent ->
      remember { derivedStateOf { chatModel.getGroupMember(member.groupMemberId) } }.value?.let { mem ->
        GroupMemberInfoView(rhId, groupInfo, mem, scrollToItemId, stats, code, chatModel, openedFromSupportChat = false, groupRelay = null, close = closeCurrent) {
          closeCurrent()
          closeScreen()
        }
      }
    }
  }
}
