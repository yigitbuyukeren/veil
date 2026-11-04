package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.*

/*
 * Which actions the Veil message sheet offers for an item. This mirrors, branch by branch and condition by
 * condition, the item menu in upstream's `ChatItemView.kt` (`MsgContentItemDropdownMenu`, `DeleteItemMenu`,
 * `MarkedDeletedItemDropdownMenu`, `DeletedItem`, `LegacyDeletedItem`), so the same item gets the same actions in
 * both.
 *
 * Two steps, so the rules can be unit tested without the Android resources a `CIContent` needs:
 *  1. [veilItemFacts] reads the upstream model (`ChatItem`, `ChatInfo`, `CIFile`) with upstream's own properties
 *     (`canBeDeletedForSelf`, `allowAddReaction`, `memberToModerate`, `forwardingAllowed`...) into plain booleans;
 *  2. [veilMessageActions] applies the menu rules to those facts.
 */

/** An action in the sheet. Declaration order is the order in the sheet (reply, edit, forward, react, then the rest, danger last). */
enum class VeilMsgAction(val danger: Boolean = false) {
  Reply,
  Edit,
  Forward,
  /** Opens the emoji row of the sheet. */
  React,
  Copy,
  Share,
  /** Save the loaded file / image to the device. */
  Save,
  /** Receive a file that is only an invitation so far. */
  Download,
  /** Delivery details (upstream "Bilgi"). */
  Info,
  Select,
  Hide,
  Reveal,
  Expand,
  Shrink,
  ArchiveReport,
  CancelFile(danger = true),
  Delete(danger = true),
  DeleteReport(danger = true),
  Moderate(danger = true),
  Report(danger = true),
}

/** State of the chat view that upstream's menu reads besides the item itself. */
class VeilActionFacts(
  /** The item is shown expanded (`revealed` in `ChatItemView`). */
  val revealed: Boolean = false,
  /** A live message is being composed (`composeState.liveMessage != null`). */
  val live: Boolean = false,
  /** Upstream's `preview` parameter (previews have no delete). */
  val preview: Boolean = false,
  /** `range.value != null`: the item is part of a collapsed run. */
  val inRange: Boolean = false,
  /** Number of items in that run (`range.value?.count()`). */
  val rangeCount: Int = 0,
  /** Multi-select mode is on (`selectedChatItems.value != null`). */
  val selectionActive: Boolean = false,
  val developerTools: Boolean = false,
  /** The file exists on disk (or on the remote host) and can be saved. */
  val fileLoaded: Boolean = false,
  /** The file is an invitation that may be downloaded (`RcvInvitation` and within the size limit). */
  val fileDownloadable: Boolean = false,
  /** The user has at least one supported reaction left to add to the item. */
  val reactionsLeft: Boolean = true,
)

/** What kind of content the item has, as far as upstream's menu is concerned (`when (cItem.content)` in `ChatItemView`). */
enum class VeilContentKind {
  /** Sent / received message: the big menu. */
  Message,
  /** `SndDeleted` / `RcvDeleted`: info, delete, select. */
  LegacyDeleted,
  /** Calls, decryption errors, group invitations, preference requests, invalid JSON: delete, select. */
  DeleteOnly,
  /** `RcvIntegrityError`: delete, select, only with developer tools. */
  IntegrityError,
  /** Events and feature changes: they go through the message menu, which for them reduces to its last branches. */
  Event,
  /** Moderated / blocked placeholders: hide / reveal / expand, info, delete, select. */
  Moderated,
  /** No menu in upstream: message errors, E2EE notices, the chat banner. */
  NoMenu,
}

