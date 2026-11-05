package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.VeilScreenScaffold
import chat.simplex.common.veil.screens.chat.chatAvatarSeed
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.chat.group.getContactsToAdd
import chat.simplex.common.views.chat.group.showProhibitedToInviteIncognitoAlertDialog
import chat.simplex.common.views.chatlist.setGroupMembers
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Veil "Üye ekle" page: replaces upstream's `AddGroupMembersView` (opened by `addGroupMembers`) for a group that already exists.
 *
 * What upstream's screen does, and where it is now:
 * - the contacts that can be invited: `getContactsToAdd` (upstream's own filter: connected, not already a member), with a search.
 * - the role of the new members: the selectable roles up to the user's own ([invitableRoles]).
 * - a contact reached with a random profile cannot be invited from a group made with the main profile: upstream's alert
 *   (`showProhibitedToInviteIncognitoAlertDialog`).
 * - invite: `apiAddMember` for each ticked contact in order, stopping at the first failure, then `upsertGroupMember` in both
 *   chat contexts, run as a long call (`withLongRunningApi`) like upstream; the page closes when done.
 * - the page closes when another chat is opened (`KeyChangeEffect` on the open chat id).
 * - not here: the "member admission" and "preferences" rows, which upstream shows only while a new group is being created
 *   (Veil's own create screen does that step).
 */
fun veilShowAddMembers(rhId: Long?, groupInfo: GroupInfo) {
  withBGApi {
    setGroupMembers(rhId, groupInfo, chatModel)
    ModalManager.end.showCustomModal { close -> VeilAddMembersScreen(rhId, groupInfo.groupId, close) }
  }
}

@Composable
fun VeilAddMembersScreen(rhId: Long?, groupId: Long, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val groupInfo = remember(groupId) { derivedStateOf { (chatModel.getGroupChat(groupId)?.chatInfo as? ChatInfo.Group)?.groupInfo } }.value
  if (groupInfo == null) {
    LaunchedEffect(Unit) { close() }
    return
  }
  val view = LocalMultiplatformView()
  LaunchedEffect(Unit) { hideKeyboard(view) }
  KeyChangeEffect(chatModel.chatId.value) { close() }

  val selected: SnapshotStateList<Long> = remember { mutableStateListOf() }
  val roles = remember(groupInfo.membership.memberRole) { invitableRoles(groupInfo.membership.memberRole) }
  var role by remember { mutableStateOf(GroupMemberRole.Member) }
  var search by remember { mutableStateOf("") }
  var inviting by remember { mutableStateOf(false) }
  val contacts = getContactsToAdd(chatModel, search)

  fun invite() {
    if (!inviteEnabled(selected.size, inviting)) return
    inviting = true
    val ids = selected.toList()
    val newRole = role
    withLongRunningApi(slow = 120_000) {
      for (contactId in ids) {
        val member = chatModel.controller.apiAddMember(rhId, groupInfo.groupId, contactId, newRole)
        if (member != null) {
          withContext(Dispatchers.Main) { chatModel.chatsContext.upsertGroupMember(rhId, groupInfo, member) }
          withContext(Dispatchers.Main) { chatModel.secondaryChatsContext.value?.upsertGroupMember(rhId, groupInfo, member) }
        } else {
          break
        }
      }
      close()
    }
  }

  VeilScreenScaffold(title = stringResource(MR.strings.veil_grp_add_members), onBack = close, subtitle = groupInfo.displayName) {
    BasicText(
      stringResource(MR.strings.veil_gadd_profile_note),
      Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
      style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
    )

    if (contacts.isEmpty() && search.isEmpty()) {
      BasicText(
        stringResource(MR.strings.veil_gadd_none),
        Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
        style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
      )
      return@VeilScreenScaffold
    }

    SectionLabel(stringResource(MR.strings.veil_gadd_role))
    Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      for (r in roles) {
        VeilChip(
          stringResource(roleLabel(r)),
          style = if (r == role) VeilChipStyle.Gradient else VeilChipStyle.Default,
          onClick = if (inviting) null else ({ role = r }),
        )
      }
    }

    VeilButton(
      if (inviting) stringResource(MR.strings.veil_gadd_inviting) else stringResource(MR.strings.veil_gadd_invite),
      onClick = ::invite,
      modifier = Modifier.padding(bottom = 8.dp),
      enabled = inviteEnabled(selected.size, inviting),
    )
    Row(
      Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BasicText(
        if (selected.isEmpty()) stringResource(MR.strings.veil_gadd_none_selected)
        else String.format(stringResource(MR.strings.veil_gadd_selected), selected.size),
        style = type.caption.copy(color = colors.ink2, fontSize = 13.sp),
      )
      if (selected.isNotEmpty()) {
        BasicText(
          stringResource(MR.strings.veil_gadd_clear),
          Modifier
            .clip(VeilTheme.shapes.input)
            .clickable(enabled = !inviting, role = Role.Button) { selected.clear() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .alpha(if (inviting) 0.38f else 1f),
          style = type.caption.copy(color = colors.mintInk, fontSize = 13.sp),
        )
      }
    }

    SectionLabel(stringResource(MR.strings.veil_grp_create_pick))
    VeilTextField(search, { search = it }, Modifier.padding(bottom = 8.dp), placeholder = stringResource(MR.strings.veil_grp_search))
    VeilCard(Modifier.padding(bottom = 16.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
      for (c in contacts) {
        val checked = c.contactId in selected
        val prohibited = prohibitedToInviteIncognito(groupInfo.membership.memberIncognito, c.contactConnIncognito)
        Row(
          Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(VeilTheme.shapes.input)
            .then(
              if (inviting) Modifier
              else Modifier.clickable(role = Role.Checkbox) {
                if (prohibited) showProhibitedToInviteIncognitoAlertDialog()
                else {
                  val next = toggleContact(selected.toList(), c.contactId)
                  selected.clear()
                  selected.addAll(next)
                }
              }
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          MaskAvatar(
            seed = chatAvatarSeed("@${c.contactId}", c.profile.displayName),
            size = 38.dp,
            incognito = c.contactConnIncognito,
            modifier = Modifier.padding(end = if (c.contactConnIncognito) 3.dp else 0.dp),
          )
          BasicText(
            c.chatViewName,
            Modifier.weight(1f),
            style = type.rowTitle.copy(color = if (prohibited) colors.ink3 else colors.ink, fontSize = 15.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (prohibited) VeilIcon(MR.images.ic_theater_comedy_filled, colors.ink3, size = 22.dp)
          else VeilIcon(if (checked) MR.images.ic_check_circle_filled else MR.images.ic_circle, if (checked) colors.mintInk else colors.ink3, size = 24.dp)
        }
      }
    }
  }
}
