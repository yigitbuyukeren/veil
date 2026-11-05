package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.settings.VeilSectionLabel
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chatlist.updateChatSettings
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Veil "Gelişmiş" page of a direct contact: replaces upstream's complete `ChatInfoView` behind the contact info's
 * "Advanced" row.
 *
 * What upstream's screen does, and where it is now:
 * - alias, security code, notifications, contact preferences, clear chat, delete contact, incognito profile: already on
 *   the Veil contact info (VeilChatInfo.kt), with the same calls and dialogs.
 * - delivery receipts: `updateChatSettings` with `ChatSettings.sendRcpts` (here).
 * - delete messages after: `setChatTTLAlert` (upstream's confirmation) then `controller.setChatTTL` (here).
 * - encryption line, contact address with QR and share, network status alert, server lists (copy on tap) (here).
 * - change / abort changing receiving address: `showSwitchAddressAlert` + `apiSwitchContact`,
 *   `showAbortSwitchAddressAlert` + `apiAbortSwitchContact` (here, upstream's dialogs).
 * - fix connection: `syncContactConnection` -> `apiSyncContactRatchet(force = false)` (here).
 * - developer tools: local name, database id, `apiContactQueueInfo` (here, only with developer tools on).
 * - not offered: search / audio / video buttons (calls are off by default and the Veil chat has its own search),
 *   chat theme editor (the Veil chat draws its own background, so a wallpaper would have no effect).
 */
@Composable
fun VeilContactAdvancedScreen(rhId: Long?, contactId: Long, connectionStats: ConnectionStats?, close: () -> Unit) {
  val chat = remember(contactId) { derivedStateOf { chatModel.getContactChat(contactId) } }.value
  val contact = (chat?.chatInfo as? ChatInfo.Direct)?.contact
  val user = remember { chatModel.currentUser }.value
  if (chat == null || contact == null || user == null) {
    // the contact was deleted while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  val chatRh = chat.remoteHostId
  val clipboard = LocalClipboardManager.current
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  val connStats = remember(contactId, connectionStats) { mutableStateOf(connectionStats) }
  val sendReceipts = remember(contact.id) { mutableStateOf(SendReceipts.fromBool(contact.chatSettings.sendRcpts, user.sendRcptsContacts)) }
  val chatItemTTL = remember(contact.id) { mutableStateOf(if (contact.chatItemTTL != null) ChatItemTTL.fromSeconds(contact.chatItemTTL) else null) }
  val deletingItems = rememberSaveable(contact.id) { mutableStateOf(false) }
  val cStats = connStats.value
  val blocks = advancedBlocks(
    ready = contact.ready,
    active = contact.active,
    ratchetSyncAllowed = cStats != null && cStats.ratchetSyncAllowed,
    hasContactLink = contact.contactLink != null,
    hasActiveConn = contact.activeConn != null,
    developerTools = developerTools,
  )
  val queues = cStats?.rcvQueuesInfo?.map { QueueSwitch(it.rcvSwitchStatus != null, it.canAbortSwitch) } ?: emptyList()

  VeilScreenScaffold(title = stringResource(MR.strings.veil_chatinfo_advanced), onBack = close, subtitle = contact.displayName) {
    // ---- messages ----
    VeilSectionLabel(stringResource(MR.strings.veil_cadv_messages))
    VeilCard(Modifier.padding(bottom = 6.dp), contentPadding = PaddingValues(8.dp)) {
      if (blocks.sendReceipts) {
        VeilChoiceRow(
          title = stringResource(MR.strings.veil_cadv_receipts),
          options = listOf(SendReceipts.Yes, SendReceipts.No, SendReceipts.UserDefault(user.sendRcptsContacts)).map { it to it.text },
          selected = sendReceipts.value,
          onSelect = { sendRcpts ->
            val chatSettings = (chat.chatInfo.chatSettings ?: ChatSettings.defaults).copy(sendRcpts = sendRcpts.bool)
            updateChatSettings(chatRh, chat.chatInfo, chatSettings, chatModel)
            sendReceipts.value = sendRcpts
          },
        )
      }
      VeilChoiceRow(
        title = stringResource(MR.strings.veil_cadv_ttl),
        options = chatTtlOptions(chatItemTTL.value, hasDefault = true).map { ttl ->
          ttl to (ttl?.text ?: String.format(generalGetString(MR.strings.chat_item_ttl_default), chatModel.chatItemTTL.value.text))
        },
        selected = chatItemTTL.value,
        enabled = !deletingItems.value,
        onSelect = { selected ->
          if (!chatTtlChanged(selected, chatItemTTL.value)) return@VeilChoiceRow
          val previous = chatItemTTL.value
          chatItemTTL.value = selected
          // upstream's confirmation; it applies the change or puts the previous value back
          setChatTTLAlert(chatModel.chatsContext, chatRh, chat.chatInfo, chatItemTTL, previous, deletingItems)
        },
      )
    }
    VeilCaption(stringResource(if (deletingItems.value) MR.strings.veil_cadv_ttl_working else MR.strings.veil_cadv_ttl_note), Modifier.padding(bottom = 10.dp))

    // ---- encryption ----
    val conn = contact.activeConn
    if (blocks.encryption && conn != null) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_encryption))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        VeilInfoRow(
          icon = MR.images.ic_lock,
          title = stringResource(MR.strings.veil_cadv_e2e),
          value = stringResource(if (conn.connPQEnabled) MR.strings.veil_cadv_e2e_pq else MR.strings.veil_cadv_e2e_standard),
        )
        if (blocks.fixConnection) {
          VeilInfoRow(
            icon = MR.images.ic_sync_problem,
            title = stringResource(MR.strings.veil_cadv_fix),
            value = stringResource(MR.strings.veil_cadv_fix_note),
            onClick = {
              withBGApi {
                syncContactConnection(chatRh, contact, connStats, force = false)
                close()
              }
            },
          )
        }
      }
    }

    // ---- address ----
    val link = contact.contactLink
    if (blocks.address && link != null) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_address))
      VeilCard(Modifier.padding(bottom = 6.dp), contentPadding = PaddingValues(8.dp)) {
        VeilQr(simplexChatLink(link), small = link.count() < 200, description = stringResource(MR.strings.veil_cadv_address_qr))
        VeilInfoRow(
          icon = MR.images.ic_share,
          title = stringResource(MR.strings.veil_cadv_address_share),
          onClick = { clipboard.shareText(simplexChatLink(link)) },
        )
      }
      VeilCaption(String.format(stringResource(MR.strings.veil_cadv_address_note), contact.displayName), Modifier.padding(bottom = 10.dp))
    }

    // ---- servers ----
    if (blocks.servers) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_servers))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        val subStatus = chatModel.chatSubStatus.value
        if (subStatus != null) {
          VeilInfoRow(
            icon = MR.images.ic_info,
            title = stringResource(MR.strings.veil_cadv_status),
            value = subStatus.statusString,
            onClick = { AlertManager.shared.showAlertMsg(generalGetString(MR.strings.network_status), subStatus.statusExplanation) },
          )
        }
        if (cStats != null) {
          val syncProhibited = cStats.ratchetSyncSendProhibited
          val switchOff = switchAddressDisabled(queues, syncProhibited)
          VeilInfoRow(
            icon = MR.images.ic_swap_horizontal_circle,
            title = stringResource(MR.strings.veil_cadv_switch),
            modifier = Modifier.alpha(if (switchOff) 0.38f else 1f),
            onClick = if (switchOff) null else ({
              showSwitchAddressAlert(switchAddress = {
                withBGApi {
                  val stats = chatModel.controller.apiSwitchContact(chatRh, contact.contactId)
                  connStats.value = stats
                  if (stats != null) {
                    withContext(Dispatchers.Main) { chatModel.chatsContext.updateContactConnectionStats(chatRh, contact, stats) }
                  }
                  close()
                }
              })
            }),
          )
          if (abortSwitchShown(queues)) {
            val abortOff = abortSwitchDisabled(queues, syncProhibited)
            VeilInfoRow(
              icon = MR.images.ic_close,
              title = stringResource(MR.strings.veil_cadv_abort),
              modifier = Modifier.alpha(if (abortOff) 0.38f else 1f),
              onClick = if (abortOff) null else ({
                showAbortSwitchAddressAlert(abortSwitchAddress = {
                  withBGApi {
                    val stats = chatModel.controller.apiAbortSwitchContact(chatRh, contact.contactId)
                    connStats.value = stats
                    if (stats != null) {
                      withContext(Dispatchers.Main) { chatModel.chatsContext.updateContactConnectionStats(chatRh, contact, stats) }
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
    }

    // ---- developer tools ----
    if (blocks.developer) {
      VeilSectionLabel(stringResource(MR.strings.veil_cadv_developer))
      VeilCard(Modifier.padding(bottom = 14.dp), contentPadding = PaddingValues(8.dp)) {
        VeilInfoRow(icon = MR.images.ic_info, title = stringResource(MR.strings.veil_cadv_local_name), value = chat.chatInfo.localDisplayName)
        VeilInfoRow(icon = MR.images.ic_info, title = stringResource(MR.strings.veil_cadv_db_id), value = chat.chatInfo.apiId.toString())
        VeilInfoRow(
          icon = MR.images.ic_info,
          title = stringResource(MR.strings.veil_cadv_queue_info),
          onClick = {
            withBGApi {
              val info = chatModel.controller.apiContactQueueInfo(chatRh, chat.chatInfo.apiId)
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
