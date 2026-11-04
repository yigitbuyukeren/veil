package chat.simplex.common.veil.data

import androidx.compose.runtime.*
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.VeilNetworkPolicy
import chat.simplex.common.platform.isAppVisibleAndFocused
import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.veil.screens.chatlist.VEIL_TOR_PROBE_INTERVAL_MS
import chat.simplex.common.veil.screens.chatlist.torRingStateFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Ring state with the same honest rule as the chat list ([torRingStateFor]): the local SOCKS listener is probed
 * (`VeilNetworkPolicy.torProxyReady`, on IO) every [VEIL_TOR_PROBE_INTERVAL_MS] while the app is visible. The probe
 * says nothing about circuits.
 */
@Composable
fun rememberTorRingState(): TorRingState {
  var proxyAnswers by remember { mutableStateOf<Boolean?>(null) }
  LaunchedEffect(Unit) {
    var sinceProbe = VEIL_TOR_PROBE_INTERVAL_MS
    var wasVisible = false
    while (true) {
      val visible = isAppVisibleAndFocused()
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
  return torRingStateFor(proxyAnswers, ChatModel.chatRunning.value)
}
