package chat.simplex.common.veil.screens.chatlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.chatlist.UserPicker
import chat.simplex.common.views.chatlist.filteredChats
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.onboarding.SetNotificationsModeAdditions
import chat.simplex.common.views.usersettings.VeilStatusView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Veil chat list (screen group B) for Android. Routed from `StartPartOfScreen` in App.kt when `VeilUi.chatList` is on.
 *
 * Data, search, actions and navigation are upstream's (see VeilChatActions.kt); this file is layout and state only.
 */
@Composable
fun VeilChatListScreen(
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  var filter by rememberSaveable { mutableStateOf(VeilListFilter.All) }
  var searching by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var sheetChatId by rememberSaveable { mutableStateOf<String?>(null) }
  val revealed = remember { mutableStateListOf<String>() }
  val scope = rememberCoroutineScope()

  // Tor proxy probe: every 15 s and when the app comes back to the foreground. Measures the local SOCKS listener only.
  var proxyAnswers by remember { mutableStateOf<Boolean?>(null) }
  LaunchedEffect(Unit) {
    var wasVisible = isAppVisibleAndFocused()
    var sinceProbe = VEIL_TOR_PROBE_INTERVAL_MS
    while (true) {
      val visible = isAppVisibleAndFocused()
      if (shouldDropReveals(wasVisible, visible)) revealed.clear()
      val resumed = visible && !wasVisible
      wasVisible = visible
      if (visible && (resumed || sinceProbe >= VEIL_TOR_PROBE_INTERVAL_MS)) {
        proxyAnswers = withContext(Dispatchers.IO) { VeilNetworkPolicy.torProxyReady() }
        sinceProbe = 0
      }
      delay(1000)
      sinceProbe += 1000
    }
  }
  val ring = torRingStateFor(proxyAnswers, chatModel.chatRunning.value)

  val allChats = chatModel.chats.value
  val showPreviews = chatModel.showChatPreviews.value
  val draft = chatModel.draft.value
  val draftChatId = chatModel.draftChatId.value
  val previewOf = { c: Chat -> veilPreviewOf(c, showPreviews, draft, draftChatId) }
  val chats = veilVisibleChats(allChats, filter, query) { c -> previewOf(c).let { p -> if (p.sensitive) p.text else null } }
  val unreadCount = veilVisibleChats(allChats, VeilListFilter.Unread, "") { null }.size
  val empty = listEmptyKind(allChats.size, chats.size, filter, query)

  Box(Modifier.fillMaxSize().background(colors.night).veilGrain()) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
      VeilTopBar(
        title = stringResource(MR.strings.veil_list_title),
        subtitle = {
          BasicText(
            torRingLabel(ring),
            style = type.caption.copy(color = if (ring == TorRingState.Unavailable) colors.warn else colors.ink2),
            maxLines = 1, overflow = TextOverflow.Ellipsis
          )
        },
        actions = {
          VeilIconButton(
            onClick = { ModalManager.start.showModal(settings = true) { VeilStatusView() } },
            contentDescription = stringResource(MR.strings.veil_list_tor_ring_open)
          ) { TorRing(ring, 30.dp) }
          VeilIconButton(
            onClick = { searching = !searching; if (!searching) query = "" },
            contentDescription = stringResource(if (searching) MR.strings.veil_list_search_close else MR.strings.veil_list_search_open)
          ) { VeilIcon(if (searching) MR.images.ic_close else MR.images.ic_search, colors.ink2, size = 22.dp) }
          VeilIconButton(
            onClick = { userPickerState.value = AnimatedViewState.VISIBLE },
            contentDescription = stringResource(MR.strings.veil_list_settings)
          ) { VeilIcon(MR.images.ic_settings, colors.ink2, size = 22.dp) }
        }
      )
      if (ring == TorRingState.Unavailable) TorBanner()
      if (searching) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        BackHandler { searching = false; query = "" }
        VeilTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.padding(horizontal = VeilGutter).padding(bottom = 8.dp).focusRequester(focus),
          placeholder = stringResource(MR.strings.veil_list_search_placeholder),
        )
      }
      FilterRow(
        selected = filter,
        unreadCount = unreadCount,
        onSelect = { filter = it },
      )
      Box(Modifier.weight(1f).fillMaxWidth()) {
        when {
          chatModel.chatRunning.value == null -> Centered { BasicText(stringResource(MR.strings.veil_list_loading), style = type.body.copy(color = colors.ink3)) }
          empty != VeilListEmpty.None -> EmptyState(empty)
          else -> {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
              items(chats, key = { it.remoteHostId to it.id }) { chat ->
                VeilChatRow(
                  chat = chat,
                  preview = previewOf(chat),
                  eyeOn = chat.id in revealed,
                  onToggleEye = { if (chat.id in revealed) revealed.remove(chat.id) else revealed.add(chat.id) },
                  onOpen = { veilOpenChat(chat, scope) },
                  onAvatarTap = { sheetChatId = chat.id },
                )
              }
            }
          }
        }
      }
    }
    NewChatButton(
      Modifier.align(Alignment.BottomEnd).navigationBarsPadding().imePadding().padding(end = 20.dp, bottom = 24.dp),
      onClick = {
        if (stopped) {
          AlertManager.shared.showAlertMsg(
            generalGetString(MR.strings.chat_is_stopped_indication),
            generalGetString(MR.strings.you_can_start_chat_via_setting_or_by_restarting_the_app)
          )
        } else {
          veilShowNewChatSheet()
        }
      }
    )
    val sheetChat = sheetChatId?.let { id -> allChats.firstOrNull { it.id == id } }
    LaunchedEffect(sheetChatId, sheetChat == null) { if (sheetChatId != null && sheetChat == null) sheetChatId = null }
    ActionsSheet(sheetChat) { sheetChatId = null }
  }

  // Same host as upstream's list: notification-mode prompts and the user picker (profiles, settings entry).
  val wasAllowedToSetupNotifications = rememberSaveable { mutableStateOf(false) }
  val canEnableNotifications = remember { derivedStateOf { chatModel.chatRunning.value == true } }
  if (wasAllowedToSetupNotifications.value || canEnableNotifications.value) {
    SetNotificationsModeAdditions()
    LaunchedEffect(Unit) { wasAllowedToSetupNotifications.value = true }
  }
  tryOrShowError("VeilUserPicker", error = {}) {
    UserPicker(chatModel = chatModel, userPickerState = userPickerState, setPerformLA = setPerformLA)
  }
}

