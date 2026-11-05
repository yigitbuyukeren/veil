package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.veilShowSheet
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.settings.*
import chat.simplex.common.views.chat.group.MemberAdmissionView
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Veil group preferences (replaces upstream's `GroupPreferencesView` where the Veil group info opens it). Same actions:
 * the owner flips features, picks the role a feature is limited to and the disappearing time (upstream's custom time
 * dialog for other values), resets, or saves with `apiUpdateGroup` and `updateGroup`; leaving with unsaved edits asks
 * with upstream's dialog; everybody else reads the values. The member admission entry opens upstream's
 * `MemberAdmissionView`. Rules are in `VeilGroupPrefsLogic`.
 */
@Composable
fun VeilGroupPrefsScreen(rhId: Long?, chatId: String, close: () -> Unit) {
  val m = chatModel
  val groupInfo = remember { derivedStateOf {
    val ch = m.getChat(chatId)
    val g = (ch?.chatInfo as? ChatInfo.Group)?.groupInfo
    if (g == null || ch.remoteHostId != rhId) null else g
  } }
  val gInfo = groupInfo.value
  if (gInfo == null) {
    // the group was deleted while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  var preferences by rememberSaveable(gInfo, stateSaver = serializableSaver()) { mutableStateOf(gInfo.fullGroupPreferences) }
  var currentPreferences by rememberSaveable(gInfo, stateSaver = serializableSaver()) { mutableStateOf(preferences) }

  fun savePrefs(afterSave: () -> Unit = {}) {
    withBGApi {
      val gp = gInfo.groupProfile.copy(groupPreferences = preferences.toGroupPreferences())
      val g = m.controller.apiUpdateGroup(rhId, gInfo.groupId, gp, gInfo.useRelays)
      if (g != null) {
        withContext(Dispatchers.Main) {
          m.chatsContext.updateGroup(rhId, g)
          currentPreferences = preferences
        }
      }
      afterSave()
    }
  }
  val saveTextId = if (gInfo.useRelays) MR.strings.save_and_notify_channel_subscribers else MR.strings.save_and_notify_group_members
  val tryClose = {
    when (prefsCloseAction(preferences, currentPreferences)) {
      VeilPrefsClose.Close -> close()
      VeilPrefsClose.AskToSave -> AlertManager.shared.showAlertDialogStacked(
        title = generalGetString(MR.strings.save_preferences_question),
        confirmText = generalGetString(saveTextId),
        dismissText = generalGetString(MR.strings.exit_without_saving),
        onConfirm = { savePrefs(close) },
        onDismiss = close,
      )
    }
  }
  val title = when (prefsTitle(gInfo.useRelays, gInfo.businessChat != null)) {
    VeilPrefsTitle.Channel -> MR.strings.channel_preferences
    VeilPrefsTitle.Group -> MR.strings.group_preferences
    VeilPrefsTitle.Chat -> MR.strings.chat_preferences
  }

  VeilSettingsScaffold(stringResource(title), tryClose) {
    if (showMemberAdmission(gInfo.useRelays, gInfo.businessChat != null)) {
      VeilGroup {
        VeilNavRow(
          MR.images.ic_toggle_on,
          stringResource(MR.strings.member_admission),
          null,
          onClick = { ModalManager.end.showCustomModal { closeAdmission -> MemberAdmissionView(m, rhId, chatId, closeAdmission) } },
        )
      }
      Spacer(Modifier.height(14.dp))
    }
    if (!gInfo.isOwner) {
      VeilNote(stringResource(MR.strings.veil_grp_preferences_readonly))
    }
    for (row in prefRows(gInfo.useRelays)) {
      PrefCard(
        row, gInfo, preferences,
        onToggle = { on ->
          val before = preferences
          preferences = toggled(before, currentPreferences, row.feature, on)
          if (on && row.relayNote && row.feature == GroupFeature.Support) {
            // as upstream: enabling chats with admins in a channel asks first; refusing switches it back off
            val revert = { preferences = toggled(before, currentPreferences, row.feature, false) }
            AlertManager.shared.showAlertDialog(
              title = generalGetString(MR.strings.enable_chats_with_admins_question),
              text = generalGetString(MR.strings.chat_with_admins_relay_note),
              confirmText = generalGetString(MR.strings.enable_chats_with_admins),
              destructive = true,
              onDismiss = revert,
              onDismissRequest = revert,
            )
          }
        },
        onRole = { role -> preferences = withRole(preferences, row.feature, role) },
        onTtl = { ttl -> preferences = withTtl(preferences, ttl) },
      )
      Spacer(Modifier.height(14.dp))
    }
    if (gInfo.isOwner) {
      val changed = hasUnsavedPrefs(preferences, currentPreferences)
      VeilButton(
        stringResource(saveTextId),
        onClick = { savePrefs() },
        modifier = Modifier.padding(bottom = 10.dp),
        enabled = changed,
      )
      VeilButton(
        stringResource(MR.strings.reset_verb),
        onClick = { preferences = currentPreferences },
        style = VeilButtonStyle.Secondary,
        enabled = changed,
      )
    }
  }
}

@Composable
private fun PrefCard(
  row: VeilPrefRow,
  gInfo: GroupInfo,
  preferences: FullGroupPreferences,
  onToggle: (Boolean) -> Unit,
  onRole: (GroupMemberRole?) -> Unit,
  onTtl: (Int?) -> Unit,
) {
  val f = row.feature
  val enabled = enabledOf(preferences, f)
  val on = enabled == GroupFeatureEnabled.ON
  val name = f.text(gInfo.isChannel)
  VeilGroup {
    if (gInfo.isOwner) {
      VeilSwitchRow(name, checked = on, onChange = onToggle, enabled = !row.disabled)
    } else {
      VeilValueRow(name, null, readOnly = false, value = enabled.text)
    }
    if (f == GroupFeature.TimedMessages && showTtlRow(preferences)) {
      VeilRowDivider()
      val ttl = preferences.timedMessages.ttl
      VeilValueRow(
        generalGetString(MR.strings.delete_after),
        null,
        readOnly = false,
        value = timeText(ttl),
        onClick = if (gInfo.isOwner) ({ showTtlSheet(ttl, onTtl) }) else null,
      )
    }
    if (showRoleRow(preferences, f)) {
      VeilRowDivider()
      val role = roleOf(preferences, f)
      VeilValueRow(
        generalGetString(MR.strings.feature_enabled_for),
        null,
        readOnly = false,
        value = roleName(role),
        onClick = if (gInfo.isOwner) ({ showRoleSheet(role, onRole) }) else null,
      )
    }
  }
  VeilNote(f.enableDescription(enabled, gInfo.isOwner, gInfo.isChannel))
  if (row.relayNote) VeilNote(generalGetString(MR.strings.chat_with_admins_relay_note))
}

private fun roleName(role: GroupMemberRole?): String = generalGetString(
  when (role) {
    null -> MR.strings.feature_roles_all_members
    GroupMemberRole.Moderator -> MR.strings.feature_roles_moderators
    GroupMemberRole.Admin -> MR.strings.feature_roles_admins
    else -> MR.strings.feature_roles_owners
  }
)

private fun showRoleSheet(selected: GroupMemberRole?, onPick: (GroupMemberRole?) -> Unit) {
  veilShowChoiceSheet(
    title = generalGetString(MR.strings.feature_enabled_for),
    labels = featureRoles.map { roleName(it) },
    selected = featureRoles.indexOf(selected),
    onPick = { onPick(featureRoles[it]) },
  )
}

/** Upstream's fixed times, the current one if it is not among them, and "custom" which opens upstream's time dialog. */
private fun showTtlSheet(selected: Int?, onPick: (Int?) -> Unit) {
  val options = ttlOptions(selected)
  val custom = generalGetString(MR.strings.veil_gpref_custom_time)
  veilShowChoiceSheet(
    title = generalGetString(MR.strings.delete_after),
    labels = options.map { timeText(it) } + custom,
    selected = options.indexOf(selected),
    onPick = { i ->
      if (i < options.size) onPick(options[i])
      else showCustomTimePickerDialog(
        selection = mutableStateOf(selected ?: DEFAULT_GROUP_TTL_SECONDS),
        title = generalGetString(MR.strings.delete_after),
        confirmButtonText = generalGetString(MR.strings.custom_time_picker_select),
        confirmButtonAction = { onPick(it) },
        cancel = {},
      )
    },
  )
}

/** A bottom sheet with one tappable row per choice; the picked row is marked. The sheet closes before [onPick] runs. */
internal fun veilShowChoiceSheet(title: String, labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
  veilShowSheet { close ->
    val colors = VeilTheme.colors
    val type = VeilTheme.type
    VeilSheet {
      BasicText(title, Modifier.padding(bottom = 10.dp), style = type.sheetTitle.copy(color = colors.ink))
      Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
        labels.forEachIndexed { i, label ->
          Row(
            Modifier
              .fillMaxWidth()
              .heightIn(min = 52.dp)
              .clip(VeilTheme.shapes.input)
              .clickable(role = Role.Button) { close(); onPick(i) }
              .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            BasicText(label, Modifier.weight(1f), style = type.bodyLarge.copy(color = colors.ink))
            if (i == selected) VeilIcon(MR.images.ic_check, colors.accent, size = 22.dp)
          }
        }
      }
    }
  }
}
