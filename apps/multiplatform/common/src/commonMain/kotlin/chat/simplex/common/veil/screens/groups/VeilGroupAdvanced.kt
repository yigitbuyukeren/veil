package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.VeilAliasField
import chat.simplex.common.veil.screens.chat.VeilCaption
import chat.simplex.common.veil.screens.chat.VeilChoiceRow
import chat.simplex.common.veil.screens.chat.VeilInfoRow
import chat.simplex.common.veil.screens.chat.VeilScreenScaffold
import chat.simplex.common.veil.screens.chat.chatAvatarSeed
import chat.simplex.common.veil.screens.chat.chatTtlChanged
import chat.simplex.common.veil.screens.chat.chatTtlOptions
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.settings.VeilSectionLabel
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chat.group.*
import chat.simplex.common.views.chatlist.updateChatSettings
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Veil "Gelişmiş" page of a group: replaces upstream's complete `GroupChatInfoView` behind the group info's "Gelişmiş" row.
 *
 * What upstream's screen does, and where it is now:
 * - mute, add members, group link (create, role, delete), preferences, edit profile, welcome message, clear chat, leave
 *   group, delete group, member list with member screens: already on the Veil group info (VeilGroupInfo.kt), with the
 *   same calls and upstream's dialogs (`leaveGroupDialog`, `deleteGroupDialog`, `clearChatDialog`, `removeMemberDialog` ...).
 * - local name: `apiSetGroupAlias` + `updateGroup` (here).
 * - delivery receipts: `updateChatSettings` with `ChatSettings.sendRcpts`, off above `SMALL_GROUPS_RCPS_MEM_LIMIT`
 *   members with upstream's alert (here).
 * - delete messages after: `setChatTTLAlert` (upstream's confirmation) then `setChatTTL` (here).
 * - member support chats, the user's own support chat, member reports: upstream's `MemberSupportView`,
 *   `showMemberSupportChatView`, `showGroupReportsView` (opened from here).
 * - several members at once (remove, block for everyone, change role): [VeilGroupBulkScreen], with upstream's
 *   `removeMembersAlert`, `blockForAllAlert` / `unblockForAllAlert`, `updateMembersRoleDialog` + `updateMembersRole`.
 * - developer tools: local name and database id (here, only with developer tools on).
 * - not offered: the search button (the Veil chat has its own search) and the chat theme editor (the Veil chat draws
 *   its own background, so a wallpaper would have no effect). Channels and business chats are not shown by Veil's group info.
 */
fun veilShowGroupAdvanced(rhId: Long?, groupId: Long) {
  ModalManager.end.showCustomModal { close -> VeilGroupAdvancedScreen(rhId, groupId, close) }
}

@Composable
fun VeilGroupAdvancedScreen(rhId: Long?, groupId: Long, close: () -> Unit) {
  val chat = remember(groupId) { derivedStateOf { chatModel.getGroupChat(groupId) } }.value
  val groupInfo = (chat?.chatInfo as? ChatInfo.Group)?.groupInfo
  val user = chatModel.currentUser.value
  if (chat == null || groupInfo == null || user == null) {
    // the group was deleted while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  val chatRh = chat.remoteHostId
  val scope = rememberCoroutineScope()
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  val membership = groupInfo.membership
  val blocks = groupAdvancedBlocks(
    userRole = membership.memberRole,
    userActive = membership.memberActive,
    hasBusinessChat = groupInfo.businessChat != null,
    canModerate = groupInfo.canModerate,
    supportOn = groupInfo.fullGroupPreferences.support.on,
    hasOwnSupportChat = membership.supportChat != null,
    developerTools = developerTools,
  )
  val currentMembers = activeSorted(chatModel.groupMembers.value).count { it.memberCurrent } + 1
  val sendReceipts = remember { mutableStateOf(SendReceipts.fromBool(groupInfo.chatSettings.sendRcpts, user.sendRcptsSmallGroups)) }
  val chatItemTTL = remember(groupInfo.id) { mutableStateOf(if (groupInfo.chatItemTTL != null) ChatItemTTL.fromSeconds(groupInfo.chatItemTTL) else null) }
  val deletingItems = rememberSaveable(groupInfo.id) { mutableStateOf(false) }
  val scrollToItemId: MutableState<Long?> = remember { mutableStateOf(null) }

  VeilScreenScaffold(title = stringResource(MR.strings.veil_chatinfo_advanced), onBack = close, subtitle = groupInfo.displayName) {
    // ---- local name ----
    VeilAliasField(
      key = chat.id,
      initial = groupInfo.localAlias,
      placeholder = stringResource(MR.strings.veil_chatinfo_alias_hint),
      focus = false,
      save = { alias ->
        withBGApi {
          chatModel.controller.apiSetGroupAlias(chatRh, groupInfo.apiId, alias)?.let {
            withContext(Dispatchers.Main) { chatModel.chatsContext.updateGroup(chatRh, it) }
          }
        }
      },
    )

    // ---- support and reports ----
    if (blocks.memberSupport || blocks.reports || blocks.userSupport) {
      VeilSectionLabel(stringResource(MR.strings.veil_gadv_support))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        if (blocks.memberSupport) {
          VeilInfoRow(
            icon = if (chat.supportUnreadCount > 0) MR.images.ic_flag_filled else MR.images.ic_flag,
            title = stringResource(MR.strings.veil_gadv_member_support),
            value = countValue(chat.supportUnreadCount),
            onClick = {
              ModalManager.end.showCustomModal { closeSupport -> MemberSupportView(chatRh, chat, groupInfo, scrollToItemId, closeSupport) }
            },
          )
        }
        if (blocks.reports) {
          VeilInfoRow(
            icon = if (chat.chatStats.reportsCount > 0) MR.images.ic_flag_filled else MR.images.ic_flag,
            title = stringResource(MR.strings.veil_gadv_reports),
            value = countValue(chat.chatStats.reportsCount),
            onClick = { scope.launch { showGroupReportsView(chatModel.chatId, scrollToItemId, chat.chatInfo) } },
          )
        }
        if (blocks.userSupport) {
          VeilInfoRow(
            icon = if (chat.supportUnreadCount > 0) MR.images.ic_flag_filled else MR.images.ic_flag,
            title = stringResource(MR.strings.veil_gadv_own_support),
            value = countValue(chat.supportUnreadCount),
            onClick = {
              val scopeInfo = GroupChatScopeInfo.MemberSupport(groupMember_ = null)
              val supportChatInfo = ChatInfo.Group(groupInfo, groupChatScope = scopeInfo)
              scope.launch { showMemberSupportChatView(chatModel.chatId, scrollToItemId = scrollToItemId, supportChatInfo, scopeInfo) }
            },
          )
        }
      }
    }

    // ---- messages ----
    VeilSectionLabel(stringResource(MR.strings.veil_cadv_messages))
    VeilCard(Modifier.padding(bottom = 6.dp), contentPadding = PaddingValues(8.dp)) {
      if (receiptsAvailable(currentMembers, SMALL_GROUPS_RCPS_MEM_LIMIT)) {
        VeilChoiceRow(
          title = stringResource(MR.strings.veil_cadv_receipts),
          options = listOf(SendReceipts.Yes, SendReceipts.No, SendReceipts.UserDefault(user.sendRcptsSmallGroups)).map { it to it.text },
          selected = sendReceipts.value,
          onSelect = { sendRcpts ->
            val chatSettings = (chat.chatInfo.chatSettings ?: ChatSettings.defaults).copy(sendRcpts = sendRcpts.bool)
            updateChatSettings(chatRh, chat.chatInfo, chatSettings, chatModel)
            sendReceipts.value = sendRcpts
          },
        )
      } else {
        VeilInfoRow(
          icon = MR.images.ic_double_check,
          title = stringResource(MR.strings.veil_cadv_receipts),
          value = stringResource(MR.strings.veil_gadv_receipts_off),
          // upstream's explanation: receipts are not sent in large groups
          onClick = {
            AlertManager.shared.showAlertMsg(
              title = generalGetString(MR.strings.send_receipts_disabled_alert_title),
              text = String.format(generalGetString(MR.strings.send_receipts_disabled_alert_msg), SMALL_GROUPS_RCPS_MEM_LIMIT),
            )
          },
        )
      }
      VeilChoiceRow(
        title = stringResource(MR.strings.veil_cadv_ttl),
        options = chatTtlOptions(chatItemTTL.value, hasDefault = true).map { ttl ->
          ttl to (ttl?.text ?: String.format(generalGetString(MR.strings.chat_item_ttl_default), chatModel.chatItemTTL.value.text))
        },
        selected = chatItemTTL.value,
        enabled = !deletingItems.value,
        onSelect = { selected ->
          if (!chatTtlChanged(selected, chatItemTTL.value)) return@VeilChoiceRow
          val previous = chatItemTTL.value
          chatItemTTL.value = selected
          // upstream's confirmation; it applies the change or puts the previous value back
          setChatTTLAlert(chatModel.chatsContext, chatRh, chat.chatInfo, chatItemTTL, previous, deletingItems)
        },
      )
    }
    VeilCaption(stringResource(if (deletingItems.value) MR.strings.veil_cadv_ttl_working else MR.strings.veil_cadv_ttl_note), Modifier.padding(bottom = 10.dp))

    // ---- several members at once ----
    if (blocks.bulkMembers) {
      VeilSectionLabel(stringResource(MR.strings.veil_grp_members))
      VeilCard(Modifier.padding(bottom = 6.dp), contentPadding = PaddingValues(8.dp)) {
        VeilInfoRow(
          icon = MR.images.ic_group,
          title = stringResource(MR.strings.veil_gadv_manage),
          onClick = { ModalManager.end.showCustomModal { closeManage -> VeilGroupBulkScreen(chatRh, groupId, closeManage) } },
        )
      }
      VeilCaption(stringResource(MR.strings.veil_gadv_manage_note), Modifier.padding(bottom = 10.dp))
    }

    // ---- developer tools ----
    if (blocks.developer) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_developer))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        VeilInfoRow(icon = MR.images.ic_info, title = stringResource(MR.strings.veil_cadv_local_name), value = groupInfo.localDisplayName)
        VeilInfoRow(icon = MR.images.ic_info, title = stringResource(MR.strings.veil_cadv_db_id), value = groupInfo.apiId.toString())
      }
    }
  }
}

