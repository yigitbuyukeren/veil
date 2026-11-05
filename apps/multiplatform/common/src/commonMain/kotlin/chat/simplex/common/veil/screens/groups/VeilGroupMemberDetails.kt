package chat.simplex.common.veil.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.QueueSwitch
import chat.simplex.common.veil.screens.chat.VeilCaption
import chat.simplex.common.veil.screens.chat.VeilInfoRow
import chat.simplex.common.veil.screens.chat.VeilQr
import chat.simplex.common.veil.screens.chat.VeilScreenScaffold
import chat.simplex.common.veil.screens.chat.abortSwitchShown
import chat.simplex.common.veil.screens.chat.chatAvatarSeed
import chat.simplex.common.veil.screens.chat.serverCopyText
import chat.simplex.common.veil.screens.chat.serverHosts
import chat.simplex.common.veil.screens.settings.VeilSectionLabel
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chat.group.*
import chat.simplex.common.views.chatlist.openDirectChat
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Veil member details ("Ayrıntılar" of the member screen): replaces upstream's complete `GroupMemberInfoView`.
 *
 * What upstream's screen does, and where it is now:
 * - security code, role change, block / unblock, block for everyone, remove, delete messages: already on the Veil member
 *   screen (VeilGroupMember.kt) with the same dialogs and calls.
 * - message the member: opens the existing direct chat (`openDirectChat`), or creates a contact with
 *   `apiCreateMemberContact` and opens it; when the connection cannot send, upstream's alerts (`showFixConnectionAlert`,
 *   renegotiation in progress, connection not ready); when direct messages are off in the group, upstream's alert. Rule: [memberMessage].
 * - support chat with the member: `showMemberSupportChatView` (upstream's screen), rule [memberSupportShown].
 * - fix connection: `apiSyncGroupMemberRatchet(force = false)` + `updateGroupMemberConnectionStats` in both contexts.
 * - member address with QR, share, and connect: `connectViaMemberAddressAlert` (upstream's dialog).
 * - network status (upstream's explanation), change / abort receiving address: `showSwitchAddressAlert` + `apiSwitchGroupMember`,
 *   `showAbortSwitchAddressAlert` + `apiAbortSwitchGroupMember`, server lists (copy on tap).
 * - connection error, developer tools: local name, database id, connection level, `apiGroupMemberQueueInfo`.
 * - not offered: call buttons (calls are an opt-in handled separately) and the forced re-sync (commented out upstream).
 */
fun veilShowMemberDetails(rhId: Long?, groupInfo: GroupInfo, member: GroupMember, closeScreen: () -> Unit) {
  withBGApi {
    // same preload as upstream's member info
    val stats = chatModel.controller.apiGroupMemberInfo(rhId, groupInfo.groupId, member.groupMemberId)?.second
    ModalManager.end.showCustomModal { closeDetails ->
      VeilMemberDetailsScreen(rhId, groupInfo.groupId, member.groupMemberId, stats, closeDetails) {
        closeDetails()
        closeScreen()
      }
    }
  }
}

@Composable
fun VeilMemberDetailsScreen(rhId: Long?, groupId: Long, groupMemberId: Long, initialStats: ConnectionStats?, close: () -> Unit, closeAll: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val groupInfo = remember(groupId) { derivedStateOf { (chatModel.getGroupChat(groupId)?.chatInfo as? ChatInfo.Group)?.groupInfo } }.value
  val member = remember(groupMemberId) { derivedStateOf { chatModel.getGroupMember(groupMemberId) } }.value
  if (groupInfo == null || member == null) {
    LaunchedEffect(Unit) { close() }
    return
  }
  KeyChangeEffect(chatModel.chatId.value) { ModalManager.end.closeModals() }
  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  val connStats = remember { mutableStateOf(initialStats) }
  var busy by remember { mutableStateOf(false) }
  val scrollToItemId: MutableState<Long?> = remember { mutableStateOf(null) }
  val cStats = connStats.value
  val contactId = member.memberContactId
  val knownChat = contactId?.let { id -> chatModel.getContactChat(id)?.takeIf { it.chatInfo is ChatInfo.Direct && (it.chatInfo as ChatInfo.Direct).contact.directOrUsed } }
  val directOn = groupInfo.fullGroupPreferences.directMessages.on(groupInfo.membership)
  val message = memberMessage(hasKnownDirectChat = knownChat != null, hasContactId = contactId != null, directMessagesOn = directOn)
  val queues = cStats?.rcvQueuesInfo?.map { QueueSwitch(it.rcvSwitchStatus != null, it.canAbortSwitch) } ?: emptyList()

  fun applyStats(r: Pair<GroupMember, ConnectionStats>) {
    connStats.value = r.second
    scope.launch {
      withContext(Dispatchers.Main) { chatModel.chatsContext.updateGroupMemberConnectionStats(rhId, groupInfo, r.first, r.second) }
      withContext(Dispatchers.Main) { chatModel.secondaryChatsContext.value?.updateGroupMemberConnectionStats(rhId, groupInfo, r.first, r.second) }
    }
  }

  fun syncConnection() {
    withBGApi {
      val r = chatModel.controller.apiSyncGroupMemberRatchet(rhId, groupInfo.apiId, member.groupMemberId, force = false)
      if (r != null) {
        applyStats(r)
        close()
      }
    }
  }

  fun createContact() {
    when (createContactStep(member.sendMsgEnabled, cStats != null, cStats?.ratchetSyncAllowed == true, cStats?.ratchetSyncInProgress == true)) {
      CreateContactStep.Create -> withBGApi {
        busy = true
        val memberContact = chatModel.controller.apiCreateMemberContact(rhId, groupInfo.apiId, member.groupMemberId)
        if (memberContact != null) {
          val memberChat = Chat(remoteHostId = rhId, ChatInfo.Direct(memberContact), chatItems = arrayListOf())
          withContext(Dispatchers.Main) { chatModel.chatsContext.addChat(memberChat) }
          openDirectChat(rhId, memberContact.contactId)
          closeAll()
        }
        busy = false
      }
      CreateContactStep.OfferFix -> showFixConnectionAlert(syncConnection = { syncConnection() })
      CreateContactStep.RenegotiationInProgress -> AlertManager.shared.showAlertMsg(
        generalGetString(MR.strings.cant_send_message_to_member_alert_title),
        generalGetString(MR.strings.encryption_renegotiation_in_progress),
      )
      CreateContactStep.NotReady -> AlertManager.shared.showAlertMsg(
        generalGetString(MR.strings.cant_send_message_to_member_alert_title),
        generalGetString(MR.strings.connection_not_ready),
      )
      CreateContactStep.Nothing -> {}
    }
  }

  VeilScreenScaffold(title = stringResource(MR.strings.veil_grp_member_details), onBack = close, subtitle = member.chatViewName) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      MaskAvatar(
        seed = chatAvatarSeed("m${member.memberId}", member.memberProfile.displayName),
        size = 72.dp,
        incognito = member.memberIncognito,
        modifier = Modifier.padding(bottom = 10.dp),
      )
      BasicText(
        member.chatViewName,
        style = type.sheetTitle.copy(color = colors.ink, textAlign = TextAlign.Center),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      BasicText(
        memberSubtitle(member),
        Modifier.padding(top = 2.dp),
        style = type.caption.copy(color = colors.ink2, fontSize = 13.sp),
      )
    }

    // ---- message, support chat, fix connection ----
    val showSupport = memberSupportShown(
      groupInfo.membership.memberRole, member.memberRole, groupInfo.fullGroupPreferences.support.on, member.supportChat != null, member.memberActive,
    )
    val showFix = memberFixShown(member.memberActive, cStats != null && cStats.ratchetSyncAllowed)
    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      val looksOff = messageLooksDisabled(message, member.sendMsgEnabled, cStats?.ratchetSyncAllowed == true) || busy
      VeilInfoRow(
        icon = MR.images.ic_chat_bubble,
        title = stringResource(if (busy) MR.strings.veil_gmem_opening else MR.strings.veil_gmem_message),
        value = if (message == MemberMessage.Prohibited) stringResource(MR.strings.veil_gmem_dm_off) else null,
        modifier = Modifier.alpha(if (looksOff || message == MemberMessage.Prohibited) 0.5f else 1f),
        onClick = {
          if (!busy) {
            when (message) {
              MemberMessage.OpenChat -> scope.launch {
                openDirectChat(rhId, contactId!!)
                closeAll()
              }
              MemberMessage.CreateContact -> createContact()
              MemberMessage.Prohibited -> AlertManager.shared.showAlertMsg(
                generalGetString(MR.strings.cant_send_message_to_member_alert_title),
                generalGetString(MR.strings.direct_messages_are_prohibited_in_group),
              )
            }
          }
        },
      )
      if (showSupport) {
        VeilInfoRow(
          icon = MR.images.ic_flag,
          title = stringResource(MR.strings.veil_gmem_support),
          onClick = {
            val scopeInfo = GroupChatScopeInfo.MemberSupport(groupMember_ = member)
            val supportChatInfo = ChatInfo.Group(groupInfo, groupChatScope = scopeInfo)
            scope.launch { showMemberSupportChatView(chatModel.chatId, scrollToItemId = scrollToItemId, supportChatInfo, scopeInfo) }
          },
        )
      }
      if (showFix) {
        VeilInfoRow(
          icon = MR.images.ic_sync_problem,
          title = stringResource(MR.strings.veil_cadv_fix),
          value = stringResource(MR.strings.veil_gmem_fix_note),
          onClick = { syncConnection() },
        )
      }
    }

    // ---- member address ----
    val link = member.contactLink
    if (link != null) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_address))
      VeilCard(Modifier.padding(bottom = 6.dp), contentPadding = PaddingValues(8.dp)) {
        VeilQr(simplexChatLink(link), small = link.count() < 200, description = stringResource(MR.strings.veil_cadv_address_qr))
        VeilInfoRow(
          icon = MR.images.ic_share,
          title = stringResource(MR.strings.veil_cadv_address_share),
          onClick = { clipboard.shareText(simplexChatLink(link)) },
        )
        if (connectViaAddressShown(contactId != null, knownChat != null, directOn)) {
          VeilInfoRow(
            icon = MR.images.ic_add_link,
            title = stringResource(MR.strings.veil_gmem_connect),
            onClick = { connectViaMemberAddressAlert(rhId, link) },
          )
        }
      }
      VeilCaption(String.format(stringResource(MR.strings.veil_cadv_address_note), member.displayName), Modifier.padding(bottom = 10.dp))
    }

    // ---- group and role ----
    VeilSectionLabel(stringResource(MR.strings.veil_grp_member_title))
    VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
      VeilInfoRow(icon = MR.images.ic_group, title = stringResource(MR.strings.info_row_group), value = groupInfo.displayName)
      VeilInfoRow(icon = MR.images.ic_person, title = stringResource(MR.strings.role_in_group), value = stringResource(roleLabel(member.memberRole)))
    }

    // ---- servers ----
    if (cStats != null) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_servers))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        val subStatus = cStats.subStatus
        if (subStatus != null) {
          VeilInfoRow(
            icon = MR.images.ic_info,
            title = stringResource(MR.strings.veil_cadv_status),
            value = subStatus.statusString,
            onClick = { AlertManager.shared.showAlertMsg(generalGetString(MR.strings.network_status), subStatus.statusExplanation) },
          )
        }
        val switchOff = memberSwitchDisabled(queues, member.sendMsgEnabled)
        VeilInfoRow(
          icon = MR.images.ic_swap_horizontal_circle,
          title = stringResource(MR.strings.veil_cadv_switch),
          modifier = Modifier.alpha(if (switchOff) 0.38f else 1f),
          onClick = if (switchOff) null else ({
            showSwitchAddressAlert(switchAddress = {
              withBGApi {
                val r = chatModel.controller.apiSwitchGroupMember(rhId, groupInfo.apiId, member.groupMemberId)
                if (r != null) {
                  applyStats(r)
                  close()
                }
              }
            })
          }),
        )
        if (abortSwitchShown(queues)) {
          val abortOff = memberAbortDisabled(queues, member.sendMsgEnabled)
          VeilInfoRow(
            icon = MR.images.ic_close,
            title = stringResource(MR.strings.veil_cadv_abort),
            modifier = Modifier.alpha(if (abortOff) 0.38f else 1f),
            onClick = if (abortOff) null else ({
              showAbortSwitchAddressAlert(abortSwitchAddress = {
                withBGApi {
                  val r = chatModel.controller.apiAbortSwitchGroupMember(rhId, groupInfo.apiId, member.groupMemberId)
                  if (r != null) {
                    applyStats(r)
                    close()
                  }
                }
              })
            }),
          )
        }
        val rcvServers = cStats.rcvQueuesInfo.map { it.rcvServer }
        if (rcvServers.isNotEmpty()) {
          VeilInfoRow(
            icon = MR.images.ic_dns,
            title = stringResource(MR.strings.veil_cadv_receiving),
            value = serverHosts(rcvServers),
            onClick = { clipboard.setText(AnnotatedString(serverCopyText(rcvServers))); showToast(generalGetString(MR.strings.copied)) },
          )
        }
        val sndServers = cStats.sndQueuesInfo.map { it.sndServer }
        if (sndServers.isNotEmpty()) {
          VeilInfoRow(
            icon = MR.images.ic_dns,
            title = stringResource(MR.strings.veil_cadv_sending),
            value = serverHosts(sndServers),
            onClick = { clipboard.setText(AnnotatedString(serverCopyText(sndServers))); showToast(generalGetString(MR.strings.copied)) },
          )
        }
      }
    }

    // ---- connection error ----
    val connFailedErr = member.activeConn?.connFailedErr
    if (connFailedErr != null) {
      VeilSectionLabel(stringResource(MR.strings.info_row_connection_failed))
      WarnBox(connFailedErr)
      Spacer(Modifier.height(14.dp))
    }

    // ---- developer tools ----
    if (developerTools) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_developer))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        VeilInfoRow(icon = MR.images.ic_info, title = stringResource(MR.strings.veil_cadv_local_name), value = member.localDisplayName)
        VeilInfoRow(icon = MR.images.ic_info, title = stringResource(MR.strings.veil_cadv_db_id), value = member.groupMemberId.toString())
        val conn = member.activeConn
        if (conn != null) {
          VeilInfoRow(
            icon = MR.images.ic_info,
            title = stringResource(MR.strings.info_row_connection),
            value = if (connectionIsDirect(conn.connLevel)) stringResource(MR.strings.conn_level_desc_direct)
              else String.format(generalGetString(MR.strings.conn_level_desc_indirect), conn.connLevel),
          )
        }
        VeilInfoRow(
          icon = MR.images.ic_info,
          title = stringResource(MR.strings.veil_cadv_queue_info),
          onClick = {
            withBGApi {
              val info = chatModel.controller.apiGroupMemberQueueInfo(rhId, groupInfo.apiId, member.groupMemberId)
              if (info != null) {
                AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.message_queue_info), text = queueInfoText(info))
              }
            }
          },
        )
      }
    }
  }
}
