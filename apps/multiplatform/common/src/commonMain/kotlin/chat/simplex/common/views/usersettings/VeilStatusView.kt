package chat.simplex.common.views.usersettings

import itemHPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.VeilNetworkPolicy
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import chat.simplex.res.MR

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
