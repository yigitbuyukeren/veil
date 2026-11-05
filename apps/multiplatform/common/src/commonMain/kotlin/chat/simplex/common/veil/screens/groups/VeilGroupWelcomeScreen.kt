package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.platform.chatJsonLength
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.settings.*
import chat.simplex.common.views.chat.item.MarkdownText
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Veil welcome message editor (replaces upstream's `GroupWelcomeView`). Same actions: edit (owner of a normal group
 * only), preview with upstream's markdown renderer, copy, and `apiUpdateGroup` + `updateGroup` on save. Leaving with
 * unsaved text asks with upstream's dialog, or says the text is too long. Rules are in `VeilGroupWelcomeLogic`.
 */
@Composable
fun VeilGroupWelcomeScreen(rhId: Long?, groupInfo: GroupInfo, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  var gInfo by remember { mutableStateOf(groupInfo) }
  val welcomeText = rememberSaveable { mutableStateOf(gInfo.groupProfile.description ?: "") }
  var editing by remember { mutableStateOf(true) }
  val clipboard = LocalClipboardManager.current
  val canEdit = canEditWelcome(gInfo.isOwner, gInfo.businessChat?.chatType != null)
  val unchanged = welcomeUnchanged(welcomeText.value, gInfo.groupProfile.description)
  val fits = welcomeFits(chatJsonLength(welcomeText.value))

  fun save(afterSave: () -> Unit = {}) {
    withBGApi {
      val welcome = welcomeToSave(welcomeText.value)
      val updated = gInfo.groupProfile.copy(description = welcome)
      val res = chatModel.controller.apiUpdateGroup(rhId, gInfo.groupId, updated, gInfo.useRelays)
      if (res != null) {
        gInfo = res
        withContext(Dispatchers.Main) { chatModel.chatsContext.updateGroup(rhId, res) }
        welcomeText.value = welcome ?: ""
      }
      afterSave()
    }
  }
  val tryClose = {
    when (welcomeCloseAction(unchanged, fits)) {
      VeilWelcomeClose.Close -> close()
      VeilWelcomeClose.TooLong -> AlertManager.shared.showAlertDialogStacked(
        title = generalGetString(MR.strings.welcome_message_is_too_long),
        confirmText = generalGetString(MR.strings.exit_without_saving),
        onConfirm = close,
      )
      VeilWelcomeClose.AskToSave -> AlertManager.shared.showAlertDialogStacked(
        title = generalGetString(MR.strings.save_welcome_message_question),
        confirmText = generalGetString(MR.strings.save_and_update_group_profile),
        dismissText = generalGetString(MR.strings.exit_without_saving),
        onConfirm = { save(close) },
        onDismiss = close,
      )
    }
  }

  VeilSettingsScaffold(stringResource(MR.strings.group_welcome_title), tryClose) {
    if (canEdit) {
      if (editing) {
        VeilTextField(
          welcomeText.value,
          { welcomeText.value = it },
          Modifier.heightIn(min = 140.dp),
          placeholder = stringResource(MR.strings.enter_welcome_message),
          singleLine = false,
        )
      } else {
        WelcomePreview(welcomeText.value)
      }
      if (!fits) {
        BasicText(
          stringResource(MR.strings.message_too_large),
          Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
          style = type.body.copy(color = colors.errText),
        )
      } else {
        VeilNote(stringResource(MR.strings.veil_gwel_hint))
      }
      Spacer(Modifier.height(8.dp))
      VeilButton(
        stringResource(if (editing) MR.strings.group_welcome_preview else MR.strings.edit_verb),
        onClick = { editing = !editing },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Secondary,
        enabled = canTogglePreview(welcomeText.value),
      )
      VeilButton(
        stringResource(MR.strings.copy_verb),
        onClick = { clipboard.setText(AnnotatedString(welcomeText.value)) },
        modifier = Modifier.padding(bottom = 10.dp),
        style = VeilButtonStyle.Secondary,
      )
      VeilButton(
        stringResource(MR.strings.save_and_update_group_profile),
        onClick = { save() },
        enabled = canSaveWelcome(unchanged, fits),
      )
    } else {
      WelcomePreview(welcomeText.value)
      Spacer(Modifier.height(14.dp))
      VeilButton(
        stringResource(MR.strings.copy_verb),
        onClick = { clipboard.setText(AnnotatedString(welcomeText.value)) },
        style = VeilButtonStyle.Secondary,
      )
    }
  }
}

/** The message as members see it: upstream's markdown rendering inside a Veil card. */
@Composable
private fun WelcomePreview(text: String) {
  val uriHandler = LocalUriHandler.current
  val linkMode = chatModel.controller.appPrefs.simplexLinkMode.get()
  VeilCard(Modifier.fillMaxWidth()) {
    SelectionContainer(Modifier.fillMaxWidth()) {
      MarkdownText(
        text,
        formattedText = remember(text) { parseToMarkdown(text) },
        toggleSecrets = false,
        modifier = Modifier.fillMaxWidth(),
        linkMode = linkMode, uriHandler = uriHandler,
        style = VeilTheme.type.bodyLarge.copy(color = VeilTheme.colors.ink),
      )
    }
  }
}
