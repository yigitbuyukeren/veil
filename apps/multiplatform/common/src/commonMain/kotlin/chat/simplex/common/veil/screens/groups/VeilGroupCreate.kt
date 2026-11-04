package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.VeilScreenScaffold
import chat.simplex.common.veil.screens.chat.chatAvatarSeed
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.chat.group.showProhibitedToInviteIncognitoAlertDialog
import chat.simplex.common.views.chatlist.openGroupChat
import chat.simplex.common.views.chatlist.setGroupMembers
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.isValidDisplayName
import chat.simplex.common.views.mkValidName
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Veil "Grup oluştur" screen: name with a live mask avatar, the random-profile switch and a picker of contacts to invite.
 *
 * Creation is upstream's `AddGroupView` flow: `apiNewGroup`, then the group chat opens. Contacts picked here are invited
 * with `apiAddMember` the way upstream's `AddGroupMembersView` does it. An incognito group takes no contact invitations
 * (upstream creates it with a group link instead): the link is created and the Veil group info opens on it.
 */

/** Opens the create screen over the new-chat sheet. [closeAll] closes the whole new-chat flow. */
fun veilShowGroupCreate(rh: RemoteHostInfo?, closeAll: () -> Unit) {
  ModalManager.start.showCustomModal { close -> VeilGroupCreateScreen(rh?.remoteHostId, close, closeAll) }
}

