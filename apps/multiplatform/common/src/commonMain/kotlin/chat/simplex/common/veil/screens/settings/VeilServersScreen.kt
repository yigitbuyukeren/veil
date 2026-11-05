package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Read-only list of the relay servers in use (enabled servers of enabled operators, message and file servers), read
 * from the core. Nothing here edits servers: the network settings are locked (see `VeilNetworkPolicy`).
 */
@Composable
fun VeilServersScreen(close: () -> Unit) {
  var smp by remember { mutableStateOf<List<VeilServerRow>?>(null) }
  var xftp by remember { mutableStateOf<List<VeilServerRow>?>(null) }
  var failed by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    val servers = chatModel.controller.getUserServers(chatModel.remoteHostId())
    if (servers == null) { failed = true; return@LaunchedEffect }
    val active = servers.filter { it.operator?.enabled != false }
    smp = active.flatMap { it.smpServers }.filter { it.enabled && !it.deleted }.flatMap { veilServerRows(it.server) }
    xftp = active.flatMap { it.xftpServers }.filter { it.enabled && !it.deleted }.flatMap { veilServerRows(it.server) }
  }
  VeilSettingsScaffold(stringResource(MR.strings.veil_srv_title), close) {
    if (failed) VeilNote(stringResource(MR.strings.veil_srv_failed))
    smp?.let { ServerGroup(stringResource(MR.strings.veil_srv_smp), it) }
    xftp?.let { ServerGroup(stringResource(MR.strings.veil_srv_xftp), it) }
    VeilNote(stringResource(MR.strings.veil_srv_note))
  }
}

@Composable
private fun ServerGroup(label: String, rows: List<VeilServerRow>) {
  VeilSectionLabel(label)
  VeilGroup {
    if (rows.isEmpty()) VeilValueRow(stringResource(MR.strings.veil_srv_none), sub = null, readOnly = false)
    rows.forEachIndexed { i, r ->
      if (i > 0) VeilRowDivider()
      VeilValueRow(
        r.host, sub = null, readOnly = false,
        value = stringResource(if (r.onion) MR.strings.veil_srv_onion else MR.strings.veil_srv_not_onion)
      )
    }
  }
}
