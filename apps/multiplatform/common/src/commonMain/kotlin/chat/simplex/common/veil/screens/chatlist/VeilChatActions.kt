package chat.simplex.common.veil.screens.chatlist

import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chat.group.*
import chat.simplex.common.views.chatlist.*
import chat.simplex.common.views.contacts.onRequestAccepted
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.*
import chat.simplex.common.veil.VeilUi
import chat.simplex.common.veil.screens.newchat.veilShowNewChatSheetModal
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*
 * Adapter between the Veil chat list and upstream's chat list logic. Nothing here re-implements an action: each
 * entry calls the same function upstream's menu items and click handlers call.
 */

enum class VeilActionTone { Normal, Warning, Danger }

class VeilChatAction(
  val label: StringResource,
  val icon: ImageResource?,
  val tone: VeilActionTone = VeilActionTone.Normal,
  val run: () -> Unit,
)

/**
 * Opens [chat] the way upstream's `ChatListNavLinkView` does on click (the conversation screen itself is shown by
 * `AndroidScreen` once `chatModel.chatId` is set).
 */
fun veilOpenChat(chat: Chat, scope: CoroutineScope) {
  val rh = chat.remoteHostId
  when (val info = chat.chatInfo) {
    is ChatInfo.Direct -> if (chatModel.chatId.value != chat.id) scope.launch { directChatAction(rh, info.contact, chatModel) }
    is ChatInfo.Group -> if (chatModel.chatId.value != chat.id) scope.launch { groupChatAction(rh, info.groupInfo, chatModel) }
    is ChatInfo.Local -> if (chatModel.chatId.value != chat.id) scope.launch { noteFolderChatAction(rh, info.noteFolder) }
    is ChatInfo.ContactRequest -> contactRequestAlertDialog(rh, info, chatModel) { onRequestAccepted(it) }
    is ChatInfo.ContactConnection, is ChatInfo.InvalidJSON -> chatModel.chatId.value = chat.id
  }
}