/** Plain facts about one item in one chat. Defaults describe a sent, editable text message in a direct chat. */
class VeilItemFacts(
  val content: VeilContentKind = VeilContentKind.Message,
  /** `meta.itemDeleted != null` */
  val markedDeleted: Boolean = false,
  /** `isDeletedContent` */
  val deletedContent: Boolean = false,
  /** `content.msgContent != null` */
  val hasMsgContent: Boolean = true,
  /** `meta.itemId >= 0` (live messages being composed have a negative id) */
  val idNonNegative: Boolean = true,
  val isReport: Boolean = false,
  val isVoice: Boolean = false,
  val isLiveDummy: Boolean = false,
  /** `meta.isLive` */
  val metaLive: Boolean = false,
  val localNote: Boolean = false,
  /** `canBeDeletedForSelf` */
  val canBeDeletedForSelf: Boolean = true,
  /** `mergeCategory != null` */
  val merged: Boolean = false,
  /** `meta.editable` */
  val editable: Boolean = true,
  /** `content.text.isNotEmpty()` */
  val hasText: Boolean = true,
  val hasFile: Boolean = false,
  /** `file.forwardingAllowed()` (false without a file) */
  val fileForwardingAllowed: Boolean = false,
  /** `file.cancelAction != null` */
  val fileCancellable: Boolean = false,
  /** `cInfo.featureEnabled(Reactions) && cItem.allowAddReaction` */
  val reactionsAllowed: Boolean = true,
  /** `cInfo.sendMsgEnabled` */
  val sendEnabled: Boolean = true,
  val inGroup: Boolean = false,
  /** `chatDir is GroupSnd` */
  val ownGroupMessage: Boolean = false,
  /** In a group: the user's membership role is Moderator or higher. */
  val moderatorOrAbove: Boolean = false,
  /** In a group: the user's membership role is exactly Member. */
  val plainMember: Boolean = false,
  /** `groupInfo.groupFeatureEnabled(GroupFeature.Reports)` */
  val reportsEnabled: Boolean = false,
  /** `cItem.memberToModerate(cInfo)?.first != null` */
  val canModerate: Boolean = false,
)

/** Reads [VeilItemFacts] from the upstream model, with upstream's own properties and functions. */
fun veilItemFacts(item: ChatItem, chatInfo: ChatInfo): VeilItemFacts {
  val group = (chatInfo as? ChatInfo.Group)?.groupInfo
  val role = group?.membership?.memberRole
  val file = item.file
  return VeilItemFacts(
    content = veilContentKind(item.content),
    markedDeleted = item.meta.itemDeleted != null,
    deletedContent = item.isDeletedContent,
    hasMsgContent = item.content.msgContent != null,
    idNonNegative = item.meta.itemId >= 0,
    isReport = item.isReport,
    isVoice = item.content.msgContent is MsgContent.MCVoice,
    isLiveDummy = item.isLiveDummy,
    metaLive = item.meta.isLive,
    localNote = item.localNote,
    canBeDeletedForSelf = item.canBeDeletedForSelf,
    merged = item.mergeCategory != null,
    editable = item.meta.editable,
    hasText = item.content.text.isNotEmpty(),
    hasFile = file != null,
    fileForwardingAllowed = file?.forwardingAllowed() == true,
    fileCancellable = file?.cancelAction != null,
    reactionsAllowed = chatInfo.featureEnabled(ChatFeature.Reactions) && item.allowAddReaction,
    sendEnabled = chatInfo.sendMsgEnabled,
    inGroup = group != null,
    ownGroupMessage = item.chatDir is CIDirection.GroupSnd,
    moderatorOrAbove = role != null && role >= GroupMemberRole.Moderator,
    plainMember = role == GroupMemberRole.Member,
    reportsEnabled = group?.groupFeatureEnabled(GroupFeature.Reports) == true,
    canModerate = item.memberToModerate(chatInfo)?.first != null,
  )
}

fun veilContentKind(content: CIContent): VeilContentKind = when (content) {
  is CIContent.SndMsgContent, is CIContent.RcvMsgContent -> VeilContentKind.Message
  is CIContent.SndDeleted, is CIContent.RcvDeleted -> VeilContentKind.LegacyDeleted
  is CIContent.SndCall, is CIContent.RcvCall -> VeilContentKind.DeleteOnly
  is CIContent.RcvIntegrityError -> VeilContentKind.IntegrityError
  is CIContent.RcvMsgErrorContent -> VeilContentKind.NoMenu
  is CIContent.RcvDecryptionError, is CIContent.RcvGroupInvitation, is CIContent.SndGroupInvitation -> VeilContentKind.DeleteOnly
  is CIContent.RcvDirectEventContent, is CIContent.RcvGroupEventContent, is CIContent.SndGroupEventContent,
  is CIContent.RcvConnEventContent, is CIContent.SndConnEventContent,
  is CIContent.RcvChatFeature, is CIContent.SndChatFeature, is CIContent.SndChatPreference,
  is CIContent.RcvGroupFeature, is CIContent.SndGroupFeature,
  is CIContent.RcvChatFeatureRejected, is CIContent.RcvGroupFeatureRejected -> VeilContentKind.Event
  is CIContent.RcvChatPreference -> VeilContentKind.DeleteOnly
  is CIContent.SndModerated, is CIContent.RcvModerated, is CIContent.RcvBlocked -> VeilContentKind.Moderated
  is CIContent.SndDirectE2EEInfo, is CIContent.RcvDirectE2EEInfo, is CIContent.SndGroupE2EEInfo, is CIContent.RcvGroupE2EEInfo,
  is CIContent.ChatBanner -> VeilContentKind.NoMenu
  is CIContent.InvalidJSON -> VeilContentKind.DeleteOnly
}

