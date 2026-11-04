package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boofcv.alg.fiducial.qrcode.QrCode
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.avatarSeed
import chat.simplex.common.views.chat.ScanCodeView
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.qrCodeBitmap
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/*
 * Güvenlik kodu (security code) screen. The data and the actions are upstream's: the code comes from
 * `apiGetContactCode` / `apiGetGroupMemberCode`, "mark verified" and "clear" are `apiVerifyContact` /
 * `apiVerifyGroupMember` with the same model updates `ChatInfoView` / `GroupMemberInfoView` do, and scanning uses
 * upstream's `ScanCodeView`.
 */

/**
 * The screen. [verify] is the upstream verify call (`null` clears, the code marks verified) and returns
 * (verified, expected code) or null when it failed. [connectionVerified] must be live state of the contact / member.
 */
@Composable
fun VeilVerifyCodeView(
  displayName: String,
  avatarSeed: String,
  incognito: Boolean,
  connectionCode: String,
  connectionVerified: Boolean,
  verify: suspend (String?) -> Pair<Boolean, String>?,
  close: () -> Unit,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val rows = remember(connectionCode) { securityCodeRows(connectionCode) }
  // Same contract as upstream's VerifyCodeLayout.verifyCode: true when the code is now verified.
  val verifyCode: suspend (String?) -> Boolean = { code ->
    val res = verify(code)
    res?.first == true
  }
  VeilScreenScaffold(
    title = stringResource(MR.strings.veil_verify_title),
    subtitle = String.format(stringResource(MR.strings.veil_verify_subtitle), displayName),
    onBack = close,
  ) {
    VeilCard(Modifier.padding(bottom = 14.dp), layer = VeilLayer.Mid) {
      Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        MaskAvatar(avatarSeed, 52.dp, incognito = incognito)
        val line = colors.line2
        Box(
          Modifier
            .padding(horizontal = 10.dp)
            .size(width = 56.dp, height = 2.dp)
            .drawBehind {
              drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
            }
        )
        val me = chatModel.currentUser.value
        MaskAvatar(avatarSeed("user${me?.userId}", me?.profile?.displayName ?: ""), 52.dp)
      }
      Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
        if (connectionVerified) VeilChip(stringResource(MR.strings.veil_chat_verified), style = VeilChipStyle.Gradient)
        else VeilChip(stringResource(MR.strings.veil_verify_unverified))
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (group in row) {
              Box(
                Modifier.weight(1f).clip(VeilTheme.shapes.input).background(colors.sheet1).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
              ) {
                BasicText(group, style = type.body.copy(color = colors.ink, fontFamily = FontFamily.Monospace, fontSize = 15.sp, textAlign = TextAlign.Center), maxLines = 1)
              }
            }
            // keep the last row's tiles the same width as the others
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
          }
        }
      }
    }
    Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
      val qr = remember(connectionCode) { qrCodeBitmap(connectionCode, 600, QrCode.ErrorLevel.M) }
      Image(
        bitmap = qr,
        contentDescription = stringResource(MR.strings.veil_verify_qr_description),
        modifier = Modifier.size(112.dp).clip(VeilTheme.shapes.input).background(androidx.compose.ui.graphics.Color.White),
      )
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(stringResource(MR.strings.veil_verify_qr_hint), style = type.body.copy(color = colors.ink2, fontSize = 14.sp))
        if (!connectionVerified) {
          VeilButton(
            stringResource(MR.strings.veil_verify_scan),
            onClick = {
              ModalManager.end.showModal {
                ScanCodeView(verifyCode) { ModalManager.end.closeModal() }
              }
            },
            style = VeilButtonStyle.Secondary,
            fillMaxWidth = false,
          )
        }
      }
    }
    VeilCard(Modifier.padding(bottom = 14.dp)) {
      BasicText(stringResource(MR.strings.veil_verify_explain), style = type.body.copy(color = colors.ink, fontSize = 14.5.sp))
      BasicText(
        stringResource(MR.strings.veil_verify_explain_channel),
        Modifier.padding(top = 8.dp),
        style = type.caption.copy(color = colors.ink3, fontSize = 12.5.sp, lineHeight = 17.sp),
      )
    }
    if (connectionVerified) {
      VeilButton(stringResource(MR.strings.veil_verify_clear), onClick = { withApi { verifyCode(null) } }, style = VeilButtonStyle.Secondary)
    } else {
      VeilButton(
        stringResource(MR.strings.veil_verify_mark),
        onClick = {
          withApi {
            if (!verifyCode(connectionCode)) {
              AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.incorrect_code))
            }
          }
        },
        style = VeilButtonStyle.Primary,
      )
    }
  }
}