private val noLinkSearch = mutableStateOf(false)
private val noLinkChatIds = mutableStateOf<Set<String>>(emptySet())

/**
 * Chats to list: upstream's `filteredChats` for the filter pill and for name search, plus a preview text match, minus
 * the upstream team card.
 */
internal fun veilVisibleChats(all: List<Chat>, filter: VeilListFilter, query: String, previewOf: (Chat) -> String?): List<Chat> {
  val candidates = filteredChats(noLinkSearch, noLinkChatIds, "", all, filter.toActiveFilter()).filter { !VeilBranding.isUpstreamTeamCard(it) }
  if (foldForSearch(query).isEmpty()) return candidates
  val byName = filteredChats(noLinkSearch, noLinkChatIds, query, all, null).mapTo(HashSet()) { it.id }
  val byId = candidates.associateBy { it.id }
  return searchResultIds(candidates.map { it.id }, byName, { id -> byId[id]?.let(previewOf) }, query).mapNotNull { byId[it] }
}

@Composable
private fun TorBanner() {
  val colors = VeilTheme.colors
  val shape = RoundedCornerShape(16.dp)
  Box(
    Modifier
      .padding(start = VeilGutter, end = VeilGutter, bottom = 8.dp)
      .fillMaxWidth()
      .clip(shape)
      .background(colors.warn.copy(alpha = 0.08f))
      .border(1.dp, colors.warn.copy(alpha = 0.35f), shape)
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    BasicText(stringResource(MR.strings.veil_list_tor_banner), style = VeilTheme.type.caption.copy(color = colors.warn, fontSize = 13.sp, lineHeight = 18.sp))
  }
}

