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
 * Extension points for the later task that builds the Veil message actions sheet, chat info and security-code screens
 * (T11). Until then each default simply runs the upstream behaviour passed in.
 */
object VeilChatHooks {
  /**
   * Long-press on a message. [openUpstreamMenu] opens upstream's item menu (reply, edit, delete, react...). A Veil
   * sheet replaces the default by assigning this and not calling [openUpstreamMenu].
   */
  var onMessageActions: (item: ChatItem, openUpstreamMenu: () -> Unit) -> Unit = { _, openUpstreamMenu -> openUpstreamMenu() }

  /** Tap on the top bar (name / avatar). [openUpstreamInfo] opens upstream's chat info / group info. */
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
