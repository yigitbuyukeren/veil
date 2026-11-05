package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.avatarSeed
import chat.simplex.common.veil.screens.settings.*
import chat.simplex.common.views.bioFitsLimit
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.isValidDisplayName
import chat.simplex.common.views.mkValidName
import chat.simplex.common.views.showInvalidNameAlert
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Veil group profile editor (replaces upstream's `GroupProfileView`). Same actions: name, full name (only when the group
 * has one), short description, photo from camera or gallery (upstream's picker sheet, cropped square and shrunk with
 * `cropToSquare` / `resizeImageToStrSize`), remove photo, and `apiUpdateGroup` + `updateGroup` on save. Leaving with a
 * valid, unsaved edit asks with upstream's dialog. Rules are in `VeilGroupProfileLogic`.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VeilGroupProfileScreen(rhId: Long?, groupInfo: GroupInfo, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val profile = groupInfo.groupProfile
  val isChannel = groupInfo.useRelays
  val displayName = rememberSaveable { mutableStateOf(profile.displayName) }
  val fullName = rememberSaveable { mutableStateOf(profile.fullName) }
  val shortDescr = rememberSaveable { mutableStateOf(profile.shortDescr ?: "") }
  val profileImage = rememberSaveable { mutableStateOf(profile.image) }
  val chosenImage = rememberSaveable { mutableStateOf<URI?>(null) }
  val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()
  val draft = VeilGroupProfileDraft(displayName.value, fullName.value, shortDescr.value, profileImage.value)
  val nameValid = isValidNewName(displayName.value, profile, ::isValidDisplayName)
  val bioOk = bioFitsLimit(shortDescr.value)
  val canSave = canSaveProfile(draft, profile, ::isValidDisplayName, ::bioFitsLimit)

  fun save() {
    val p = applyDraft(profile, draft)
    withBGApi {
      val g = chatModel.controller.apiUpdateGroup(rhId, groupInfo.groupId, p, groupInfo.useRelays)
      if (g != null) {
        withContext(Dispatchers.Main) { chatModel.chatsContext.updateGroup(rhId, g) }
        close()
      }
    }
  }
  val tryClose = {
    when (profileCloseAction(draft, profile, ::isValidDisplayName, ::bioFitsLimit)) {
      VeilProfileClose.Close -> close()
      VeilProfileClose.AskToSave -> AlertManager.shared.showAlertDialogStacked(
        title = generalGetString(MR.strings.save_preferences_question),
        confirmText = generalGetString(if (isChannel) MR.strings.save_and_notify_channel_subscribers else MR.strings.save_and_notify_group_members),
        dismissText = generalGetString(MR.strings.exit_without_saving),
        onConfirm = ::save,
        onDismiss = close,
      )
    }
  }

  ModalBottomSheetLayout(
    scrimColor = colors.scrim,
    modifier = Modifier.imePadding(),
    sheetContent = {
      GetImageBottomSheet(
        chosenImage,
        onImageChange = { bitmap -> profileImage.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
        hideBottomSheet = { scope.launch { sheetState.hide() } },
      )
    },
    sheetState = sheetState,
    sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    sheetBackgroundColor = colors.sheetBase,
  ) {
    VeilSettingsScaffold(stringResource(MR.strings.veil_grp_edit_profile), tryClose) {
      VeilNote(stringResource(if (isChannel) MR.strings.channel_profile_is_stored_on_subscribers_devices else MR.strings.group_profile_is_stored_on_members_devices))

      Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val image = profileImage.value
        if (image != null) {
          ProfileImage(108.dp, image)
        } else {
          MaskAvatar(avatarSeed("g${groupInfo.groupId}", profile.displayName), 108.dp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          VeilButton(
            stringResource(MR.strings.edit_image),
            onClick = { scope.launch { sheetState.show() } },
            style = VeilButtonStyle.Secondary,
            fillMaxWidth = false,
          )
          if (image != null) {
            VeilButton(
              stringResource(MR.strings.delete_image),
              onClick = { profileImage.value = null },
              style = VeilButtonStyle.Secondary,
              fillMaxWidth = false,
            )
          }
        }
      }

      FieldLabel(stringResource(if (isChannel) MR.strings.channel_display_name_field else MR.strings.veil_gprof_name))
      VeilTextField(displayName.value, { displayName.value = it })
      if (!nameValid) {
        BasicText(
          stringResource(MR.strings.invalid_name),
          Modifier
            .clip(VeilTheme.shapes.input)
            .clickable(role = Role.Button) { showInvalidNameAlert(mkValidName(displayName.value), displayName) }
            .heightIn(min = 48.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 6.dp),
          style = type.body.copy(color = colors.errText),
        )
      }

      if (showFullNameField(profile)) {
        Spacer(Modifier.height(14.dp))
        FieldLabel(stringResource(MR.strings.veil_gprof_fullname))
        VeilTextField(fullName.value, { fullName.value = it })
      }

      Spacer(Modifier.height(14.dp))
      FieldLabel(stringResource(MR.strings.veil_gprof_descr))
      VeilTextField(shortDescr.value, { shortDescr.value = it })
      if (!bioOk) {
        BasicText(
          stringResource(MR.strings.group_descr_too_large),
          Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
          style = type.body.copy(color = colors.errText),
        )
      }

      Spacer(Modifier.height(22.dp))
      VeilButton(
        stringResource(if (isChannel) MR.strings.save_channel_profile else MR.strings.save_group_profile),
        onClick = ::save,
        enabled = canSave,
      )
    }
  }
}

@Composable
private fun FieldLabel(text: String) {
  BasicText(text, Modifier.padding(start = 4.dp, bottom = 6.dp), style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink3))
}
