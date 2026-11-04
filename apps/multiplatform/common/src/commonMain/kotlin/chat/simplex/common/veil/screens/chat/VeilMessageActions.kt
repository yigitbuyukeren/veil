package chat.simplex.common.veil.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatModel.controller
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chat.item.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * Everything upstream's item menu has in scope in `ChatItemView` and the Veil sheet needs to run the same actions.
 * Built at the long-press site and handed to [VeilChatHooks.onMessageActions].
 */
class VeilMessageEnv(
  val chatsCtx: ChatModel.ChatsContext,
  val rhId: Long?,
  val chat: Chat,
  val composeState: MutableState<ComposeState>,
  val useLinkPreviews: Boolean,
  val revealed: State<Boolean>,
  val range: State<IntRange?>,
  val selectedChatItems: MutableState<Set<Long>?>,
  val live: Boolean,
  val preview: Boolean,
  val developerTools: Boolean,
  val selectChatItem: () -> Unit,
  val deleteMessage: (Long, CIDeleteMode) -> Unit,
  val deleteMessages: (List<Long>) -> Unit,
  val archiveReports: (List<Long>, Boolean) -> Unit,
  val cancelFile: (Long) -> Unit,
  val forwardItem: (ChatInfo, ChatItem) -> Unit,
  val setReaction: (ChatInfo, ChatItem, Boolean, MsgReaction) -> Unit,
  val showItemDetails: (ChatInfo, ChatItem) -> Unit,
  val reveal: (Boolean) -> Unit,
  /** Launcher for "Kaydet" of a file, created in the item's own composition so it outlives the sheet. */
  val saveLauncher: FileChooserLauncher?,
)

/** The reactions upstream offers for [item]: the supported ones that the user has not used on it yet. */
fun veilReactionChoices(item: ChatItem): List<MsgReaction> =
  MsgReaction.supported.filter { r -> item.reactions.none { it.userReacted && it.reaction.text == r.text } }

private fun VeilMessageEnv.facts(item: ChatItem): VeilActionFacts {
  val file = item.file
  val fileLoaded = file != null &&
      (getLoadedFilePath(file) != null || (chatModel.connectedToRemote() && CIFile.cachedRemoteFileRequests[file.fileSource] != false && file.loaded))
  val fileDownloadable = file != null && file.fileStatus is CIFileStatus.RcvInvitation && fileSizeValid(file, ciSenderProfile(item, chat.chatInfo))
  return VeilActionFacts(
    revealed = revealed.value,
    live = live,
    preview = preview,
    inRange = range.value != null,
    rangeCount = range.value?.count() ?: 0,
    selectionActive = selectedChatItems.value != null,
    developerTools = developerTools,
    fileLoaded = fileLoaded,
    fileDownloadable = fileDownloadable,
    reactionsLeft = veilReactionChoices(item).isNotEmpty(),
  )
}

/**
 * Long-press on a message ([VeilChatHooks.onMessageActions] default): opens the Veil sheet, or does nothing when
 * upstream would show no menu either.
 */
fun veilShowMessageActions(item: ChatItem, env: VeilMessageEnv, @Suppress("UNUSED_PARAMETER") openUpstreamMenu: () -> Unit) {
  val chatInfo = env.chat.chatInfo
  val actions = veilMessageActions(item, chatInfo, env.facts(item))
  if (actions.isEmpty()) return
  veilShowSheet { close -> VeilMessageSheet(item, env, actions, close) }
}

/**
 * Shows [content] as a bottom sheet over the screen, using the app's modal stack (unanimated: the sheet slides itself
 * in). Android only; the callers are the Veil screens, which are Android only.
 */
fun veilShowSheet(content: @Composable (close: () -> Unit) -> Unit) {
  ModalManager.end.showCustomModal(animated = false) { close ->
    VeilSheetHost(close, content)
  }
}

@Composable
private fun VeilSheetHost(close: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
  val motion = VeilTheme.motion
  val ms = motion.lift(VeilTheme.reduceMotion)
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }
  BackHandler(onBack = close)
  Box(Modifier.fillMaxSize()) {
    AnimatedVisibility(shown, enter = fadeIn(tween(ms)), exit = fadeOut(tween(ms))) {
      val dismissLabel = stringResource(MR.strings.veil_msgact_close)
      Box(
        Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f))
          .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
          .semantics { contentDescription = dismissLabel }
      )
    }
    AnimatedVisibility(
      shown,
      Modifier.align(Alignment.BottomCenter),
      enter = slideInVertically(tween(ms, easing = motion.easeOut)) { it },
      exit = slideOutVertically(tween(ms, easing = motion.easeOut)) { it },
    ) {
      Box(Modifier.navigationBarsPadding().pointerInput(Unit) { detectTapGestures { } }) {
        content(close)
      }
    }
  }
}