@Composable
fun VeilGroupCreateScreen(rhId: Long?, close: () -> Unit, closeAll: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val view = LocalMultiplatformView()
  var name by rememberSaveable { mutableStateOf("") }
  var incognito by remember { mutableStateOf(appPrefs.incognito.get()) }
  val selected = remember { mutableStateListOf<Long>() }
  var search by remember { mutableStateOf("") }
  var creating by remember { mutableStateOf(false) }
  val trimmed = name.trim()
  val nameValid = isValidDisplayName(trimmed)
  val canCreate = canCreateGroup(name, ::isValidDisplayName) && !creating
  val userName = chatModel.currentUser.value?.displayName ?: ""
  val contacts = contactsToInvite(search)

  fun create() {
    if (!canCreate) return
    creating = true
    hideKeyboard(view)
    val invite = selected.toList()
    withBGApi {
      val profile = GroupProfile(
        displayName = trimmed,
        fullName = "",
        shortDescr = null,
        image = null,
        groupPreferences = GroupPreferences(history = GroupPreference(GroupFeatureEnabled.ON)),
      )
      val groupInfo = chatModel.controller.apiNewGroup(rhId, incognito, profile)
      if (groupInfo != null) {
        withContext(Dispatchers.Main) {
          chatModel.chatsContext.updateGroup(rhId = rhId, groupInfo)
          openGroupChat(rhId, groupInfo.groupId)
        }
        setGroupMembers(rhId, groupInfo, chatModel)
        closeAll()
        if (groupInfo.incognito) {
          val link = chatModel.controller.apiCreateGroupLink(rhId, groupInfo.groupId)
          veilShowGroupInfo(rhId, groupInfo.groupId, link)
        } else {
          for (contactId in invite) {
            val member = chatModel.controller.apiAddMember(rhId, groupInfo.groupId, contactId, GroupMemberRole.Member)
            if (member == null) break
            withContext(Dispatchers.Main) {
              chatModel.chatsContext.upsertGroupMember(rhId, groupInfo, member)
              chatModel.secondaryChatsContext.value?.upsertGroupMember(rhId, groupInfo, member)
            }
          }
        }
      }
      creating = false
    }
  }

  VeilScreenScaffold(title = stringResource(MR.strings.veil_grp_create_title), onBack = close) {
    Row(
      Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      MaskAvatar(
        seed = chatAvatarSeed("#new", trimmed),
        size = 64.dp,
        incognito = incognito,
        modifier = Modifier.padding(end = if (incognito) 3.dp else 0.dp),
      )
      VeilTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.weight(1f),
        placeholder = stringResource(MR.strings.veil_grp_create_name_hint),
      )
    }
    if (trimmed.isNotEmpty() && !nameValid) {
      BasicText(
        String.format(stringResource(MR.strings.veil_grp_create_name_invalid), mkValidName(trimmed)),
        Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
        style = type.caption.copy(color = colors.errText, fontSize = 12.5.sp),
      )
    }

    // random-profile switch (upstream's incognito preference)
    Row(
      Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .clip(VeilTheme.shapes.card)
        .background(colors.sheet1)
        .border(1.dp, colors.line, VeilTheme.shapes.card)
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      VeilIcon(if (incognito) MR.images.ic_theater_comedy_filled else MR.images.ic_theater_comedy, if (incognito) colors.accent else colors.ink3, size = 24.dp)
      Column(Modifier.weight(1f)) {
        BasicText(stringResource(MR.strings.veil_grp_create_incognito), style = type.rowTitle.copy(color = colors.ink, fontSize = 15.sp))
        BasicText(
          if (incognito) stringResource(MR.strings.veil_grp_create_incognito_on)
          else String.format(stringResource(MR.strings.veil_grp_create_incognito_off), userName),
          style = type.caption.copy(color = colors.ink2, fontSize = 12.5.sp, lineHeight = 17.sp),
        )
      }
      VeilSwitch(incognito, onCheckedChange = { v ->
        incognito = v
        appPrefs.incognito.set(v)
        if (v) selected.clear()
      })
    }

    if (memberPickerAvailable(incognito)) {
      SectionLabel(stringResource(MR.strings.veil_grp_create_pick))
      if (contacts.isEmpty() && search.isEmpty()) {
        BasicText(
          stringResource(MR.strings.veil_grp_create_no_contacts),
          Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
          style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
        )
      } else {
        VeilTextField(search, { search = it }, Modifier.padding(bottom = 8.dp), placeholder = stringResource(MR.strings.veil_grp_search))
        VeilCard(Modifier.padding(bottom = 16.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
          for (c in contacts) {
            val checked = c.contactId in selected
            Row(
              Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(VeilTheme.shapes.input)
                .clickable(role = Role.Checkbox) {
                  // upstream: a contact reached with a random profile cannot be invited to a group made with the main profile
                  if (c.contactConnIncognito) showProhibitedToInviteIncognitoAlertDialog()
                  else if (checked) selected.remove(c.contactId) else selected.add(c.contactId)
                }
                .padding(horizontal = 6.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              MaskAvatar(seed = chatAvatarSeed("@${c.contactId}", c.profile.displayName), size = 38.dp, incognito = c.contactConnIncognito, modifier = Modifier.padding(end = if (c.contactConnIncognito) 3.dp else 0.dp))
              BasicText(
                c.chatViewName,
                Modifier.weight(1f),
                style = type.rowTitle.copy(color = if (c.contactConnIncognito) colors.ink3 else colors.ink, fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (c.contactConnIncognito) VeilIcon(MR.images.ic_theater_comedy_filled, colors.ink3, size = 22.dp)
              else VeilIcon(if (checked) MR.images.ic_check_circle_filled else MR.images.ic_circle, if (checked) colors.mintInk else colors.ink3, size = 24.dp)
            }
          }
        }
      }
    } else {
      BasicText(
        stringResource(MR.strings.veil_grp_create_incognito_link),
        Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
        style = type.body.copy(color = colors.ink2, fontSize = 14.sp),
      )
    }

    VeilButton(
      if (selected.isEmpty()) stringResource(MR.strings.veil_grp_create_button)
      else String.format(stringResource(MR.strings.veil_grp_create_button_with), selected.size),
      onClick = ::create,
      enabled = canCreate,
    )
  }
}

/** Contacts that can be invited right now: upstream's `getContactsToAdd` filters, without the member list of another open group. */
@Composable
private fun contactsToInvite(search: String): List<Contact> {
  val s = search.trim().lowercase()
  return chatModel.chats.value
    .asSequence()
    .map { it.chatInfo }
    .filterIsInstance<ChatInfo.Direct>()
    .filter { it.sendMsgEnabled }
    .map { it.contact }
    .filter { c -> !c.sendMsgToConnect && c.anyNameContains(s) }
    .sortedBy { it.displayName.lowercase() }
    .toList()
}
