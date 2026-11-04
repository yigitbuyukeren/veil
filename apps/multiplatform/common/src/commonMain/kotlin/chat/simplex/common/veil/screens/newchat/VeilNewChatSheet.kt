package chat.simplex.common.veil.screens.newchat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boofcv.alg.fiducial.qrcode.QrCode
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.veil.data.launchOrbot
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.chatlist.torRingStateFor
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Veil new-chat sheet (screen group D) for Android. Opened from the chat list's "Yeni" button (see
 * `veilShowNewChatSheet` in VeilChatActions.kt).
 *
 * Logic is upstream's: `controller.apiAddContact` creates the one-time invitation, `apiSetConnectionIncognito`
 * changes its profile mode, `strConnectTarget` + `planAndConnect` handle pasted and scanned links with all of upstream's
 * plans and alerts, `QRCodeScanner` is the camera view, `AddGroupView` is group creation. This file is layout and state.
 */

private const val TOR_PROBE_UNAVAILABLE_MS = 3_000L
private const val TOR_PROBE_OK_MS = 15_000L

/** Shows the sheet as a non-animated modal so the chat list stays visible under its scrim. */
fun veilShowNewChatSheetModal() {
  connectProgressManager.cancelConnectProgress()
  ModalManager.start.closeModals()
  ModalManager.end.closeModals()
  chatModel.newChatSheetVisible.value = true
  ModalManager.start.showCustomModal(animated = false) { close ->
    val closeSheet = {
      chatModel.newChatSheetVisible.value = false
      close()
    }
    DisposableEffect(Unit) {
      onDispose { chatModel.newChatSheetVisible.value = false }
    }
    VeilNewChatHost(chatModel.currentRemoteHost.value, closeSheet)
  }
}

@Composable
fun VeilNewChatHost(rh: RemoteHostInfo?, closeSheet: () -> Unit) {
  val rhId = rh?.remoteHostId
  val motion = VeilTheme.motion
  val ms = motion.lift(VeilTheme.reduceMotion)
  var page by rememberSaveable { mutableStateOf(NewChatPage.Menu) }
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }

  // Tor proxy probe (the same measurement as the chat list's ring: the local SOCKS listener only).
  var proxyAnswers by remember { mutableStateOf<Boolean?>(null) }
  LaunchedEffect(Unit) {
    while (true) {
      proxyAnswers = withContext(Dispatchers.IO) { VeilNetworkPolicy.torProxyReady() }
      delay(if (proxyAnswers == true) TOR_PROBE_OK_MS else TOR_PROBE_UNAVAILABLE_MS)
    }
  }
  val ring = torRingStateFor(proxyAnswers, chatModel.chatRunning.value)

  val closeAll = { ModalManager.start.closeModals() }
  val back = { if (page == NewChatPage.Menu) closeSheet() else page = NewChatPage.Menu }
  BackHandler(onBack = back)

  DisposableEffect(Unit) {
    onDispose {
      connectProgressManager.cancelConnectProgress()
      offerToKeepUnusedInvitation(rhId)
    }
  }

  val dismissLabel = stringResource(MR.strings.veil_new_close)
  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = closeSheet)
        .semantics { contentDescription = dismissLabel }
    )
    AnimatedVisibility(
      shown,
      Modifier.align(Alignment.BottomCenter),
      enter = slideInVertically(tween(ms, easing = motion.easeOut)) { it } + fadeIn(tween(ms)),
    ) {
      VeilSheet(
        Modifier
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
          .pointerInput(Unit) { detectTapGestures { } }
      ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          when (page) {
            NewChatPage.Menu -> MenuPage(
              onOpen = { page = it },
              onGroup = {
                ModalManager.start.showCustomModal { close -> AddGroupView(chatModel, chatModel.currentRemoteHost.value, close, closeAll) }
              }
            )
            NewChatPage.Invite -> InvitePage(rhId, ring, onBack = back)
            NewChatPage.Paste -> PastePage(rhId, onBack = back, closeAll = closeAll)
            NewChatPage.Scan -> ScanPage(rhId, onBack = back, closeAll = closeAll)
          }
        }
      }
    }
  }
}

