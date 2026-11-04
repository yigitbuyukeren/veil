package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionDividerSpaced
import SectionView
import itemHPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.VeilNetworkPolicy
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.DEFAULT_PADDING_HALF
import chat.simplex.common.views.helpers.AppBarTitle
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.helpers.openExternalLink
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import chat.simplex.res.MR

/** Tor/privacy indicators and licence notice (VEIL-016). The proxy probe shows a SOCKS listener only, not circuit health. */
@Composable
fun VeilStatusView() {
  val uriHandler = LocalUriHandler.current
  val proxyUp = remember { mutableStateOf<Boolean?>(null) }
  // Veil: null = checking or unknown; computed from the config the core reports as active, not from the app preferences
  val policyOk = remember { mutableStateOf<Boolean?>(null) }
  val policyUnknown = remember { mutableStateOf(false) }
  val tick = remember { mutableStateOf(0) }
  LaunchedEffect(tick.value) {
    proxyUp.value = null
    proxyUp.value = withContext(Dispatchers.IO) { VeilNetworkPolicy.torProxyReady() }
    policyOk.value = null
    policyUnknown.value = false
    val active = chatModel.controller.apiGetNetworkConfig()
    if (active == null) policyUnknown.value = true else policyOk.value = VeilNetworkPolicy.isCompliant(active)
  }
  fun yn(b: Boolean?) = when (b) {
    null -> generalGetString(MR.strings.veil_status_checking)
    true -> generalGetString(MR.strings.veil_status_ok)
    false -> generalGetString(MR.strings.veil_status_fail)
  }
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.veil_status_title))
    SectionView(stringResource(MR.strings.veil_status_network)) {
      Column(Modifier.padding(horizontal = itemHPadding, vertical = DEFAULT_PADDING_HALF)) {
        Text(String.format(stringResource(MR.strings.veil_status_tor_proxy), "${VeilNetworkPolicy.SOCKS_HOST}:${VeilNetworkPolicy.SOCKS_PORT}", yn(proxyUp.value)))
        Text(String.format(stringResource(MR.strings.veil_status_policy), if (policyUnknown.value) generalGetString(MR.strings.veil_status_unknown) else yn(policyOk.value)))
        Text(stringResource(MR.strings.veil_status_note), color = MaterialTheme.colors.secondary)
      }
      SettingsActionItem(painterResource(MR.images.ic_wifi_tethering), stringResource(MR.strings.veil_status_recheck), { tick.value++ })
    }
    SectionDividerSpaced()
    SectionView(stringResource(MR.strings.veil_status_privacy)) {
      Column(Modifier.padding(horizontal = itemHPadding, vertical = DEFAULT_PADDING_HALF)) {
        Text(String.format(stringResource(MR.strings.veil_status_incognito), yn(appPrefs.incognito.state.value)))
        Text(String.format(stringResource(MR.strings.veil_status_previews), yn(!VeilNetworkPolicy.LINK_PREVIEWS_ENABLED)))
        Text(String.format(stringResource(MR.strings.veil_status_calls), yn(!VeilNetworkPolicy.CALLS_ENABLED)))
      }
    }
    SectionDividerSpaced()
    SectionView(stringResource(MR.strings.veil_licenses)) {
      Column(Modifier.padding(horizontal = itemHPadding, vertical = DEFAULT_PADDING_HALF)) {
        Text(stringResource(MR.strings.veil_licenses_text))
      }
      SettingsActionItem(painterResource(MR.images.ic_github), stringResource(MR.strings.veil_licenses_notices), { uriHandler.openExternalLink("https://github.com/yigitbuyukeren/veil/blob/main/docs/THIRD_PARTY_NOTICES.md") }, textColor = MaterialTheme.colors.primary)
    }
    SectionBottomSpacer()
  }
}

/** Thin chat-list line: measures only whether the local SOCKS proxy answers, never Tor circuit or relay health. */
@Composable
fun VeilSocksIndicator() {
  val up = remember { mutableStateOf<Boolean?>(null) }
  LaunchedEffect(Unit) {
    while (true) {
      up.value = withContext(Dispatchers.IO) { VeilNetworkPolicy.torProxyReady() }
      delay(15_000)
    }
  }
  val v = up.value ?: return
  Text(
    stringResource(if (v) MR.strings.veil_socks_up else MR.strings.veil_socks_down),
    Modifier.padding(horizontal = itemHPadding, vertical = 2.dp),
    fontSize = 11.sp,
    color = if (v) MaterialTheme.colors.secondary else Color(0xFFC62828)
  )
}
