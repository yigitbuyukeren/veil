package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.NetCfg
import chat.simplex.common.model.VeilNetworkPolicy
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VEIL_TOR_PROBE_INTERVAL_MS
import chat.simplex.common.veil.screens.chatlist.torRingStateFor
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.usersettings.networkAndServers.NetworkAndServersView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Opens the Tor and status screen. Also what the chat list's Tor ring and the old status entries open. */
fun veilOpenTorScreen() {
  veilShowSettingsPage { close -> VeilTorScreen(close) }
}

/**
 * Tor and status. The ring comes from the same rule as the chat list ([torRingStateFor]): it measures only whether the
 * local SOCKS proxy answers, never Tor circuits. The policy line is read from the config the core reports as active.
 */
@Composable
fun VeilTorScreen(close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  var tick by remember { mutableIntStateOf(0) }
  var proxyAnswers by remember { mutableStateOf<Boolean?>(null) }
  var policyChecked by remember { mutableStateOf(false) }
  var activeConfig by remember { mutableStateOf<NetCfg?>(null) }

  LaunchedEffect(tick) {
    proxyAnswers = null
    while (true) {
      proxyAnswers = withContext(Dispatchers.IO) { VeilNetworkPolicy.torProxyReady() }
      delay(VEIL_TOR_PROBE_INTERVAL_MS)
    }
  }
  LaunchedEffect(tick) {
    policyChecked = false
    activeConfig = chatModel.controller.apiGetNetworkConfig()
    policyChecked = true
  }
  val ring = torRingStateFor(proxyAnswers, chatModel.chatRunning.value)
  val policy = policyLineState(policyChecked, activeConfig)

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_tor_title), close) {
    VeilCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), layer = VeilLayer.Mid) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TorRing(ring, 56.dp)
        BasicText(torRingLabel(ring), Modifier.weight(1f), style = type.sheetTitle.copy(color = colors.ink))
      }
      Spacer(Modifier.height(14.dp))
      VeilButton(
        stringResource(MR.strings.veil_status_recheck), onClick = { tick++ },
        style = VeilButtonStyle.Secondary
      )
    }
    VeilNote(stringResource(MR.strings.veil_set_tor_note))

    VeilGroup(Modifier.padding(top = 4.dp)) {
      VeilValueRow(
        stringResource(MR.strings.veil_set_tor_policy_title),
        sub = stringResource(
          when (policy) {
            PolicyLineState.Checking -> MR.strings.veil_set_tor_policy_checking
            PolicyLineState.Ok -> MR.strings.veil_set_tor_policy_ok
            PolicyLineState.NotCompliant -> MR.strings.veil_set_tor_policy_not_compliant
            PolicyLineState.Unknown -> MR.strings.veil_set_tor_policy_unknown
          }
        ),
        readOnly = false
      )
    }

    VeilSectionLabel(stringResource(MR.strings.veil_set_tor_network))
    VeilGroup {
      veilNetworkRows().forEachIndexed { i, row ->
        if (i > 0) VeilRowDivider()
        VeilValueRow(
          stringResource(
            when (row.kind) {
              NetRowKind.Path -> MR.strings.veil_set_net_path
              NetRowKind.Socks -> MR.strings.veil_set_net_socks
              NetRowKind.Direct -> MR.strings.veil_set_net_direct
              NetRowKind.NoTor -> MR.strings.veil_set_net_no_tor
            }
          ),
          sub = row.value ?: stringResource(
            when (row.kind) {
              NetRowKind.Path -> MR.strings.veil_set_net_path_value
              NetRowKind.Direct -> MR.strings.veil_set_net_direct_value
              else -> MR.strings.veil_set_net_no_tor_value
            }
          ),
          readOnly = row.readOnly
        )
      }
    }
    VeilNote(stringResource(MR.strings.veil_set_tor_locked_note))

    VeilSectionLabel(stringResource(MR.strings.veil_status_privacy))
    VeilGroup {
      val incognito = remember { appPrefs.incognito.state }.value
      privacyDefaultRows(incognito, callsEnabled = VeilNetworkPolicy.callsAllowed()).forEachIndexed { i, row ->
        if (i > 0) VeilRowDivider()
        val yn = stringResource(if (row.ok) MR.strings.veil_status_ok else MR.strings.veil_status_fail)
        VeilValueRow(
          stringResource(
            when (row.kind) {
              PrivacyDefaultKind.Incognito -> MR.strings.veil_set_default_incognito
              PrivacyDefaultKind.LinkPreviewsOff -> MR.strings.veil_set_default_previews
              PrivacyDefaultKind.CallsOff -> if (row.ok) MR.strings.veil_set_default_calls else MR.strings.veil_calls_section
            }
          ),
          sub = null, readOnly = false,
          // Veil: when calls are turned on the row says so instead of a plain "no"
          value = if (row.kind == PrivacyDefaultKind.CallsOff && !row.ok) stringResource(MR.strings.veil_calls_on_outside_tor) else yn
        )
      }
    }

    Spacer(Modifier.height(16.dp))
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_set_tor_servers), sub = stringResource(MR.strings.veil_set_tor_servers_desc), readOnly = false,
        onClick = { ModalManager.start.showCustomModal { c -> if (veilIsDebugBuild) NetworkAndServersView(c) else VeilServersScreen(c) } }
      )
    }
  }
}