/**
 * Upstream's `NewChatView` asks this when its invitation page goes away unused (the link was neither copied, shared
 * nor scanned): keep it in the chat list as a pending connection, or delete it.
 */
private fun offerToKeepUnusedInvitation(rhId: Long?) {
  val showing = chatModel.showingInvitation.value ?: return
  val conn = showing.conn
  if (!showing.connChatUsed) {
    AlertManager.shared.showAlertDialog(
      title = generalGetString(MR.strings.keep_unused_invitation_question),
      text = generalGetString(MR.strings.you_can_view_invitation_link_again),
      confirmText = generalGetString(MR.strings.delete_verb),
      dismissText = generalGetString(MR.strings.keep_invitation_link),
      destructive = true,
      onConfirm = {
        withBGApi {
          val chatInfo = ChatInfo.ContactConnection(conn)
          chatModel.controller.deleteChat(Chat(remoteHostId = rhId, chatInfo = chatInfo, chatItems = listOf()))
          if (chatModel.chatId.value == chatInfo.id) {
            chatModel.chatId.value = null
            ModalManager.start.closeModals()
          }
        }
      }
    )
  }
  chatModel.showingInvitation.value = null
}

// ---- menu ----

@Composable
private fun SheetHeader(title: String, onBack: (() -> Unit)?) {
  val colors = VeilTheme.colors
  Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    if (onBack != null) {
      VeilIconButton(onBack, stringResource(MR.strings.veil_new_back)) { VeilIcon(MR.images.ic_arrow_back_ios_new, colors.ink2, size = 20.dp) }
    }
    BasicText(title, Modifier.weight(1f), style = VeilTheme.type.sheetTitle.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun MenuPage(onOpen: (NewChatPage) -> Unit, onGroup: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  SheetHeader(stringResource(MR.strings.veil_new_title), null)
  BasicText(stringResource(MR.strings.veil_new_intro), Modifier.padding(bottom = 10.dp), style = type.caption.copy(color = colors.ink3, fontSize = 13.sp, lineHeight = 18.sp))
  for (entry in newChatEntries) {
    val (icon, title, sub) = when (entry) {
      NewChatEntry.Invite -> Triple(MR.images.ic_add_link, MR.strings.veil_new_entry_invite, MR.strings.veil_new_entry_invite_sub)
      NewChatEntry.Paste -> Triple(MR.images.ic_content_paste, MR.strings.veil_new_entry_paste, MR.strings.veil_new_entry_paste_sub)
      NewChatEntry.Scan -> Triple(MR.images.ic_qr_code_scanner, MR.strings.veil_new_entry_scan, MR.strings.veil_new_entry_scan_sub)
      NewChatEntry.Group -> Triple(MR.images.ic_group, MR.strings.veil_new_entry_group, MR.strings.veil_new_entry_group_sub)
    }
    MenuRow(icon, stringResource(title), stringResource(sub)) {
      val target = entry.page()
      if (target != null) onOpen(target) else onGroup()
    }
  }
}

@Composable
private fun MenuRow(icon: ImageResource, title: String, sub: String, onClick: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 60.dp)
      .clip(VeilTheme.shapes.input)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Box(Modifier.size(42.dp).clip(VeilTheme.shapes.input).background(colors.sheet2), contentAlignment = Alignment.Center) {
      VeilIcon(icon, colors.ink, size = 22.dp)
    }
    Column(Modifier.weight(1f)) {
      BasicText(title, style = type.rowTitle.copy(color = colors.ink))
      BasicText(sub, style = type.caption.copy(color = colors.ink3, fontSize = 13.sp))
    }
  }
}

// ---- invitation ----

