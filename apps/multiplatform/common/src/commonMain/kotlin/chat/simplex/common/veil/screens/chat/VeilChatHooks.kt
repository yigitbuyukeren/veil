package chat.simplex.common.veil.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import chat.simplex.common.model.ChatItem
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.veil.VeilUi

/** True when the Veil chat screen is the one being drawn (Android only; desktop keeps upstream). */
fun veilChatActive(): Boolean = VeilUi.chat && appPlatform.isAndroid

/**
 * Extension points of the Veil chat screen. The defaults are the Veil sheet and screens (message actions, chat info,
 * security code); each still receives upstream's behaviour as a lambda to fall back to.
 */
object VeilChatHooks {
  /**
   * Long-press on a message. [env] carries what upstream's item menu has in scope; [openUpstreamMenu] opens that menu.
   * The default opens the Veil sheet ([veilShowMessageActions]).
   */
  var onMessageActions: (item: ChatItem, env: VeilMessageEnv, openUpstreamMenu: () -> Unit) -> Unit = ::veilShowMessageActions

  /**
   * Tap on the top bar (name / avatar). [openUpstreamInfo] opens upstream's chat info / group info. The default shows
   * the Veil screen for direct contacts and calls [openUpstreamInfo] for everything else ([veilShowChatInfo]).
   */
  var onChatInfo: (openUpstreamInfo: () -> Unit) -> Unit = { openUpstreamInfo -> openUpstreamInfo() }
}

/** Per-item facts the list knows and the bubble needs. Provided by `ChatViewListItem`. */
class VeilItemContext(
  /** First message of a run by the same sender: the group sender name is shown on it. */
  val firstInRun: Boolean = false,
)

val LocalVeilItemContext = compositionLocalOf { VeilItemContext() }

private val alwaysOff = mutableStateOf(false)

/** Upstream's one-hand / bottom-bar preferences move the app bar to the bottom; the Veil chat always has its bar on top. */
fun veilForceOff(upstream: State<Boolean>): State<Boolean> = if (veilChatActive()) alwaysOff else upstream
