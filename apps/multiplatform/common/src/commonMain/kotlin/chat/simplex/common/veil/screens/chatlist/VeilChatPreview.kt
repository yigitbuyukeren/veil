package chat.simplex.common.veil.screens.chatlist

import chat.simplex.common.model.*
import chat.simplex.common.views.chat.ComposeState
import chat.simplex.common.views.chat.item.markedDeletedText
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR

/**
 * What the second line of a chat row shows. [sensitive] text (message content, drafts, connection state of a
 * chat) sits behind the Perde; the rest (no messages yet, previews switched off) is shown as is.
 */
class VeilPreview(val text: String, val sensitive: Boolean)

/** Profile name used to seed the mask avatar: the profile's own name, not the local alias, so aliasing does not change the mask. */
fun veilAvatarName(chatInfo: ChatInfo): String = when (chatInfo) {
  is ChatInfo.Direct -> chatInfo.contact.profile.displayName
  is ChatInfo.Group -> chatInfo.groupInfo.groupProfile.displayName
  else -> chatInfo.displayName
}

fun veilAvatarSeed(chat: Chat): String = avatarSeed(chat.id, veilAvatarName(chat.chatInfo))

/**
 * Plain-text preview of a chat. Mirrors what upstream's `ChatPreviewView` decides to show (draft, connection state,
 * last item), reduced to one string. Honours the "show previews" privacy preference: when it is off the message
 * text is never read here.
 */
fun veilPreviewOf(
  chat: Chat,
  showPreviews: Boolean,
  draft: ComposeState?,
  draftChatId: String?,
): VeilPreview {
  val cInfo = chat.chatInfo
  when (cInfo) {
    is ChatInfo.ContactRequest -> return VeilPreview(generalGetString(MR.strings.veil_list_preview_request), sensitive = false)
    is ChatInfo.ContactConnection -> return VeilPreview(cInfo.contactConnection.description, sensitive = false)
    is ChatInfo.InvalidJSON -> return VeilPreview(generalGetString(MR.strings.invalid_data), sensitive = false)
    else -> {}
  }
  val ci = chat.chatItems.lastOrNull()
  if (showPreviews && draft != null && draftChatId == chat.id && draft.message.text.isNotBlank()) {
    return VeilPreview(String.format(generalGetString(MR.strings.veil_list_preview_draft), oneLine(draft.message.text)), sensitive = true)
  }
  val state = connectionStateText(cInfo)
  if (ci?.content?.hasMsgContent != true && state != null) return VeilPreview(state, sensitive = true)
  if (!showPreviews) return VeilPreview(generalGetString(MR.strings.veil_list_preview_hidden), sensitive = false)
  if (ci == null) return VeilPreview(generalGetString(MR.strings.veil_list_preview_none), sensitive = false)
  val text = when {
    ci.meta.itemDeleted != null -> markedDeletedText(ci, cInfo)
    ci.content.msgContent is MsgContent.MCChat -> (ci.content.msgContent as MsgContent.MCChat).chatLink.displayName
    else -> ci.text(cInfo.isChannel)
  }
  val sender = if (cInfo is ChatInfo.Group && !ci.chatDir.sent && !ci.meta.showGroupAsSender) ci.memberDisplayName else null
  return VeilPreview((if (sender != null) "$sender: " else "") + oneLine(text), sensitive = true)
}

private fun oneLine(s: String): String = s.replace(Regex("\\s*\\n+\\s*"), " ").trim()

/** Connection-state line upstream shows instead of a message (same conditions and strings as `chatPreviewInfoText`). */
private fun connectionStateText(cInfo: ChatInfo): String? = when (cInfo) {
  is ChatInfo.Direct -> {
    val c = cInfo.contact
    when {
      c.isContactCard -> generalGetString(MR.strings.contact_tap_to_connect)
      c.isBot && c.nextConnectPrepared -> generalGetString(MR.strings.open_to_use_bot)
      c.sendMsgToConnect -> generalGetString(MR.strings.open_to_connect)
      c.nextAcceptContactRequest -> generalGetString(MR.strings.open_to_accept)
      !c.sndReady && c.activeConn != null && c.active ->
        if ((c.preparedContact?.uiConnLinkType == ConnectionMode.Con && !c.isBot) || c.contactGroupMemberId != null) {
          generalGetString(MR.strings.contact_should_accept)
        } else {
          generalGetString(MR.strings.contact_connection_pending)
        }
      else -> null
    }
  }
  is ChatInfo.Group -> {
    val g = cInfo.groupInfo
    if (g.nextConnectPrepared) {
      generalGetString(if (g.businessChat?.chatType == BusinessChatType.Business) MR.strings.open_to_connect else MR.strings.group_preview_open_to_join)
    } else when (g.membership.memberStatus) {
      GroupMemberStatus.MemRejected -> generalGetString(MR.strings.group_preview_rejected)
      GroupMemberStatus.MemInvited ->
        if (g.membership.memberIncognito) String.format(generalGetString(MR.strings.group_preview_join_as), g.membership.memberProfile.displayName)
        else generalGetString(MR.strings.group_preview_you_are_invited)
      GroupMemberStatus.MemAccepted -> generalGetString(MR.strings.group_connection_pending)
      GroupMemberStatus.MemPendingReview, GroupMemberStatus.MemPendingApproval -> generalGetString(MR.strings.reviewed_by_admins)
      else -> null
    }
  }
  else -> null
}