@Composable
private fun InvitePage(rhId: Long?, ring: TorRingState, onBack: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val clipboard = LocalClipboardManager.current
  val showing = chatModel.showingInvitation.value
  val connLink = showing?.connLink?.takeIf { it.connFullLink.isNotEmpty() }
  var creating by remember { mutableStateOf(false) }
  var failed by remember { mutableStateOf(false) }
  var copied by remember { mutableStateOf(false) }

  fun create() {
    if (creating) return
    creating = true
    failed = false
    withBGApi {
      val (r, alert) = chatModel.controller.apiAddContact(rhId, incognito = appPrefs.incognito.get())
      if (r != null) {
        withContext(Dispatchers.Main) {
          chatModel.chatsContext.updateContactConnection(rhId, r.second)
          chatModel.showingInvitation.value = ShowingInvitation(connId = r.second.id, connLink = r.first, connChatUsed = false, conn = r.second)
        }
      } else {
        failed = true
        alert?.invoke()
      }
      creating = false
    }
  }

  LaunchedEffect(ring, connLink == null, creating, failed) {
    if (shouldCreateInvitation(ring, connLink != null, creating, failed)) create()
  }
  LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }

  val state = inviteViewState(ring, connLink != null, creating, failed)
  val ready = canShareInvitation(state)
  val link = connLink?.simplexChatUri(short = true) ?: ""

  SheetHeader(stringResource(MR.strings.veil_new_invite_title), onBack)
  when (state) {
    InviteViewState.TorUnavailable -> {
      WarnNote(stringResource(MR.strings.veil_new_invite_tor_down))
      Spacer(Modifier.height(12.dp))
      VeilButton(stringResource(MR.strings.veil_new_invite_open_orbot), onClick = { launchOrbot() }, style = VeilButtonStyle.Secondary)
    }
    InviteViewState.Creating -> StatusLine(stringResource(MR.strings.veil_new_invite_creating))
    InviteViewState.Failed -> {
      StatusLine(stringResource(MR.strings.veil_new_invite_failed))
      Spacer(Modifier.height(12.dp))
      VeilButton(stringResource(MR.strings.veil_new_invite_retry), onClick = { failed = false }, style = VeilButtonStyle.Secondary, enabled = canRetryInvitation(ring, creating))
    }
    InviteViewState.Ready -> {
      Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
        InviteQr(connLink!!)
      }
      Box(
        Modifier
          .fillMaxWidth()
          .clip(VeilTheme.shapes.card)
          .background(colors.sheet2)
          .border(1.dp, colors.line, VeilTheme.shapes.card)
          .padding(horizontal = 14.dp, vertical = 12.dp)
      ) {
        BasicText(
          link,
          style = type.caption.copy(color = colors.ink2, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, lineHeight = 17.sp),
          maxLines = 2, overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
  if (state == InviteViewState.Ready || state == InviteViewState.Creating) {
    BasicText(
      stringResource(MR.strings.veil_new_invite_note),
      Modifier.padding(start = 2.dp, end = 2.dp, top = 12.dp),
      style = type.body.copy(color = colors.ink2, fontSize = 13.5.sp, lineHeight = 19.sp)
    )
    BasicText(
      stringResource(MR.strings.veil_new_invite_route),
      Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 12.dp),
      style = type.caption.copy(color = colors.ink3, fontSize = 12.5.sp, lineHeight = 17.sp)
    )
    IncognitoRow(
      incognito = invitationIncognito(showing?.conn?.incognito, appPrefs.incognito.state.value),
      enabled = state == InviteViewState.Ready,
      onChange = { v -> setInvitationIncognito(rhId, v) }
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      VeilButton(
        stringResource(if (copied) MR.strings.veil_new_copied else MR.strings.veil_new_copy),
        onClick = {
          chatModel.markShowingInvitationUsed()
          clipboard.setText(AnnotatedString(link))
          copied = true
        },
        modifier = Modifier.weight(1f),
        style = VeilButtonStyle.Secondary,
        enabled = ready,
        leading = { VeilIcon(MR.images.ic_content_copy, colors.ink, size = 18.dp) }
      )
      VeilButton(
        stringResource(MR.strings.veil_new_share),
        onClick = {
          chatModel.markShowingInvitationUsed()
          clipboard.shareText(link)
        },
        modifier = Modifier.weight(1f),
        enabled = ready,
      )
    }
  }
}

/**
 * The invitation as upstream's QR (`qrCodeBitmap`): dark modules on white with a quiet zone, no logo, no tint and no
 * gradient, so any scanner reads it. Only the frame around it is Veil's.
 */
@Composable
private fun InviteQr(connLink: CreatedConnLink) {
  val colors = VeilTheme.colors
  val short = connLink.connShortLink != null
  val content = connLink.simplexChatUri(short = true)
  val bitmap = remember(content) { qrCodeBitmap(content, 1024, if (short) QrCode.ErrorLevel.M else QrCode.ErrorLevel.L) }
  val frame = VeilTheme.shapes.card
  Box(
    Modifier
      .size(220.dp)
      .clip(frame)
      .background(Color.White)
      .border(1.dp, colors.line2, frame)
      .padding(16.dp)
  ) {
    Image(bitmap, contentDescription = stringResource(MR.strings.veil_new_invite_qr_desc), Modifier.fillMaxSize())
  }
}

/**
 * Same call upstream's profile picker makes for the incognito option: a pending connection can be switched between
 * incognito and the current profile while it is new. Without a connection yet only the preference changes.
 */
private fun setInvitationIncognito(rhId: Long?, incognito: Boolean) {
  val conn = chatModel.showingInvitation.value?.conn
  if (conn == null) {
    appPrefs.incognito.set(incognito)
    return
  }
  withBGApi {
    val updated = chatModel.controller.apiSetConnectionIncognito(rhId, conn.pccConnId, incognito)
    if (updated != null) {
      appPrefs.incognito.set(incognito)
      withContext(Dispatchers.Main) {
        chatModel.chatsContext.updateContactConnection(rhId, updated)
        val current = chatModel.showingInvitation.value
        if (current != null) {
          chatModel.showingInvitation.value = current.copy(conn = updated, connLink = updated.connLinkInv ?: current.connLink)
        }
      }
    }
  }
}

@Composable
private fun IncognitoRow(incognito: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val name = remember { chatModel.currentUser }.value?.displayName ?: ""
  Row(
    Modifier
      .fillMaxWidth()
      .clip(VeilTheme.shapes.card)
      .background(colors.sheet1)
      .border(1.dp, colors.line, VeilTheme.shapes.card)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    VeilIcon(if (incognito) MR.images.ic_theater_comedy_filled else MR.images.ic_theater_comedy, if (incognito) colors.iris else colors.ink3, size = 24.dp)
    Column(Modifier.weight(1f)) {
      BasicText(stringResource(MR.strings.veil_new_incognito_title), style = type.rowTitle.copy(color = colors.ink, fontSize = 15.sp))
      BasicText(
        if (incognito) stringResource(MR.strings.veil_new_incognito_on) else String.format(stringResource(MR.strings.veil_new_incognito_off), name),
        style = type.caption.copy(color = colors.ink3, fontSize = 12.5.sp, lineHeight = 17.sp)
      )
    }
    VeilSwitch(incognito, onChange, enabled = enabled)
  }
}

@Composable
private fun WarnNote(text: String) {
  val colors = VeilTheme.colors
  val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
  Box(
    Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.warn.copy(alpha = 0.08f))
      .border(1.dp, colors.warn.copy(alpha = 0.35f), shape)
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    BasicText(text, style = VeilTheme.type.caption.copy(color = colors.warn, fontSize = 13.sp, lineHeight = 18.sp))
  }
}