/** Chat actions for the sheet that opens from the avatar. Same set and conditions as upstream's long-press menu (list assignment excluded). */
fun veilChatActions(chat: Chat): List<VeilChatAction> {
  val rh = chat.remoteHostId
  val showMarkRead = chat.chatStats.unreadCount > 0 || chat.chatStats.unreadChat

  fun markAction() =
    if (showMarkRead) VeilChatAction(MR.strings.veil_list_act_mark_read, MR.images.ic_check) {
      markChatRead(chat)
      ntfManager.cancelNotificationsForChat(chat.id)
    } else VeilChatAction(MR.strings.veil_list_act_mark_unread, MR.images.ic_mark_chat_unread) { markChatUnread(chat, chatModel) }

  fun favoriteAction(): VeilChatAction {
    val fav = chat.chatInfo.chatSettings?.favorite == true
    return VeilChatAction(
      if (fav) MR.strings.veil_list_act_unfavorite else MR.strings.veil_list_act_favorite,
      if (fav) MR.images.ic_star_off else MR.images.ic_star
    ) { toggleChatFavorite(rh, chat.chatInfo, !fav, chatModel) }
  }

  fun muteAction(next: MsgFilter) = VeilChatAction(
    when (next) {
      MsgFilter.None -> MR.strings.veil_list_act_mute
      MsgFilter.All -> MR.strings.veil_list_act_unmute
      MsgFilter.Mentions -> MR.strings.veil_list_act_mentions
    },
    next.icon
  ) { toggleNotifications(rh, chat.chatInfo, next, chatModel) }

  fun clearAction() = VeilChatAction(MR.strings.veil_list_act_clear, MR.images.ic_settings_backup_restore, VeilActionTone.Warning) { clearChatDialog(chat) }

  fun requestActions(contactRequestId: Long): List<VeilChatAction> = buildList {
    add(VeilChatAction(MR.strings.veil_list_act_accept, MR.images.ic_check) { acceptContactRequest(rh, incognito = false, contactRequestId, true, chatModel) })
    if (!chatModel.addressShortLinkDataSet()) {
      add(VeilChatAction(MR.strings.veil_list_act_accept_incognito, MR.images.ic_theater_comedy) { acceptContactRequest(rh, incognito = true, contactRequestId, true, chatModel) })
    }
    add(VeilChatAction(MR.strings.veil_list_act_reject, MR.images.ic_close, VeilActionTone.Danger) { rejectContactRequest(rh, contactRequestId, chatModel) })
  }

  fun deleteContact() = VeilChatAction(MR.strings.veil_list_act_delete_contact, MR.images.ic_delete, VeilActionTone.Danger) { deleteContactDialog(chat, chatModel) }

  return when (val info = chat.chatInfo) {
    is ChatInfo.Direct -> {
      val contact = info.contact
      buildList {
        if (contact.nextAcceptContactRequest) {
          if (contact.contactRequestId != null) {
            addAll(requestActions(contact.contactRequestId))
          } else if (contact.groupDirectInv != null && !contact.groupDirectInv.memberRemoved) {
            add(VeilChatAction(MR.strings.veil_list_act_accept, MR.images.ic_check) { acceptMemberContact(rh, contact.contactId, null) })
            add(VeilChatAction(MR.strings.veil_list_act_reject, MR.images.ic_close, VeilActionTone.Danger) { showRejectMemberContactRequestAlert(rh, contact) })
          } else {
            add(deleteContact())
          }
        } else {
          if (contact.activeConn != null) {
            add(markAction())
            add(favoriteAction())
            add(muteAction(contact.chatSettings.enableNtfs.nextMode(false)))
            add(clearAction())
          }
          add(deleteContact())
        }
      }
    }
    is ChatInfo.Group -> {
      val g = info.groupInfo
      val canLeave = g.membership.memberCurrentOrPending && !(g.useRelays && g.isOwner)
      val leave = VeilChatAction(MR.strings.veil_list_act_leave_group, MR.images.ic_logout, VeilActionTone.Danger) { leaveGroupDialog(rh, g, chatModel) }
      val delete = VeilChatAction(MR.strings.veil_list_act_delete_group, MR.images.ic_delete, VeilActionTone.Danger) { deleteGroupDialog(chat, g, chatModel) }
      buildList {
        when (g.membership.memberStatus) {
          GroupMemberStatus.MemInvited -> {
            add(VeilChatAction(MR.strings.veil_list_act_join_group, MR.images.ic_login) {
              withBGApi { chatModel.controller.apiJoinGroup(rh, g.groupId) }
            })
            if (g.canDelete) add(delete)
          }
          GroupMemberStatus.MemAccepted -> {
            if (canLeave) add(leave)
            if (g.canDelete) add(delete)
          }
          else -> {
            add(markAction())
            add(favoriteAction())
            add(muteAction(g.chatSettings.enableNtfs.nextMode(true)))
            add(clearAction())
            if (canLeave) add(leave)
            if (g.canDelete) add(delete)
          }
        }
      }
    }
    is ChatInfo.Local -> listOf(
      markAction(),
      VeilChatAction(MR.strings.veil_list_act_clear, MR.images.ic_settings_backup_restore, VeilActionTone.Warning) { clearNoteFolderDialog(chat) },
    )
    is ChatInfo.ContactRequest -> requestActions(info.apiId)
    is ChatInfo.ContactConnection -> listOf(
      VeilChatAction(MR.strings.veil_list_act_rename_connection, MR.images.ic_edit) {
        ModalManager.center.closeModals()
        ModalManager.end.closeModals()
        ModalManager.center.showModalCloseable(settings = true, showClose = appPlatform.isAndroid, cardScreen = true) { close ->
          ContactConnectionInfoView(chatModel, rh, info.contactConnection.connLinkInv, info.contactConnection, true, close)
        }
      },
      VeilChatAction(MR.strings.veil_list_act_delete_connection, MR.images.ic_delete, VeilActionTone.Danger) {
        deleteContactConnectionAlert(rh, info.contactConnection, chatModel) { chatModel.dismissConnReqView(info.contactConnection.id) }
      },
    )
    is ChatInfo.InvalidJSON -> emptyList()
  }
}

/**
 * Opens the new-chat sheet: the Veil sheet when `VeilUi.newChat` is on (Android), otherwise upstream's `NewChatSheet`
 * through a copy of the private `showNewChatSheet` in ChatListView.kt (that function is private and upstream files are
 * not edited).
 */
fun veilShowNewChatSheet() {
  if (VeilUi.newChat && appPlatform.isAndroid) {
    veilShowNewChatSheetModal()
    return
  }
  val oneHandUI = appPrefs.oneHandUI.state
  connectProgressManager.cancelConnectProgress()
  ModalManager.start.closeModals()
  ModalManager.end.closeModals()
  chatModel.newChatSheetVisible.value = true
  ModalManager.start.showCustomModal { close ->
    val close = {
      chatModel.newChatSheetVisible.value = false
      close()
    }
    ModalView(close, showAppBar = !oneHandUI.value) {
      if (appPlatform.isAndroid) {
        BackHandler { close() }
      }
      NewChatSheet(rh = chatModel.currentRemoteHost.value, close)
      androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { chatModel.newChatSheetVisible.value = false }
      }
    }
  }
}
