package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boofcv.alg.fiducial.qrcode.QrCode
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.VeilInfoRow
import chat.simplex.common.veil.screens.chat.VeilScreenScaffold
import chat.simplex.common.veil.screens.chat.chatAvatarSeed
import chat.simplex.common.veil.screens.chat.veilShowSheet
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.system.VeilAlertButton
import chat.simplex.common.veil.screens.system.VeilAlertButtons
import chat.simplex.common.veil.screens.system.VeilAlertRole
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chat.group.*
import chat.simplex.common.views.chatlist.cantInviteIncognitoAlert
import chat.simplex.common.views.chatlist.setGroupMembers
import chat.simplex.common.views.chatlist.toggleNotifications
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.qrCodeBitmap
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Veil group info (screen group E) for Android: header, members with role chips, group link card, preferences.
 *
 * Every action is upstream's: `apiCreateGroupLink` / `apiGroupLinkMemberRole` / `apiDeleteGroupLink` / `apiAddGroupShortLink`
 * for the link (this card also stands in for upstream's `GroupLinkView`), the Veil add-members page (VeilGroupAddMembers.kt,
 * instead of `AddGroupMembersView`) for invitations, `VeilGroupPrefsScreen` / `VeilGroupProfileScreen` / `VeilGroupWelcomeScreen` for
 * preferences, `clearChatDialog` / `leaveGroupDialog` / `deleteGroupDialog` for the destructive actions, and the "Gelişmiş" row
 * opens the Veil page that replaces `GroupChatInfoView` (VeilGroupAdvanced.kt). Which rows appear follows [VeilGroupsLogic].
 */

/**
 * Tap on the group name in the chat top bar ([chat.simplex.common.veil.screens.chat.VeilChatHooks.onChatInfo] for groups).
 * Preloads members and the link before showing the screen, like upstream.
 */
fun veilOpenGroupInfo(chat: Chat, info: ChatInfo.Group) {
  // same toggle as upstream's info(): a second tap closes the open info
  if (ModalManager.end.hasModalsOpen()) {
    ModalManager.end.closeModals()
    return
  }
  val rhId = chat.remoteHostId
  val groupInfo = info.groupInfo
  withBGApi {
    setGroupMembers(rhId, groupInfo, chatModel)
    val link = if (canAddMembers(groupInfo.membership.memberRole, groupInfo.membership.memberActive)) chatModel.controller.apiGetGroupLink(rhId, groupInfo.groupId) else null
    veilShowGroupInfo(rhId, groupInfo.groupId, link)
  }
}

/** Shows the screen of an already preloaded group (also used right after an incognito group is created). */
fun veilShowGroupInfo(rhId: Long?, groupId: Long, link: GroupLink?) {
  ModalManager.end.showCustomModal { close ->
    VeilGroupInfoScreen(rhId, groupId, link, close)
  }
}