/** Actions for [item] in [chatInfo], in sheet order. Empty = upstream shows no menu either. */
fun veilMessageActions(item: ChatItem, chatInfo: ChatInfo, facts: VeilActionFacts): List<VeilMsgAction> =
  veilMessageActions(veilItemFacts(item, chatInfo), facts)

/** The rules, on plain facts. */
fun veilMessageActions(i: VeilItemFacts, f: VeilActionFacts): List<VeilMsgAction> {
  val out = LinkedHashSet<VeilMsgAction>()
  val selectIfAllowed = { if (i.canBeDeletedForSelf) out += VeilMsgAction.Select }
  // ChatItemView: a deleted item that is collapsed (or whose content is gone) gets MarkedDeletedItemDropdownMenu
  if (i.markedDeleted && (!f.revealed || i.deletedContent)) {
    if (!i.deletedContent) out += VeilMsgAction.Reveal
    out += VeilMsgAction.Info
    out += VeilMsgAction.Delete
    selectIfAllowed()
    return out.sortedBy { it.ordinal }
  }
  when (i.content) {
    VeilContentKind.NoMenu -> {}
    VeilContentKind.DeleteOnly -> {
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    VeilContentKind.IntegrityError -> if (f.developerTools) {
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    VeilContentKind.LegacyDeleted -> {
      out += VeilMsgAction.Info
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    VeilContentKind.Moderated -> {
      revealToggle(i, f, out)
      out += VeilMsgAction.Info
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    VeilContentKind.Message, VeilContentKind.Event -> contentActions(i, f, out, selectIfAllowed)
  }
  return out.sortedBy { it.ordinal }
}

private fun revealToggle(i: VeilItemFacts, f: VeilActionFacts, out: MutableSet<VeilMsgAction>) {
  if (f.revealed) out += VeilMsgAction.Hide
  else if (!i.deletedContent) out += VeilMsgAction.Reveal
  else if (f.inRange) out += VeilMsgAction.Expand
}

/** `MsgContentItemDropdownMenu`: first matching branch wins, same order as upstream's `when`. */
private fun contentActions(i: VeilItemFacts, f: VeilActionFacts, out: MutableSet<VeilMsgAction>, selectIfAllowed: () -> Unit) {
  when {
    i.isReport && !i.markedDeleted && i.inGroup -> {
      if (!i.ownGroupMessage && i.moderatorOrAbove) out += VeilMsgAction.ArchiveReport
      out += VeilMsgAction.DeleteReport
      out += VeilMsgAction.Select // unconditional upstream
    }
    i.hasMsgContent && i.idNonNegative && !i.isReport -> {
      if (i.reactionsAllowed && f.reactionsLeft) out += VeilMsgAction.React
      if (!i.markedDeleted && !f.live && !i.localNote && i.sendEnabled) out += VeilMsgAction.Reply
      if (i.hasText || i.fileForwardingAllowed) {
        out += VeilMsgAction.Share
        out += VeilMsgAction.Copy
      }
      if (i.hasFile && f.fileLoaded) out += VeilMsgAction.Save
      else if (i.hasFile && f.fileDownloadable) out += VeilMsgAction.Download
      if (i.editable && !i.isVoice && !f.live) out += VeilMsgAction.Edit
      if (!i.markedDeleted && (!i.hasFile || i.fileForwardingAllowed) && !i.isLiveDummy && !f.live) out += VeilMsgAction.Forward
      out += VeilMsgAction.Info
      if (f.revealed) out += VeilMsgAction.Hide
      if (!i.markedDeleted && i.hasFile && i.fileCancellable && !i.localNote) out += VeilMsgAction.CancelFile
      if (!(f.live && i.metaLive) && !f.preview) out += VeilMsgAction.Delete
      if (!i.ownGroupMessage) {
        if (i.canModerate) {
          out += VeilMsgAction.Moderate
        } else if (!i.markedDeleted && i.inGroup && i.reportsEnabled && i.plainMember && !f.live) {
          out += VeilMsgAction.Report
        }
      }
      selectIfAllowed()
    }
    i.markedDeleted -> {
      revealToggle(i, f, out)
      out += VeilMsgAction.Info
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    i.deletedContent -> {
      out += VeilMsgAction.Info
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    i.merged && (f.rangeCount > 1 || f.revealed) -> {
      out += if (f.revealed) VeilMsgAction.Shrink else VeilMsgAction.Expand
      out += VeilMsgAction.Delete
      selectIfAllowed()
    }
    else -> {
      out += VeilMsgAction.Delete
      if (!f.selectionActive) out += VeilMsgAction.Select
    }
  }
}
