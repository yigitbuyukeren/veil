package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.appVersionInfo
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.openExternalLink
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

private const val VEIL_SOURCE_URL = "https://github.com/yigitbuyukeren/veil"
private const val VEIL_NOTICES_URL = "https://github.com/yigitbuyukeren/veil/blob/main/docs/THIRD_PARTY_NOTICES.md"

/** About, version, source code and licences. Web links go through upstream's external-link path, which warns that they leave Tor. */
@Composable
fun VeilAboutScreen(close: () -> Unit) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val uriHandler = LocalUriHandler.current
  val (versionName, versionCode) = appVersionInfo

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_about_title), close) {
    VeilCard(Modifier.fillMaxWidth()) {
      BasicText(stringResource(MR.strings.veil_about_title), style = type.sheetTitle.copy(color = colors.ink))
      Spacer(Modifier.height(8.dp))
      BasicText(stringResource(MR.strings.veil_about_text), style = type.body.copy(color = colors.ink2))
    }

    Spacer(Modifier.height(16.dp))
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_set_about_version), sub = null, readOnly = false,
        value = versionName + (if (versionCode != null) " ($versionCode)" else "")
      )
      VeilRowDivider()
      VeilValueRow(
        stringResource(MR.strings.veil_source_code), sub = null, readOnly = false,
        onClick = { uriHandler.openExternalLink(VEIL_SOURCE_URL) }
      )
    }

    VeilSectionLabel(stringResource(MR.strings.veil_licenses))
    VeilCard(Modifier.fillMaxWidth()) {
      BasicText(stringResource(MR.strings.veil_licenses_text), style = type.body.copy(color = colors.ink2))
    }
    Spacer(Modifier.height(12.dp))
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_licenses_notices), sub = null, readOnly = false,
        onClick = { uriHandler.openExternalLink(VEIL_NOTICES_URL) }
      )
    }

    // Debug builds only: the design gallery (hardcoded label on purpose)
    if (veilIsDebugBuild) {
      Spacer(Modifier.height(16.dp))
      VeilGroup {
        VeilValueRow(
          "Veil design gallery (debug)", sub = null, readOnly = false,
          onClick = { ModalManager.start.showModal(cardScreen = true) { VeilGalleryScreen() } }
        )
      }
    }
  }
}