@Composable
fun VeilGroupInfoScreen(rhId: Long?, groupId: Long, initialLink: GroupLink?, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val chat = remember(groupId) { derivedStateOf { chatModel.getGroupChat(groupId) } }.value
  val groupInfo = (chat?.chatInfo as? ChatInfo.Group)?.groupInfo
  val user = chatModel.currentUser.value
  if (chat == null || groupInfo == null || user == null) {
    // the group was deleted while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  val view = LocalMultiplatformView()
  LaunchedEffect(Unit) { hideKeyboard(view) }
  var link by remember(groupId) { mutableStateOf(initialLink) }
  val allMembers = chatModel.groupMembers.value
  val members = remember(allMembers) { activeSorted(allMembers) }
  val membership = groupInfo.membership
  val canManage = canAddMembers(membership.memberRole, membership.memberActive)
  val notifications = groupInfo.chatSettings.enableNtfs
  val removedCount = removedMembersCount(allMembers.map { it.memberStatus })
  var search by remember { mutableStateOf("") }
  val shown = remember(members, search) {
    val s = search.trim().lowercase()
    if (s.isEmpty()) members else members.filter { it.anyNameContains(s) }
  }

  VeilScreenScaffold(title = stringResource(MR.strings.veil_grp_info_title), onBack = close) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      MaskAvatar(
        seed = chatAvatarSeed(chat.id, groupInfo.groupProfile.displayName),
        size = 88.dp,
        incognito = groupInfo.incognito,
        modifier = Modifier.padding(bottom = 10.dp),
      )
      BasicText(
        groupInfo.displayName,
        style = type.sheetTitle.copy(color = colors.ink, textAlign = TextAlign.Center),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      BasicText(
        stringResource(MR.strings.veil_grp_member_count, members.size + 1),
        Modifier.padding(top = 4.dp),
        style = type.caption.copy(color = colors.ink2, fontSize = 13.sp),
      )
      if (groupInfo.incognito) {
        Spacer(Modifier.height(8.dp))
        VeilChip(stringResource(MR.strings.veil_chat_incognito), style = VeilChipStyle.Dashed)
      }
    }

    // ---- members ----
    SectionLabel(stringResource(MR.strings.veil_grp_members))
    VeilCard(Modifier.padding(bottom = 18.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
      if (canManage) {
        VeilInfoRow(
          icon = MR.images.ic_person_add_500,
          title = stringResource(MR.strings.veil_grp_add_members),
          onClick = {
            if (groupInfo.incognito) cantInviteIncognitoAlert()
            else veilShowAddMembers(rhId, groupInfo)
          },
        )
      }
      if (members.size > 8) {
        VeilTextField(search, { search = it }, Modifier.padding(vertical = 6.dp), placeholder = stringResource(MR.strings.veil_grp_search))
      }
      MemberRow(
        name = membership.chatViewName,
        seed = chatAvatarSeed("m${membership.memberId}", membership.memberProfile.displayName),
        incognito = membership.memberIncognito,
        subtitle = stringResource(MR.strings.veil_grp_you),
        role = membership.memberRole,
        onClick = null,
      )
      for (m in shown) {
        MemberRow(
          name = m.chatViewName,
          seed = chatAvatarSeed("m${m.memberId}", m.memberProfile.displayName),
          incognito = m.memberIncognito,
          subtitle = memberSubtitle(m),
          role = m.memberRole,
          onClick = { veilShowMember(rhId, groupId, m.groupMemberId) },
        )
      }
    }

    // ---- group link (admins and owners, like upstream) ----
    if (canManage) {
      SectionLabel(stringResource(MR.strings.veil_grp_link))
      GroupLinkCard(
        rhId = rhId,
        groupInfo = groupInfo,
        link = link,
        onLinkChanged = { link = it },
        showRejoinNote = showRejoinNote(linkActive = link != null, removedMembers = removedCount),
      )
    }

    // ---- settings ----
    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      VeilInfoRow(
        icon = if (notifications != MsgFilter.None) MR.images.ic_notifications else MR.images.ic_notifications_off,
        title = stringResource(MR.strings.veil_chatinfo_notifications),
        value = stringResource(
          when (notifications) {
            MsgFilter.All -> MR.strings.veil_chatinfo_notifications_on
            MsgFilter.Mentions -> MR.strings.veil_grp_notifications_mentions
            MsgFilter.None -> MR.strings.veil_chatinfo_notifications_off
          }
        ),
        onClick = { toggleNotifications(rhId, chat.chatInfo, notifications.nextMode(true), chatModel) },
      )
      VeilInfoRow(
        icon = MR.images.ic_settings,
        title = stringResource(MR.strings.veil_grp_preferences),
        value = if (groupInfo.isOwner) null else stringResource(MR.strings.veil_grp_preferences_readonly),
        onClick = {
          ModalManager.end.showCustomModal { closePrefs -> VeilGroupPrefsScreen(rhId, chat.id, closePrefs) }
        },
      )
      if (groupInfo.isOwner) {
        VeilInfoRow(
          icon = MR.images.ic_edit,
          title = stringResource(MR.strings.veil_grp_edit_profile),
          onClick = { ModalManager.end.showCustomModal { closeEdit -> VeilGroupProfileScreen(rhId, groupInfo, closeEdit) } },
        )
      }
      if (groupInfo.groupProfile.description != null || groupInfo.isOwner) {
        VeilInfoRow(
          icon = MR.images.ic_chat_bubble,
          title = stringResource(MR.strings.veil_grp_welcome),
          onClick = { ModalManager.end.showCustomModal { closeWelcome -> VeilGroupWelcomeScreen(rhId, groupInfo, closeWelcome) } },
        )
      }
    }

    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      // delivery receipts, disappearing messages timer, local name, support chats, reports, several members at once
      VeilInfoRow(
        icon = MR.images.ic_settings_backup_restore,
        title = stringResource(MR.strings.veil_chatinfo_advanced),
        onClick = { veilShowGroupAdvanced(rhId, groupId) },
      )
    }

    VeilButton(
      stringResource(MR.strings.veil_chatinfo_clear),
      onClick = { clearChatDialog(chat, close) },
      modifier = Modifier.padding(bottom = 10.dp),
      style = VeilButtonStyle.Secondary,
    )
    if (membership.memberCurrentOrPending) {
      VeilButton(
        stringResource(MR.strings.veil_grp_leave),
        onClick = { leaveGroupDialog(rhId, groupInfo, chatModel, close) },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Danger,
      )
    }
    if (groupInfo.canDelete) {
      VeilButton(
        stringResource(MR.strings.veil_grp_delete),
        onClick = { deleteGroupDialog(chat, groupInfo, chatModel, close) },
        style = VeilButtonStyle.Danger,
      )
    }
  }
}