@Composable
private fun StatusLine(text: String) {
  BasicText(
    text,
    Modifier.fillMaxWidth().padding(vertical = 28.dp),
    style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink2, textAlign = TextAlign.Center)
  )
}

// ---- paste ----

@Composable
private fun PastePage(rhId: Long?, onBack: () -> Unit, closeAll: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val clipboard = LocalClipboardManager.current
  var text by rememberSaveable { mutableStateOf("") }
  val connecting = connectProgressManager.showConnectProgress != null

  fun connect() {
    val target = strConnectTarget(cleanPastedLink(text))
    if (target == null) {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(MR.strings.invalid_contact_link),
        text = generalGetString(MR.strings.the_text_you_pasted_is_not_a_link)
      )
      return
    }
    val link = when (target) {
      is ConnectTarget.Link -> target.text
      is ConnectTarget.Name -> target.text
    }
    text = link
    withBGApi { planAndConnect(rhId, link, close = closeAll, cleanup = { text = "" }) }
  }

  SheetHeader(stringResource(MR.strings.veil_new_paste_title), onBack)
  BasicText(stringResource(MR.strings.veil_new_paste_intro), Modifier.padding(bottom = 14.dp), style = type.body.copy(color = colors.ink2, fontSize = 14.sp))
  VeilTextField(
    value = text,
    onValueChange = { text = it },
    placeholder = stringResource(MR.strings.veil_new_paste_placeholder),
    singleLine = true,
    trailing = {
      VeilIconButton(
        onClick = { clipboard.getText()?.text?.let { text = cleanPastedLink(it) } },
        contentDescription = stringResource(MR.strings.veil_new_paste_from_clipboard),
      ) { VeilIcon(MR.images.ic_content_paste, colors.ink2, size = 22.dp) }
    }
  )
  Spacer(Modifier.height(10.dp))
  IncognitoPrefRow()
  Spacer(Modifier.height(14.dp))
  VeilButton(
    stringResource(if (connecting) MR.strings.veil_new_connecting else MR.strings.veil_new_connect),
    onClick = { connect() },
    enabled = pasteInputReady(text, connecting),
  )
}