@Composable
private fun FilterRow(selected: VeilListFilter, unreadCount: Int, onSelect: (VeilListFilter) -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(start = VeilGutter, end = VeilGutter, top = 4.dp, bottom = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FilterPill(stringResource(MR.strings.veil_list_filter_all), selected == VeilListFilter.All) { onSelect(VeilListFilter.All) }
    FilterPill(
      if (showUnreadCount(unreadCount)) String.format(stringResource(MR.strings.veil_list_filter_unread_count), unreadCount) else stringResource(MR.strings.veil_list_filter_unread),
      selected == VeilListFilter.Unread
    ) { onSelect(VeilListFilter.Unread) }
    FilterPill(stringResource(MR.strings.veil_list_filter_groups), selected == VeilListFilter.Groups) { onSelect(VeilListFilter.Groups) }
  }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
  val colors = VeilTheme.colors
  val shape = VeilTheme.shapes.pill
  Box(
    Modifier
      .height(34.dp)
      .clip(shape)
      .then(
        if (selected) Modifier.background(colors.night3).border(1.dp, colors.gradient, shape)
        else Modifier.background(colors.sheet1).border(1.dp, colors.line, shape)
      )
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .padding(horizontal = 14.dp),
    contentAlignment = Alignment.Center
  ) {
    BasicText(text, style = VeilTheme.type.body.copy(fontSize = 13.5.sp, color = if (selected) colors.ink else colors.ink2), maxLines = 1)
  }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
  Box(Modifier.fillMaxSize().padding(horizontal = 30.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun EmptyState(kind: VeilListEmpty) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  Centered {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (kind == VeilListEmpty.NoChats) {
        BasicText(stringResource(MR.strings.veil_list_empty_title), style = type.sheetTitle.copy(color = colors.ink, textAlign = TextAlign.Center))
        BasicText(stringResource(MR.strings.veil_list_empty_text), style = type.body.copy(color = colors.ink3, textAlign = TextAlign.Center))
      } else {
        val res = when (kind) {
          VeilListEmpty.NoUnread -> MR.strings.veil_list_empty_unread
          VeilListEmpty.NoGroups -> MR.strings.veil_list_empty_groups
          else -> MR.strings.veil_list_empty_search
        }
        BasicText(stringResource(res), style = type.body.copy(color = colors.ink3, textAlign = TextAlign.Center))
      }
    }
  }
}

@Composable
private fun NewChatButton(modifier: Modifier, onClick: () -> Unit) {
  val colors = VeilTheme.colors
  val shape = RoundedCornerShape(20.dp)
  val label = stringResource(MR.strings.veil_list_new_chat)
  val desc = stringResource(MR.strings.veil_list_new_chat_desc)
  Row(
    modifier
      .shadow(14.dp, shape, ambientColor = colors.iris, spotColor = colors.iris)
      .height(56.dp)
      .clip(shape)
      .drawBehind { drawRect(colors.gradientFor(size, 120f)) }
      .clickable(role = Role.Button, onClick = onClick)
      .semantics { contentDescription = desc }
      .padding(start = 18.dp, end = 22.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    VeilIcon(MR.images.ic_add, colors.onGradient, size = 22.dp)
    BasicText(label, style = VeilTheme.type.rowTitle.copy(color = colors.onGradient, fontSize = 15.5.sp))
  }
}

/** Bottom sheet with the chat actions of [chat] (null = closed). Scrim and back dismiss it; an action closes it, then runs. */
@Composable
private fun ActionsSheet(chat: Chat?, onDismiss: () -> Unit) {
  val motion = VeilTheme.motion
  val ms = motion.lift(VeilTheme.reduceMotion)
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  // keep the last chat while the sheet animates out
  var last by remember { mutableStateOf<Chat?>(null) }
  if (chat != null) last = chat
  if (chat != null) BackHandler(onBack = onDismiss)
  Box(Modifier.fillMaxSize()) {
    AnimatedVisibility(chat != null, enter = fadeIn(tween(ms)), exit = fadeOut(tween(ms))) {
      val dismissLabel = stringResource(MR.strings.veil_list_sheet_close)
      Box(
        Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f))
          .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
          .semantics { contentDescription = dismissLabel }
      )
    }
    AnimatedVisibility(
      chat != null,
      Modifier.align(Alignment.BottomCenter),
      enter = slideInVertically(tween(ms, easing = motion.easeOut)) { it },
      exit = slideOutVertically(tween(ms, easing = motion.easeOut)) { it },
    ) {
      val shown = chat ?: last
      if (shown != null) {
        VeilSheet(Modifier.navigationBarsPadding().pointerInput(Unit) { detectTapGestures { } }) {
          Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.padding(end = 3.dp, bottom = 3.dp)) { MaskAvatar(veilAvatarSeed(shown), 44.dp, incognito = shown.chatInfo.incognito) }
            BasicText(shown.chatInfo.chatViewName, Modifier.weight(1f), style = type.sheetTitle.copy(color = colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          for (action in veilChatActions(shown)) {
            val tint = when (action.tone) {
              VeilActionTone.Normal -> colors.ink
              VeilActionTone.Warning -> colors.warn
              VeilActionTone.Danger -> colors.errText
            }
            Row(
              Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(VeilTheme.shapes.input)
                .clickable(role = Role.Button) { onDismiss(); action.run() }
                .padding(horizontal = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              if (action.icon != null) VeilIcon(action.icon, tint, size = 22.dp) else Spacer(Modifier.size(22.dp))
              BasicText(stringResource(action.label), style = type.bodyLarge.copy(color = tint))
            }
          }
        }
      }
    }
  }
}