@Composable
internal fun SectionLabel(text: String) {
  BasicText(
    text.uppercase(),
    Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
    style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink3, fontSize = 11.5.sp, letterSpacing = 1.2.sp),
  )
}

/** Name, one line under it and the role chip; clickable when [onClick] is set. */
@Composable
private fun MemberRow(name: String, seed: String, incognito: Boolean, subtitle: String, role: GroupMemberRole, onClick: (() -> Unit)?) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 60.dp)
      .clip(VeilTheme.shapes.input)
      .then(if (onClick != null) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick) else Modifier)
      .padding(horizontal = 6.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    MaskAvatar(seed = seed, size = 42.dp, incognito = incognito, modifier = Modifier.padding(end = if (incognito) 3.dp else 0.dp))
    Column(Modifier.weight(1f)) {
      BasicText(name, style = type.rowTitle.copy(color = colors.ink, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
      BasicText(subtitle, style = type.caption.copy(color = colors.ink2, fontSize = 12.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    RoleChip(role)
  }
}

@Composable
internal fun RoleChip(role: GroupMemberRole) {
  val chip = roleChip(role)
  VeilChip(
    stringResource(roleLabel(chip)),
    style = when (chip) {
      VeilRoleChip.Owner -> VeilChipStyle.Gradient
      VeilRoleChip.Observer -> VeilChipStyle.Dashed
      else -> VeilChipStyle.Default
    },
  )
}

internal fun roleLabel(chip: VeilRoleChip): StringResource = when (chip) {
  VeilRoleChip.Owner -> MR.strings.veil_grp_role_owner
  VeilRoleChip.Admin -> MR.strings.veil_grp_role_admin
  VeilRoleChip.Moderator -> MR.strings.veil_grp_role_moderator
  VeilRoleChip.Member -> MR.strings.veil_grp_role_member
  VeilRoleChip.Observer -> MR.strings.veil_grp_role_observer
  VeilRoleChip.Author -> MR.strings.veil_grp_role_author
  VeilRoleChip.Relay -> MR.strings.veil_grp_role_relay
}

internal fun roleLabel(role: GroupMemberRole): StringResource = roleLabel(roleChip(role))

private fun roleHintLabel(role: GroupMemberRole): StringResource = when (roleHint(role)) {
  VeilRoleHint.Manages -> MR.strings.veil_grp_hint_manages
  VeilRoleHint.Administers -> MR.strings.veil_grp_hint_administers
  VeilRoleHint.Moderates -> MR.strings.veil_grp_hint_moderates
  VeilRoleHint.Writes -> MR.strings.veil_grp_hint_writes
  VeilRoleHint.ReadOnly -> MR.strings.veil_grp_hint_readonly
  VeilRoleHint.Relays -> MR.strings.veil_grp_hint_relays
}

/** Connection trouble and invitations first (upstream's wording), then what the role can do. */
internal fun memberSubtitle(m: GroupMember): String {
  val conn = m.activeConn
  return when {
    m.blocked -> generalGetString(MR.strings.member_info_member_blocked)
    conn?.connStatus is ConnStatus.Failed -> generalGetString(MR.strings.member_info_member_failed)
    conn?.connDisabled == true -> generalGetString(MR.strings.member_info_member_disabled)
    conn?.connInactive == true -> generalGetString(MR.strings.member_info_member_inactive)
    m.memberStatus != GroupMemberStatus.MemComplete && m.memberStatus != GroupMemberStatus.MemCreator && m.memberStatus != GroupMemberStatus.MemConnected -> m.memberStatus.shortText
    else -> generalGetString(roleHintLabel(m.memberRole))
  }
}

// ---- group link ----

@Composable
private fun GroupLinkCard(rhId: Long?, groupInfo: GroupInfo, link: GroupLink?, onLinkChanged: (GroupLink?) -> Unit, showRejoinNote: Boolean) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val clipboard = LocalClipboardManager.current
  var creating by remember { mutableStateOf(false) }
  var deleting by remember { mutableStateOf(false) }
  var copied by remember { mutableStateOf(false) }
  // initial role of the next created link (a link's own role is edited in place)
  var newLinkRole by remember { mutableStateOf(GroupMemberRole.Member) }
  val state = linkCardState(link != null, creating, deleting)
  LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }

  fun create() {
    if (creating) return
    creating = true
    withBGApi {
      val created = chatModel.controller.apiCreateGroupLink(rhId, groupInfo.groupId, newLinkRole)
      if (created != null) {
        withContext(Dispatchers.Main) { onLinkChanged(created) }
      }
      creating = false
    }
  }

  fun delete() {
    deleting = true
    withBGApi {
      val ok = chatModel.controller.apiDeleteGroupLink(rhId, groupInfo.groupId)
      if (ok) withContext(Dispatchers.Main) { onLinkChanged(null) }
      deleting = false
    }
  }

  fun setRole(role: GroupMemberRole) {
    if (link == null || link.acceptMemberRole == role) return
    withBGApi {
      val updated = chatModel.controller.apiGroupLinkMemberRole(rhId, groupInfo.groupId, role)
      if (updated != null) withContext(Dispatchers.Main) { onLinkChanged(updated) }
    }
  }

  fun addShortLink(shareOnCompletion: Boolean) {
    creating = true
    withBGApi {
      val updated = chatModel.controller.apiAddGroupShortLink(rhId, groupInfo.groupId)
      if (updated != null) {
        withContext(Dispatchers.Main) {
          onLinkChanged(updated)
          if (shareOnCompletion) clipboard.shareText(updated.connLinkContact.simplexChatUri(short = true))
        }
      }
      creating = false
    }
  }

  fun upgrade() {
    AlertManager.shared.showAlertDialog(
      title = generalGetString(MR.strings.share_group_profile_via_link),
      text = generalGetString(MR.strings.share_group_profile_via_link_alert_text),
      confirmText = generalGetString(MR.strings.share_profile_via_link_alert_confirm),
      onConfirm = { addShortLink(shareOnCompletion = false) },
    )
  }

  /** Sharing a link that should be upgraded first asks, like upstream's `GroupLinkView`: upgrade and share, share the old one, or cancel. */
  fun share(current: GroupLink, text: String) {
    if (!current.shouldBeUpgraded) {
      clipboard.shareText(text)
      return
    }
    AlertManager.shared.showAlertDialogButtonsColumn(
      title = generalGetString(MR.strings.share_group_profile_via_link),
      text = generalGetString(MR.strings.share_group_profile_via_link_alert_text),
      buttons = {
        VeilAlertButtons(listOf(
          VeilAlertButton(generalGetString(MR.strings.share_profile_via_link_alert_confirm), VeilAlertRole.Primary) {
            AlertManager.shared.hideAlert()
            addShortLink(shareOnCompletion = true)
          },
          VeilAlertButton(generalGetString(MR.strings.share_old_link_alert_button), VeilAlertRole.Secondary) {
            AlertManager.shared.hideAlert()
            clipboard.shareText(text)
          },
          VeilAlertButton(generalGetString(MR.strings.cancel_verb), VeilAlertRole.Secondary) { AlertManager.shared.hideAlert() },
        ))
      },
    )
  }

  VeilCard(Modifier.padding(bottom = 18.dp)) {
    when (state) {
      VeilLinkState.None, VeilLinkState.Creating -> {
        BasicText(
          stringResource(MR.strings.veil_grp_link_none),
          style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
        )
        Spacer(Modifier.height(12.dp))
        BasicText(stringResource(MR.strings.veil_grp_link_role), Modifier.padding(bottom = 6.dp), style = type.caption.copy(color = colors.ink3))
        RoleChoice(linkRoles, newLinkRole, enabled = state == VeilLinkState.None) { newLinkRole = it }
        Spacer(Modifier.height(12.dp))
        VeilButton(
          stringResource(if (state == VeilLinkState.Creating) MR.strings.veil_grp_link_creating else MR.strings.veil_grp_link_new),
          onClick = ::create,
          style = VeilButtonStyle.Secondary,
          enabled = state == VeilLinkState.None,
        )
      }
      VeilLinkState.Active, VeilLinkState.Deleting -> {
        val active = state == VeilLinkState.Active
        val text = link!!.connLinkContact.simplexChatUri(short = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
          GroupLinkQr(link.connLinkContact)
          BasicText(
            text,
            Modifier.weight(1f),
            style = type.caption.copy(color = colors.ink2, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(Modifier.height(14.dp))
        BasicText(stringResource(MR.strings.veil_grp_link_role), Modifier.padding(bottom = 6.dp), style = type.caption.copy(color = colors.ink3))
        RoleChoice(linkRoles, link.acceptMemberRole, enabled = active, onSelect = ::setRole)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          VeilButton(
            stringResource(if (copied) MR.strings.veil_new_copied else MR.strings.veil_new_copy),
            onClick = { clipboard.setText(AnnotatedString(text)); copied = true },
            modifier = Modifier.weight(1f),
            style = VeilButtonStyle.Secondary,
            enabled = active,
            leading = { VeilIcon(MR.images.ic_content_copy, colors.ink, size = 18.dp) },
          )
          VeilButton(
            stringResource(MR.strings.veil_new_share),
            onClick = { share(link, text) },
            modifier = Modifier.weight(1f),
            enabled = active,
          )
        }
        if (link.shouldBeUpgraded) {
          Spacer(Modifier.height(10.dp))
          VeilButton(stringResource(MR.strings.veil_grp_link_upgrade), onClick = ::upgrade, style = VeilButtonStyle.Secondary, enabled = active)
        }
        Spacer(Modifier.height(10.dp))
        VeilButton(
          stringResource(if (deleting) MR.strings.veil_grp_link_deleting else MR.strings.veil_grp_link_delete),
          onClick = { veilShowDeleteLinkSheet(onConfirm = ::delete) },
          style = VeilButtonStyle.Danger,
          enabled = active,
        )
        if (link.acceptMemberRole == GroupMemberRole.Observer) {
          BasicText(
            stringResource(MR.strings.veil_grp_link_observer_note),
            Modifier.padding(top = 10.dp),
            style = type.caption.copy(color = colors.ink3, fontSize = 12.5.sp, lineHeight = 17.sp),
          )
        }
      }
    }
    if (showRejoinNote) {
      Spacer(Modifier.height(12.dp))
      WarnBox(stringResource(MR.strings.veil_grp_rejoin_note))
    }
  }
}

/** Two-way choice (or more) between roles, as chips. */
@Composable
private fun RoleChoice(roles: List<GroupMemberRole>, selected: GroupMemberRole, enabled: Boolean, onSelect: (GroupMemberRole) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    for (r in roles) {
      VeilChip(
        stringResource(roleLabel(r)),
        style = if (r == selected) VeilChipStyle.Gradient else VeilChipStyle.Default,
        onClick = if (enabled) ({ onSelect(r) }) else null,
      )
    }
  }
}

@Composable
internal fun WarnBox(text: String) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.input
  Box(
    Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.warn.copy(alpha = 0.08f))
      .border(1.dp, colors.warn.copy(alpha = 0.40f), shape)
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    BasicText(text, style = VeilTheme.type.body.copy(color = colors.warn, fontSize = 13.5.sp, lineHeight = 19.sp))
  }
}

/**
 * The group link as upstream's QR (`qrCodeBitmap`): dark modules on white with a quiet zone, no logo, no tint and no
 * gradient, so any scanner reads it. Only the frame around it is Veil's.
 */
@Composable
private fun GroupLinkQr(connLink: CreatedConnLink) {
  val colors = VeilTheme.colors
  val content = connLink.simplexChatUri(short = true)
  val bitmap = remember(content) { qrCodeBitmap(content, 1024, if (connLink.connShortLink != null) QrCode.ErrorLevel.M else QrCode.ErrorLevel.L) }
  val frame = VeilTheme.shapes.input
  Box(
    Modifier
      .size(110.dp)
      .clip(frame)
      .background(Color.White)
      .border(1.dp, colors.line2, frame)
      .padding(8.dp)
  ) {
    Image(bitmap, contentDescription = stringResource(MR.strings.veil_grp_link_qr_desc), Modifier.fillMaxSize())
  }
}

/** The amber warning before the link is deleted: removing a member is not a ban while the link stands. */
private fun veilShowDeleteLinkSheet(onConfirm: () -> Unit) {
  veilShowSheet { close ->
    val colors = VeilTheme.colors
    val type = VeilTheme.type
    VeilSheet {
      BasicText(stringResource(MR.strings.veil_grp_link_delete_title), Modifier.padding(bottom = 12.dp), style = type.sheetTitle.copy(color = colors.ink))
      WarnBox(stringResource(MR.strings.veil_grp_link_delete_warning))
      BasicText(
        stringResource(MR.strings.veil_grp_link_delete_members_stay),
        Modifier.padding(top = 12.dp, bottom = 16.dp),
        style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VeilButton(stringResource(MR.strings.veil_grp_cancel), onClick = close, modifier = Modifier.weight(1f), style = VeilButtonStyle.Secondary)
        VeilButton(stringResource(MR.strings.veil_grp_link_delete), onClick = { close(); onConfirm() }, modifier = Modifier.weight(1f), style = VeilButtonStyle.Danger)
      }
    }
  }
}
