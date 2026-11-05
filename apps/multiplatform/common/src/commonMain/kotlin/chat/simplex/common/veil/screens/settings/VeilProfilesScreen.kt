package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.User
import chat.simplex.common.model.UserInfo
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.chatlist.avatarSeed
import chat.simplex.common.views.chatlist.unreadCountStr
import chat.simplex.common.views.helpers.KeyChangeEffect
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * Veil profiles screen. It does what upstream's `UserProfilesView` does and nothing else: switch profile, add profile,
 * hide (password screen), unhide, mute and unmute, delete (with upstream's confirmation and password screens). All of it
 * goes through [userProfilesActions], the code `UserProfilesView` itself uses; only the look is Veil's. The search field
 * is also where the password of a hidden profile is typed, as upstream. [authorize] runs a block after the device
 * credential check (once per opening), the same wrapper the upstream entry used.
 */
@Composable
fun VeilProfilesScreen(authorize: (block: () -> Unit) -> Unit, close: () -> Unit) {
  val m = chatModel
  val search = rememberSaveable { mutableStateOf("") }
  val profileHidden = rememberSaveable { mutableStateOf(false) }
  val actions = remember { userProfilesActions(m, search, profileHidden, { m.users.map { it.user } }, authorize) }
  val query = search.value
  val shown = filteredUsers(m, query)
  val visibleCount = visibleUsersCount(m)
  val running = m.chatRunning.value == true || m.connectedToRemote
  var menuFor by remember { mutableStateOf<User?>(null) }

  Box(Modifier.fillMaxSize()) {
    VeilSettingsScaffold(stringResource(MR.strings.veil_set_row_profiles), close) {
      if (profileHidden.value) {
        VeilGroup {
          VeilValueRow(stringResource(MR.strings.enter_password_to_show), null, readOnly = false, onClick = { profileHidden.value = false })
        }
        Spacer(Modifier.height(12.dp))
      }
      VeilTextField(query, { search.value = it }, placeholder = stringResource(MR.strings.veil_prof_search))
      Spacer(Modifier.height(14.dp))
      VeilGroup {
        shown.forEachIndexed { i, info ->
          if (i > 0) VeilRowDivider()
          ProfileRow(info, running, onOpen = { actions.activateUser(info.user) }, onMenu = { menuFor = info.user })
        }
        if (showAddProfile(query)) {
          if (shown.isNotEmpty()) VeilRowDivider()
          VeilNavRow(MR.images.ic_add, stringResource(MR.strings.users_add), null, onClick = actions.addUser)
        }
      }
      VeilNote(stringResource(MR.strings.veil_prof_hint))
    }
    ProfileMenuSheet(menuFor, visibleCount, actions, onDismiss = { menuFor = null })
  }

  LaunchedEffect(Unit) { showHiddenProfilesNoticeIfNeeded(m.controller.appPrefs.showHiddenProfilesNotice, m.users.size) }
  KeyChangeEffect(remember { m.currentRemoteHost }.value) { ModalManager.start.closeModal() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileRow(info: UserInfo, enabled: Boolean, onOpen: () -> Unit, onMenu: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val u = info.user
  val marks = profileMarks(u.activeUser, u.hidden, u.showNtfs, info.unreadCount)
  val menuLabel = stringResource(MR.strings.veil_prof_menu)
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 68.dp)
      .combinedClickable(
        enabled = enabled,
        role = Role.Button,
        onClick = { if (profileTapSwitches(u.activeUser)) onOpen() },
        onLongClick = onMenu,
      )
      .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    MaskAvatar(avatarSeed("u${u.userId}", u.displayName), 46.dp)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      BasicText(
        u.displayName,
        style = type.rowTitle.copy(color = if (enabled) colors.ink else colors.ink3),
        maxLines = 1, overflow = TextOverflow.Ellipsis
      )
      if (marks.active || marks.locked || marks.muted || marks.unread > 0) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          if (marks.active) VeilChip(stringResource(MR.strings.veil_prof_active), style = VeilChipStyle.Gradient)
          if (marks.unread > 0) {
            VeilChip(unreadCountStr(marks.unread), style = if (marks.unreadMuted) VeilChipStyle.Default else VeilChipStyle.Gradient)
          }
          if (marks.locked) VeilChip(stringResource(MR.strings.veil_prof_hidden))
          if (marks.muted) VeilChip(stringResource(MR.strings.veil_prof_muted))
        }
      }
    }
    VeilIconButton(onClick = onMenu, contentDescription = "$menuLabel: ${u.displayName}", enabled = enabled) {
      VeilIcon(MR.images.ic_more_horiz, colors.ink2, size = 22.dp)
    }
  }
}

private fun menuLabel(a: ProfileMenuAction): StringResource = when (a) {
  ProfileMenuAction.Hide -> MR.strings.user_hide
  ProfileMenuAction.Unhide -> MR.strings.veil_prof_unhide
  ProfileMenuAction.Mute -> MR.strings.user_mute
  ProfileMenuAction.Unmute -> MR.strings.user_unmute
  ProfileMenuAction.Delete -> MR.strings.delete_verb
}

private fun menuIcon(a: ProfileMenuAction): ImageResource = when (a) {
  ProfileMenuAction.Hide -> MR.images.ic_lock
  ProfileMenuAction.Unhide -> MR.images.ic_lock_open_right
  ProfileMenuAction.Mute -> MR.images.ic_notifications_off
  ProfileMenuAction.Unmute -> MR.images.ic_notifications
  ProfileMenuAction.Delete -> MR.images.ic_delete
}

/** Bottom sheet with the actions of one profile; each runs the matching upstream action (which asks for confirmation itself). */
@Composable
private fun ProfileMenuSheet(user: User?, visibleCount: Int, actions: UserProfilesActions, onDismiss: () -> Unit) {
  if (user == null) return
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  BackHandler(onBack = onDismiss)
  val dismissLabel = stringResource(MR.strings.veil_list_sheet_close)
  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier
        .fillMaxSize()
        .background(colors.scrim)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        .semantics { contentDescription = dismissLabel }
    )
    VeilSheet(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().pointerInput(Unit) { detectTapGestures { } }) {
      Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MaskAvatar(avatarSeed("u${user.userId}", user.displayName), 44.dp)
        BasicText(user.displayName, Modifier.weight(1f), style = type.sheetTitle.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      for (action in profileMenuActions(user.hidden, user.showNtfs, visibleCount)) {
        val tint = if (action == ProfileMenuAction.Delete) colors.errText else colors.ink
        Row(
          Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(VeilTheme.shapes.input)
            .clickable(role = Role.Button) {
              onDismiss()
              when (action) {
                ProfileMenuAction.Hide -> actions.showHiddenProfile(user)
                ProfileMenuAction.Unhide -> actions.unhideUser(user)
                ProfileMenuAction.Mute -> actions.muteUser(user)
                ProfileMenuAction.Unmute -> actions.unmuteUser(user)
                ProfileMenuAction.Delete -> actions.removeUser(user)
              }
            }
            .padding(horizontal = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          VeilIcon(menuIcon(action), tint, size = 22.dp)
          BasicText(stringResource(menuLabel(action)), style = type.bodyLarge.copy(color = tint))
        }
      }
    }
  }
}
