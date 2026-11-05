package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.VeilNetworkPolicy
import chat.simplex.common.platform.appVersionInfo
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.migration.MigrateFromDeviceView
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Migration uploads an archive over the network, so it starts only while the Tor proxy answers. */
fun veilMigrationMayStart(torProxyAnswers: Boolean): Boolean = torProxyAnswers

/**
 * "Gelişmiş": move to another device, version info, developer tools. Each row opens upstream's screen; the migration
 * itself is upstream's `MigrateFromDeviceView` (its temporary chat controller sets its network config through
 * `apiSetNetworkConfig`, which `VeilNetworkPolicy.enforce` rewrites, so the archive upload goes through the Tor proxy).
 */
@Composable
fun VeilAdvancedScreen(close: () -> Unit) {
  val type = VeilTheme.type
  val colors = VeilTheme.colors
  VeilSettingsScaffold(stringResource(MR.strings.veil_set_row_advanced), close) {
    VeilGroup {
      VeilNavRow(
        MR.images.ic_ios_share, stringResource(MR.strings.veil_adv_row_migrate),
        stringResource(MR.strings.veil_adv_sum_migrate),
        onClick = { veilStartMigrationFromDevice() }
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_info, stringResource(MR.strings.veil_adv_row_version),
        appVersionInfo.first,
        onClick = {
          showUpstreamSettings {
            VersionInfoView(
              showSettingsModal = { modalView -> { ModalManager.start.showModal(settings = true, cardScreen = true) { modalView(chatModel) } } },
              withAuth = ::doWithAuth
            )
          }
        }
      )
      VeilRowDivider()
      VeilNavRow(
        MR.images.ic_code, stringResource(MR.strings.veil_adv_row_dev),
        stringResource(MR.strings.veil_set_sum_advanced),
        onClick = { showUpstreamSettings { DeveloperView(withAuth = ::doWithAuth) } }
      )
    }
    BasicText(
      stringResource(MR.strings.veil_adv_migrate_note),
      Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp),
      style = type.caption.copy(color = colors.ink3),
    )
  }
}

/** Checks the Tor proxy first; an unanswered proxy shows an explanation and nothing starts. Then upstream's flow, behind the same auth. */
private fun veilStartMigrationFromDevice() {
  withBGApi {
    val ready = withContext(Dispatchers.IO) { VeilNetworkPolicy.torProxyReady() }
    if (!veilMigrationMayStart(ready)) {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(MR.strings.veil_adv_migrate_tor_title),
        text = generalGetString(MR.strings.veil_adv_migrate_tor_text),
      )
      return@withBGApi
    }
    doWithAuth(generalGetString(MR.strings.auth_open_migration_to_another_device), generalGetString(MR.strings.auth_log_in_using_credential)) {
      ModalManager.fullscreen.showCustomModal { close -> MigrateFromDeviceView(close) }
    }
  }
}