private fun countValue(count: Int): String? = if (count > 0) count.toString() else null

/**
 * Several members at once: tick members below moderator, then remove them, block or unblock them for everyone, or change
 * their role. Each action is upstream's dialog and call (`removeMembersAlert`, `blockForAllAlert` / `unblockForAllAlert`,
 * `updateMembersRoleDialog` + `updateMembersRole`); [bulkActions] decides which are offered, with upstream's rules.
 */
@Composable
fun VeilGroupBulkScreen(rhId: Long?, groupId: Long, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val groupInfo = remember(groupId) { derivedStateOf { (chatModel.getGroupChat(groupId)?.chatInfo as? ChatInfo.Group)?.groupInfo } }.value
  if (groupInfo == null) {
    LaunchedEffect(Unit) { close() }
    return
  }
  val membership = groupInfo.membership
  val userRole = membership.memberRole
  val userActive = membership.memberActive
  val all = chatModel.groupMembers.value
  val members = remember(all) { activeSorted(all) }
  val selectable = remember(members, userRole, userActive) { members.filter { canSelectMember(userRole, userActive, it.memberRole) } }
  val selectableIds = remember(selectable) { selectable.map { it.groupMemberId }.toSet() }
  var picked by remember { mutableStateOf(setOf<Long>()) }
  // members that left or lost the right to be selected drop out of the selection, like upstream's recheck
  val selected = picked.intersect(selectableIds)
  val actions = bulkActions(
    userRole, userActive,
    selectable.filter { it.groupMemberId in selected }.map { BulkMember(it.groupMemberId, it.memberRole, it.memberStatus, it.blockedByAdmin) },
  )
  val ids = selected.sorted()

  VeilScreenScaffold(title = stringResource(MR.strings.veil_gadv_manage), onBack = close, subtitle = groupInfo.displayName) {
    BasicText(
      stringResource(MR.strings.veil_gadv_manage_hint),
      Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
      style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
    )
    if (selectable.isEmpty()) {
      BasicText(stringResource(MR.strings.veil_gadv_manage_none), Modifier.padding(horizontal = 4.dp), style = type.body.copy(color = colors.ink2, fontSize = 14.sp))
      return@VeilScreenScaffold
    }

    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(12.dp)) {
      BasicText(
        String.format(stringResource(MR.strings.veil_gadv_selected), selected.size),
        Modifier.padding(bottom = 10.dp),
        style = type.rowTitle.copy(color = colors.ink, fontSize = 15.sp),
      )
      VeilButton(
        stringResource(MR.strings.veil_gadv_act_remove),
        onClick = { removeMembersAlert(rhId, groupInfo, ids) { picked = emptySet() } },
        modifier = Modifier.padding(bottom = 8.dp),
        style = VeilButtonStyle.Danger,
        enabled = actions.remove,
      )
      VeilButton(
        stringResource(if (actions.unblockForAll) MR.strings.veil_grp_unblock_all else MR.strings.veil_grp_block_all),
        onClick = {
          if (actions.unblockForAll) unblockForAllAlert(rhId, groupInfo, ids) { picked = emptySet() }
          else blockForAllAlert(rhId, groupInfo, ids) { picked = emptySet() }
        },
        modifier = Modifier.padding(bottom = 8.dp),
        style = VeilButtonStyle.Secondary,
        enabled = actions.blockForAll || actions.unblockForAll,
      )
      val target = actions.roleTarget
      VeilButton(
        stringResource(if (target == GroupMemberRole.Member) MR.strings.veil_gadv_act_to_member else MR.strings.veil_gadv_act_to_observer),
        onClick = {
          if (target != null) {
            updateMembersRoleDialog(target, groupInfo) {
              updateMembersRole(target, rhId, groupInfo, ids) { picked = emptySet() }
            }
          }
        },
        style = VeilButtonStyle.Secondary,
        enabled = target != null,
      )
    }

    VeilButton(
      stringResource(if (selectableIds.isNotEmpty() && selected.containsAll(selectableIds)) MR.strings.veil_gadv_clear else MR.strings.veil_gadv_select_all),
      onClick = { picked = toggleSelectAll(selectableIds, selected) },
      modifier = Modifier.padding(bottom = 14.dp),
      style = VeilButtonStyle.Secondary,
    )

    VeilCard(Modifier.padding(bottom = 18.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
      for (m in selectable) {
        val checked = m.groupMemberId in selected
        Row(
          Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(VeilTheme.shapes.input)
            .clickable(role = Role.Checkbox) { picked = toggleSelected(selected, m.groupMemberId) }
            .padding(horizontal = 6.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          MaskAvatar(
            seed = chatAvatarSeed("m${m.memberId}", m.memberProfile.displayName),
            size = 38.dp,
            incognito = m.memberIncognito,
            modifier = Modifier.padding(end = if (m.memberIncognito) 3.dp else 0.dp),
          )
          Column(Modifier.weight(1f)) {
            BasicText(m.chatViewName, style = type.rowTitle.copy(color = colors.ink, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(memberSubtitle(m), style = type.caption.copy(color = colors.ink2, fontSize = 12.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          RoleChip(m.memberRole)
          VeilIcon(if (checked) MR.images.ic_check_circle_filled else MR.images.ic_circle, if (checked) colors.mintInk else colors.ink3, size = 24.dp)
        }
      }
    }
  }
}
