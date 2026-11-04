package chat.simplex.common.veil.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.VeilNetworkPolicy
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.veil.data.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.chat.item.openBrowserAlert
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Orbot setup. A real check, not a timer: every [ORBOT_POLL_INTERVAL_MS] the local SOCKS proxy is greeted
 * (`VeilNetworkPolicy.torProxyReady`, on IO). The ring says "Connecting" until the proxy answers and "Proxy yanıt
 * veriyor" afterwards. This never claims that Tor itself works; circuits are not measured.
 */
@Composable
internal fun OrbotScreen(
  onBack: () -> Unit,
  onContinue: () -> Unit,
  probe: () -> Boolean = { VeilNetworkPolicy.torProxyReady() },
) {
  val colors = VeilTheme.colors
  var check by remember { mutableStateOf(OrbotCheckState()) }
  var showMissing by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    val checker = OrbotCheck(probe)
    while (isActive) {
      check = withContext(Dispatchers.IO) { checker.tick() }
      delay(ORBOT_POLL_INTERVAL_MS)
    }
  }

  BackHandler(enabled = showMissing) { showMissing = false }
  BackHandler(enabled = !showMissing, onBack = onBack)

  Box(Modifier.fillMaxSize()) {
    OnbScaffold(
      onBack = onBack,
      title = stringResource(MR.strings.veil_onb_orbot_title),
      bottom = {
        VeilButton(stringResource(MR.strings.veil_onb_continue), onClick = onContinue, enabled = check.canContinue)
      },
    ) {
      Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TorRing(check.ringState, size = 92.dp)
        BasicText(torRingLabel(check.ringState), style = VeilTheme.type.caption.copy(color = colors.ink2))
      }
      VeilCard(Modifier.fillMaxWidth()) {
        OrbotLog(check)
      }
      Spacer(Modifier.height(14.dp))
      BasicText(stringResource(MR.strings.veil_onb_orbot_text), Modifier.padding(horizontal = 2.dp), style = VeilTheme.type.body.copy(color = colors.ink2))
      Spacer(Modifier.height(14.dp))
      OnbNote(stringResource(MR.strings.veil_onb_orbot_hint), Modifier.padding(horizontal = 2.dp))
      Spacer(Modifier.height(16.dp))
      VeilButton(
        stringResource(MR.strings.veil_onb_orbot_open),
        onClick = {
          when (launchOrbot()) {
            OrbotLaunchResult.Launched -> {}
            OrbotLaunchResult.NotInstalled -> showMissing = true
            OrbotLaunchResult.Failed -> AlertManager.shared.showAlertMsg(generalGetString(MR.strings.veil_onb_orbot_launch_failed))
          }
        },
        style = VeilButtonStyle.Secondary,
      )
      Spacer(Modifier.height(10.dp))
    }
    if (showMissing) {
      OrbotMissingSheet(onClose = { showMissing = false })
    }
  }
}

@Composable
private fun OrbotLog(state: OrbotCheckState) {
  val colors = VeilTheme.colors
  val mono = VeilTheme.type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, lineHeight = 21.sp)
  Column(Modifier.fillMaxWidth().heightIn(min = 98.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
    BasicText(stringResource(MR.strings.veil_onb_orbot_log_waiting), style = mono.copy(color = colors.ink2))
    if (state.answered) {
      BasicText(stringResource(MR.strings.veil_onb_orbot_log_answered), style = mono.copy(color = colors.ink))
      BasicText(stringResource(MR.strings.veil_onb_orbot_log_note), style = mono.copy(color = colors.ink2))
    } else {
      BlinkingCursor(mono.copy(color = colors.accent))
    }
  }
}

@Composable
private fun BlinkingCursor(style: androidx.compose.ui.text.TextStyle) {
  val reduce = VeilTheme.reduceMotion
  val t = rememberInfiniteTransition(label = "cursor")
  val alpha by t.animateFloat(
    1f, 0f, infiniteRepeatable(keyframes { durationMillis = 1000; 1f at 0; 1f at 499; 0f at 500; 0f at 1000 }), label = "blink"
  )
  BasicText("▍", style = style.copy(color = style.color.copy(alpha = if (reduce) 1f else alpha)))
}

/** Shown when Orbot is not installed: says so and lists where to get it as plain text. Opening an address goes through the external-link warning. */
@Composable
private fun OrbotMissingSheet(onClose: () -> Unit) {
  val colors = VeilTheme.colors
  val uriHandler = LocalUriHandler.current
  Box(
    Modifier.fillMaxSize().background(colors.scrim).clickable(onClick = onClose),
    contentAlignment = Alignment.BottomCenter
  ) {
    VeilSheet(Modifier.navigationBarsPadding().clickable(enabled = false) {}) {
      BasicText(stringResource(MR.strings.veil_onb_orbot_missing_title), style = VeilTheme.type.sheetTitle.copy(color = colors.ink))
      Spacer(Modifier.height(10.dp))
      BasicText(stringResource(MR.strings.veil_onb_orbot_missing_text), style = VeilTheme.type.body.copy(color = colors.ink2))
      Spacer(Modifier.height(14.dp))
      StoreRow(stringResource(MR.strings.veil_onb_orbot_fdroid), ORBOT_FDROID_URL) { openBrowserAlert(ORBOT_FDROID_URL, uriHandler) }
      Spacer(Modifier.height(8.dp))
      StoreRow(stringResource(MR.strings.veil_onb_orbot_play), ORBOT_PLAY_URL) { openBrowserAlert(ORBOT_PLAY_URL, uriHandler) }
      Spacer(Modifier.height(16.dp))
      VeilButton(stringResource(MR.strings.veil_onb_close), onClick = onClose, style = VeilButtonStyle.Secondary)
    }
  }
}

@Composable
private fun StoreRow(label: String, address: String, onOpen: () -> Unit) {
  val colors = VeilTheme.colors
  VeilCard(Modifier.fillMaxWidth(), layer = VeilLayer.Mid, onClick = onOpen, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
    BasicText(label, style = VeilTheme.type.rowTitle.copy(color = colors.ink))
    BasicText(address, style = VeilTheme.type.caption.copy(color = colors.mintInk))
  }
}