/** Incognito row for the connect pages: it sets the preference upstream's connect flow reads when the link is opened. */
@Composable
private fun IncognitoPrefRow() {
  IncognitoRow(incognito = appPrefs.incognito.state.value, enabled = true, onChange = { appPrefs.incognito.set(it) })
}

// ---- scan ----

@Composable
private fun ScanPage(rhId: Long?, onBack: () -> Unit, closeAll: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val showScanner = remember { mutableStateOf(true) }
  SheetHeader(stringResource(MR.strings.veil_new_scan_title), onBack)
  BasicText(stringResource(MR.strings.veil_new_scan_intro), Modifier.padding(bottom = 14.dp), style = type.body.copy(color = colors.ink2, fontSize = 14.sp))
  Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Box(
      Modifier
        .widthIn(max = 320.dp)
        .fillMaxWidth()
        .clip(VeilTheme.shapes.card)
        .background(colors.night2)
        .border(1.dp, colors.line2, VeilTheme.shapes.card)
    ) {
      // Camera permission handling is the scanner's own: it asks once, then offers a button.
      QRCodeScanner(showScanner, padding = PaddingValues(0.dp)) { scanned ->
        if (!strIsSimplexLink(scanned)) {
          AlertManager.shared.showAlertMsg(
            title = generalGetString(MR.strings.invalid_qr_code),
            text = generalGetString(MR.strings.code_you_scanned_is_not_simplex_link_qr_code)
          )
          false
        } else {
          withContext(Dispatchers.Default) { planAndConnect(rhId, scanned, close = closeAll, cleanup = null).await() }
        }
      }
    }
  }
  BasicText(stringResource(MR.strings.veil_new_scan_camera_note), Modifier.padding(start = 2.dp, end = 2.dp, top = 12.dp, bottom = 12.dp), style = type.caption.copy(color = colors.ink3, fontSize = 12.5.sp))
  IncognitoPrefRow()
}