/** Contact verification, the same call and model update as the `verify` lambda in upstream's `ChatInfoView`. */
suspend fun veilVerifyContact(rhId: Long?, ct: Contact, code: String?): Pair<Boolean, String>? =
  chatModel.controller.apiVerifyContact(rhId, ct.contactId, code)?.let { r ->
    val (verified, existingCode) = r
    withContext(Dispatchers.Main) {
      chatModel.chatsContext.updateContact(
        rhId,
        ct.copy(activeConn = ct.activeConn?.copy(connectionCode = if (verified) SecurityCode(existingCode, Clock.System.now()) else null))
      )
    }
    r
  }

/** Group member verification, the same call and model updates as the `verify` lambda in upstream's `GroupMemberInfoView`. */
suspend fun veilVerifyMember(rhId: Long?, groupInfo: GroupInfo, mem: GroupMember, code: String?): Pair<Boolean, String>? =
  chatModel.controller.apiVerifyGroupMember(rhId, mem.groupId, mem.groupMemberId, code)?.let { r ->
    val (verified, existingCode) = r
    val sec = if (verified) SecurityCode(existingCode, Clock.System.now()) else null
    val copy = if (groupInfo.useRelays) mem.copy(memberVerifiedCode = sec) else mem.copy(activeConn = mem.activeConn?.copy(connectionCode = sec))
    withContext(Dispatchers.Main) { chatModel.chatsContext.upsertGroupMember(rhId, groupInfo, copy) }
    withContext(Dispatchers.Main) { chatModel.secondaryChatsContext.value?.upsertGroupMember(rhId, groupInfo, copy) }
    r
  }

/** Opens the security code screen of a direct contact. [code] is the connection code from `apiGetContactCode`. */
fun veilShowContactSecurityCode(rhId: Long?, contactId: Long, code: String) {
  ModalManager.end.showCustomModal { close ->
    val contact = remember { derivedStateOf { (chatModel.getContactChat(contactId)?.chatInfo as? ChatInfo.Direct)?.contact } }.value
    if (contact != null) {
      VeilVerifyCodeView(
        displayName = contact.displayName,
        avatarSeed = chatAvatarSeed(contact.id, contact.profile.displayName),
        incognito = contact.contactConnIncognito,
        connectionCode = code,
        connectionVerified = contact.verified,
        verify = { c -> veilVerifyContact(rhId, contact, c) },
        close = close,
      )
    }
  }
}

/** Opens the security code screen of a group member (called from upstream's member info). */
fun veilShowMemberSecurityCode(rhId: Long?, groupInfo: GroupInfo, member: GroupMember, code: String) {
  ModalManager.end.showCustomModal { close ->
    val mem = remember { derivedStateOf { chatModel.getGroupMember(member.groupMemberId) } }.value
    if (mem != null) {
      VeilVerifyCodeView(
        displayName = mem.displayName,
        avatarSeed = chatAvatarSeed(mem.id, mem.memberProfile.displayName),
        incognito = false,
        connectionCode = code,
        connectionVerified = mem.verified,
        verify = { c -> veilVerifyMember(rhId, groupInfo, mem, c) },
        close = close,
      )
    }
  }
}

/**
 * Tap on the verification chip in the chat top bar ([VeilChatHooks.onVerifyCode] default): fetches the code and opens
 * the screen. When the contact has no code yet (connection not ready), the chat info opens instead.
 */
fun veilOpenSecurityCode(chatInfo: ChatInfo, openInfo: () -> Unit) {
  val contact = (chatInfo as? ChatInfo.Direct)?.contact
  if (contact == null || !contact.ready || !contact.active) {
    openInfo()
    return
  }
  val rhId = chatModel.getChat(chatInfo.id)?.remoteHostId
  withBGApi {
    val code = chatModel.controller.apiGetContactCode(rhId, contact.contactId)?.second
    if (code != null) veilShowContactSecurityCode(rhId, contact.contactId, code) else openInfo()
  }
}