@Composable
private fun VeilMessageSheet(item: ChatItem, env: VeilMessageEnv, actions: List<VeilMsgAction>, close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val clipboard = LocalClipboardManager.current
  var reactionsOpen by remember { mutableStateOf(false) }
  val previewText = item.text.trim()
  VeilSheet {
    Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
      if (previewText.isNotEmpty()) {
        Box(
          Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(VeilTheme.shapes.input)
            .background(colors.sheet1)
            .border(1.dp, colors.line, VeilTheme.shapes.input)
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          BasicText(previewText, style = type.body.copy(color = colors.ink2, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
      AnimatedVisibility(reactionsOpen, enter = fadeIn(tween(VeilTheme.motion.lift(VeilTheme.reduceMotion))) + expandVertically()) {
        val choices = remember(item) { veilReactionChoices(item) }
        Row(
          Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShapeReactions)
            .background(colors.sheet2)
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          for (r in choices) {
            Box(
              Modifier
                .size(46.dp)
                .clip(VeilTheme.shapes.input)
                .clickable(role = Role.Button) {
                  close()
                  env.setReaction(env.chat.chatInfo, item, true, r)
                }
                .semantics { contentDescription = r.text },
              contentAlignment = Alignment.Center
            ) { BasicText(r.text, style = type.body.copy(fontSize = 26.sp)) }
          }
        }
      }
      for (action in actions) {
        val label = veilActionLabel(action, item)
        val tint = if (action.danger) colors.errText else colors.ink
        Row(
          Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(VeilTheme.shapes.input)
            .clickable(role = Role.Button) {
              if (action == VeilMsgAction.React) {
                reactionsOpen = true
              } else {
                close()
                runVeilMessageAction(action, item, env, clipboard)
              }
            }
            .padding(horizontal = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          VeilIcon(veilActionIcon(action), tint, size = 22.dp)
          BasicText(stringResource(label), style = type.bodyLarge.copy(color = tint))
        }
      }
    }
  }
}

private val RoundedCornerShapeReactions = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)

private fun veilActionLabel(action: VeilMsgAction, item: ChatItem): StringResource = when (action) {
  VeilMsgAction.Reply -> MR.strings.veil_msgact_reply
  VeilMsgAction.Edit -> MR.strings.veil_msgact_edit
  VeilMsgAction.Forward -> MR.strings.veil_msgact_forward
  VeilMsgAction.React -> MR.strings.veil_msgact_react
  VeilMsgAction.Copy -> MR.strings.veil_msgact_copy
  VeilMsgAction.Share -> MR.strings.veil_msgact_share
  VeilMsgAction.Save -> MR.strings.veil_msgact_save
  VeilMsgAction.Download -> MR.strings.veil_msgact_download
  VeilMsgAction.Info -> MR.strings.veil_msgact_info
  VeilMsgAction.Select -> MR.strings.veil_msgact_select
  VeilMsgAction.Hide -> MR.strings.veil_msgact_hide
  VeilMsgAction.Reveal -> MR.strings.veil_msgact_reveal
  VeilMsgAction.Expand -> MR.strings.veil_msgact_expand
  VeilMsgAction.Shrink -> MR.strings.veil_msgact_hide
  VeilMsgAction.ArchiveReport -> MR.strings.veil_msgact_archive_report
  // upstream's own wording: cancel upload / stop receiving / revoke, depending on the file's state
  VeilMsgAction.CancelFile -> item.file?.cancelAction?.uiActionId ?: MR.strings.veil_msgact_cancel_file
  VeilMsgAction.Delete -> MR.strings.veil_msgact_delete
  VeilMsgAction.DeleteReport -> MR.strings.veil_msgact_delete_report
  VeilMsgAction.Moderate -> MR.strings.veil_msgact_moderate
  VeilMsgAction.Report -> MR.strings.veil_msgact_report
}

private fun veilActionIcon(action: VeilMsgAction): ImageResource = when (action) {
  VeilMsgAction.Reply -> MR.images.ic_reply
  VeilMsgAction.Edit -> MR.images.ic_edit_filled
  VeilMsgAction.Forward -> MR.images.ic_forward
  VeilMsgAction.React -> MR.images.ic_add_reaction
  VeilMsgAction.Copy -> MR.images.ic_content_copy
  VeilMsgAction.Share -> MR.images.ic_share
  VeilMsgAction.Save -> MR.images.ic_download
  VeilMsgAction.Download -> MR.images.ic_arrow_downward
  VeilMsgAction.Info -> MR.images.ic_info
  VeilMsgAction.Select -> MR.images.ic_check_circle
  VeilMsgAction.Hide -> MR.images.ic_visibility_off
  VeilMsgAction.Reveal -> MR.images.ic_visibility
  VeilMsgAction.Expand -> MR.images.ic_expand_all
  VeilMsgAction.Shrink -> MR.images.ic_collapse_all
  VeilMsgAction.ArchiveReport -> MR.images.ic_inventory_2
  VeilMsgAction.CancelFile -> MR.images.ic_close
  VeilMsgAction.Delete, VeilMsgAction.DeleteReport -> MR.images.ic_delete
  VeilMsgAction.Moderate, VeilMsgAction.Report -> MR.images.ic_flag
}

/** Runs [action]. Every branch calls the function upstream's menu item calls (see the notes in the Veil report). */
internal fun runVeilMessageAction(action: VeilMsgAction, item: ChatItem, env: VeilMessageEnv, clipboard: ClipboardManager) {
  val cInfo = env.chat.chatInfo
  val sent = item.chatDir.sent
  // same text as upstream's deleteMessageQuestionText() (moderated / blocked items use the plain warning)
  fun deleteQuestion(): String =
    if (!sent || cInfo.featureEnabled(ChatFeature.FullDelete) || cInfo is ChatInfo.Local || veilContentKind(item.content) == VeilContentKind.Moderated) {
      generalGetString(MR.strings.delete_message_cannot_be_undone_warning)
    } else {
      generalGetString(MR.strings.delete_message_mark_deleted_warning)
    }
  when (action) {
    VeilMsgAction.Reply -> {
      if (env.composeState.value.editing) {
        env.composeState.value = ComposeState(contextItem = ComposeContextItem.QuotedItem(item), useLinkPreviews = env.useLinkPreviews)
      } else {
        env.composeState.value = env.composeState.value.copy(contextItem = ComposeContextItem.QuotedItem(item))
      }
    }
    VeilMsgAction.Edit -> env.composeState.value = ComposeState(editingItem = item, useLinkPreviews = env.useLinkPreviews)
    VeilMsgAction.Forward -> env.forwardItem(cInfo, item)
    VeilMsgAction.React -> {} // opens the emoji row, handled by the sheet
    VeilMsgAction.Copy -> copyItemToClipboard(item, clipboard)
    VeilMsgAction.Share -> {
      var fileSource = getLoadedFileSource(item.file)
      val shareIfExists = {
        when (val f = fileSource) {
          null -> clipboard.shareText(item.content.text)
          else -> shareFile(item.text, f)
        }
      }
      if (chatModel.connectedToRemote() && fileSource == null) {
        withLongRunningApi(slow = 600_000) {
          item.file?.loadRemoteFile(true)
          fileSource = getLoadedFileSource(item.file)
          shareIfExists()
        }
      } else shareIfExists()
    }
    VeilMsgAction.Save -> env.saveLauncher?.let { veilSaveItemMedia(item, it) }
    VeilMsgAction.Download -> {
      val file = item.file ?: return
      withBGApi {
        val user = chatModel.currentUser.value
        if (user != null) controller.receiveFile(env.rhId, user, file.fileId)
      }
    }
    VeilMsgAction.Info -> env.showItemDetails(cInfo, item)
    VeilMsgAction.Select -> env.selectChatItem()
    VeilMsgAction.Hide, VeilMsgAction.Shrink -> env.reveal(false)
    VeilMsgAction.Reveal, VeilMsgAction.Expand -> env.reveal(true)
    VeilMsgAction.ArchiveReport -> {
      val group = (cInfo as? ChatInfo.Group)?.groupInfo ?: return
      showArchiveReportsAlert(listOf(item.id), group.membership.memberActive, env.archiveReports)
    }
    VeilMsgAction.CancelFile -> {
      val file = item.file ?: return
      val cancel = file.cancelAction ?: return
      cancelFileAlertDialog(file.fileId, cancelFile = env.cancelFile, cancelAction = cancel)
    }
    VeilMsgAction.Delete, VeilMsgAction.DeleteReport ->
      deleteItemWithDialog(env.chatsCtx, cInfo, item, env.revealed, deleteQuestion(), env.deleteMessage, env.deleteMessages)
    VeilMsgAction.Moderate ->
      moderateMessageAlertDialog(item, moderateMessageQuestionText(cInfo.featureEnabled(ChatFeature.FullDelete), 1), env.deleteMessage)
    VeilMsgAction.Report -> showReportReasonAlert(item, env.composeState)
  }
}
