package chat.simplex.common.veil.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.RemoteHostSessionState
import chat.simplex.common.platform.appFilesDir
import chat.simplex.common.platform.chatModel
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chat.VeilCaption
import chat.simplex.common.veil.screens.chat.VeilChoiceRow
import chat.simplex.common.veil.screens.chat.chatTtlOptions
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.database.DatabaseEncryptionView
import chat.simplex.common.views.database.databaseActions
import chat.simplex.common.views.database.databaseManagementActions
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.helpers.directoryFileCountAndSize
import chat.simplex.common.views.helpers.formatBytes
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.stringResource

/*
 * Veil database screens. They change the look of upstream's `DatabaseView` and `DatabaseManagementView` and nothing else:
 * every row calls the same functions (`databaseActions`, `databaseManagementActions`, which hold the code those views
 * ran) and so shows the same confirmation dialogs, in the same order, with the same texts. The passphrase screen is
 * upstream's own `DatabaseEncryptionView`, opened from a Veil row. Rows are enabled and disabled by the same rules
 * ([VeilDatabaseLogic.kt]).
 */

/** Spinner shown while an operation runs, as upstream shows it (centered, no blocking layer). */
@Composable
private fun BoxScope.VeilProgress(visible: Boolean) {
  if (!visible) return
  Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(Modifier.size(30.dp), color = VeilTheme.colors.accent, strokeWidth = 2.5.dp)
  }
}

private fun passphraseIconRes(icon: PassphraseIcon): ImageResource = when (icon) {
  PassphraseIcon.OpenLock -> MR.images.ic_lock_open_right
  PassphraseIcon.Key -> MR.images.ic_vpn_key_filled
  PassphraseIcon.Lock -> MR.images.ic_lock
}

/** Settings > Veritabanı ve parola: delete messages after, the entry to passphrase and export, delete files and media. */
@Composable
fun VeilDatabaseScreen(close: () -> Unit) {
  val m = chatModel
  val prefs = m.controller.appPrefs
  val progress = remember { mutableStateOf(false) }
  val useKeychain = remember { mutableStateOf(prefs.storeDBPassphrase.get()) }
  val chatLastStart = remember { mutableStateOf(prefs.chatLastStart.get()) }
  val stopped = remember { m.chatRunning }.value == false
  val appFilesCountAndSize = remember { mutableStateOf(directoryFileCountAndSize(appFilesDir.absolutePath)) }
  val chatItemTTL = remember { mutableStateOf(m.chatItemTTL.value) }
  val user = m.currentUser.value
  val actions = databaseActions(m, user?.remoteHostId, stopped, chatLastStart, progress, appFilesCountAndSize, chatItemTTL)
  val opsDisabled = operationsDisabled(progress.value, m.desktopNoUserNoRemote)
  val ttlOptions = remember { chatTtlOptions(chatItemTTL.value, hasDefault = false).mapNotNull { t -> t?.let { it to it.text } } }
  val (fileCount, fileSize) = appFilesCountAndSize.value
  val filesDisabled = deleteFilesDisabled(opsDisabled, fileCount)

  Box(Modifier.fillMaxSize()) {
    VeilSettingsScaffold(stringResource(MR.strings.veil_set_row_database), close) {
      if (messagesSectionShown(m.desktopNoUserNoRemote)) {
        VeilSectionLabel(stringResource(MR.strings.veil_db_messages))
        VeilCard(contentPadding = PaddingValues(8.dp)) {
          VeilChoiceRow(
            title = stringResource(MR.strings.veil_db_ttl),
            options = ttlOptions,
            selected = chatItemTTL.value,
            onSelect = actions.onChatItemTTLSelected,
            enabled = ttlEnabled(stopped, progress.value),
          )
        }
        VeilCaption(String.format(stringResource(MR.strings.veil_db_ttl_note), user?.displayName ?: ""), Modifier.padding(bottom = 10.dp))
      }

      Spacer(Modifier.height(8.dp))
      VeilGroup {
        VeilNavRow(
          passphraseIconRes(passphraseIcon(m.chatDbEncrypted.value, useKeychain.value)),
          stringResource(MR.strings.veil_db_manage),
          databaseSummary(m.chatDbEncrypted.value).joinSummary(::renderSummary),
          onClick = { veilShowSettingsPage { c -> VeilDatabaseManagementScreen(c) } },
          enabled = !opsDisabled
        )
      }

      VeilSectionLabel(stringResource(MR.strings.veil_db_files))
      VeilButton(
        stringResource(if (m.users.size > 1) MR.strings.delete_files_and_media_for_all_users else MR.strings.delete_files_and_media_all),
        onClick = actions.deleteAppFilesAndMedia,
        style = VeilButtonStyle.Danger,
        enabled = !filesDisabled,
      )
      VeilCaption(
        if (!hasStoredFiles(fileCount)) stringResource(MR.strings.no_received_app_files)
        else String.format(stringResource(MR.strings.total_files_count_and_size), fileCount, formatBytes(fileSize)),
        Modifier.padding(top = 6.dp)
      )
    }
    VeilProgress(progress.value)
  }
}

/**
 * Settings > Veritabanı ve parola > Parola ve yedek: the passphrase screen (upstream's), backup (upstream's export) and
 * restore (upstream's import), delete all data (upstream's delete database), and the run-chat switch. The archive,
 * the file pickers and the confirmations are upstream's; Veil only asks first (below).
 */
@Composable
fun VeilDatabaseManagementScreen(close: () -> Unit) {
  val m = chatModel
  val prefs = m.controller.appPrefs
  val progress = remember { mutableStateOf(false) }
  val useKeychain = remember { mutableStateOf(prefs.storeDBPassphrase.get()) }
  val chatLastStart = remember { mutableStateOf(prefs.chatLastStart.get()) }
  val stopped = remember { m.chatRunning }.value == false
  val appFilesCountAndSize = remember { mutableStateOf(directoryFileCountAndSize(appFilesDir.absolutePath)) }
  val actions = databaseManagementActions(m, stopped, chatLastStart, progress, appFilesCountAndSize)
  val opsDisabled = operationsDisabled(progress.value, m.desktopNoUserNoRemote)
  val toggleEnabled = runChatToggleEnabled(remember { chatModel.remoteHosts }.count { it.sessionState is RemoteHostSessionState.Connected })

  Box(Modifier.fillMaxSize()) {
    VeilSettingsScaffold(stringResource(MR.strings.veil_db_manage), close) {
      VeilSectionLabel(stringResource(MR.strings.veil_db_section_db))
      VeilGroup {
        if (disconnectHostsInDatabaseGroup(m.localUserCreated.value, toggleEnabled)) {
          VeilValueRow(stringResource(MR.strings.disconnect_remote_hosts), null, readOnly = false, onClick = actions.disconnectAllHosts)
          VeilRowDivider()
        }
        VeilNavRow(
          passphraseIconRes(passphraseIcon(m.chatDbEncrypted.value, useKeychain.value)),
          stringResource(MR.strings.veil_db_passphrase),
          databaseSummary(m.chatDbEncrypted.value).joinSummary(::renderSummary),
          onClick = { ModalManager.start.showModal(cardScreen = true) { DatabaseEncryptionView(chatModel, false) } },
          enabled = !opsDisabled
        )
      }

      // Backup and restore are upstream's export and import (archive, file picker, replace confirmation); Veil asks first.
      VeilSectionLabel(stringResource(MR.strings.veil_bk_section))
      VeilGroup {
        VeilActionRow(MR.images.ic_ios_share, stringResource(MR.strings.veil_bk_backup), stringResource(MR.strings.veil_bk_backup_sum), danger = false, enabled = !opsDisabled, onClick = {
          veilStartBackup(
            gate = backupGate(prefs.initialRandomDBPassphrase.get(), m.chatDbEncrypted.value),
            export = actions.exportDatabase,
          )
        })
        VeilRowDivider()
        VeilActionRow(MR.images.ic_download, stringResource(MR.strings.veil_bk_restore), stringResource(MR.strings.veil_bk_restore_sum), danger = true, enabled = !opsDisabled, onClick = { veilStartRestore(actions.importDatabase) })
      }
      VeilNote(stringResource(MR.strings.veil_bk_note))

      VeilSectionLabel(stringResource(MR.strings.veil_wipe_section))
      VeilGroup {
        VeilActionRow(MR.images.ic_delete_forever, stringResource(MR.strings.veil_db_delete), stringResource(MR.strings.veil_db_delete_sum), danger = true, enabled = !opsDisabled, onClick = actions.deleteDatabase)
      }
      VeilNote(stringResource(MR.strings.veil_wipe_note))
      VeilNote(stringResource(MR.strings.veil_db_stops_note))

      if (runChatSectionShown(m.localUserCreated.value)) {
        VeilSectionLabel(stringResource(MR.strings.veil_db_run))
        VeilGroup {
          if (disconnectHostsInRunChat(toggleEnabled)) {
            VeilValueRow(stringResource(MR.strings.disconnect_remote_hosts), null, readOnly = false, onClick = actions.disconnectAllHosts)
            VeilRowDivider()
          }
          VeilSwitchRow(
            title = stringResource(if (stopped) MR.strings.chat_is_stopped else MR.strings.chat_is_running),
            checked = !stopped,
            onChange = { on ->
              when (runChatAction(on)) {
                RunChatAction.Start -> actions.startChat()
                RunChatAction.AskToStop -> actions.stopChatAlert()
              }
            },
            enabled = runChatSwitchEnabled(toggleEnabled, progress.value),
          )
        }
        if (recentVersionFooterShown(stopped)) VeilNote(stringResource(MR.strings.you_must_use_the_most_recent_version_of_database))
      }
    }
    VeilProgress(progress.value)
  }
}

/** Row that runs an action (no chevron): icon tile, title, one-line summary; [danger] colours it like a destructive action. */
@Composable
private fun VeilActionRow(icon: ImageResource, title: String, summary: String, danger: Boolean, enabled: Boolean, onClick: () -> Unit) {
  val colors = VeilTheme.colors
  val tint = if (danger) colors.errText else colors.ink
  Row(
    Modifier
      .fillMaxWidth()
      .alpha(if (enabled) 1f else 0.38f)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
      .heightIn(min = 48.dp)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.sheet2), contentAlignment = Alignment.Center) {
      VeilIcon(icon, tint, size = 20.dp)
    }
    Column(Modifier.weight(1f)) {
      BasicText(title, style = VeilTheme.type.rowTitle.copy(color = tint), maxLines = 2, overflow = TextOverflow.Ellipsis)
      BasicText(summary, style = VeilTheme.type.caption.copy(color = colors.ink3), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
  }
}

/**
 * "Yedek al": a backup is always protected by a passphrase the owner chose. Without one the owner is sent to upstream's
 * passphrase screen; with one a short Veil explanation comes before upstream's export (stop chat, archive, file picker).
 */
private fun veilStartBackup(gate: BackupGate, export: () -> Unit) {
  when (gate) {
    BackupGate.NeedsPassphrase -> AlertManager.shared.showAlertDialog(
      title = generalGetString(MR.strings.veil_bk_need_title),
      text = generalGetString(MR.strings.veil_bk_need_text),
      confirmText = generalGetString(MR.strings.veil_bk_need_confirm),
      onConfirm = { ModalManager.start.showModal { DatabaseEncryptionView(chatModel, false) } },
    )
    BackupGate.Allowed -> AlertManager.shared.showAlertDialog(
      title = generalGetString(MR.strings.veil_bk_ready_title),
      text = generalGetString(MR.strings.veil_bk_ready_text),
      confirmText = generalGetString(MR.strings.veil_bk_ready_confirm),
      onConfirm = export,
    )
  }
}

/** "Yedekten geri yükle": the Veil warning about old backups and two devices comes before upstream's file picker and its replace confirmation. */
private fun veilStartRestore(import: () -> Unit) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(MR.strings.veil_bk_restore_title),
    text = generalGetString(MR.strings.veil_bk_restore_text),
    confirmText = generalGetString(MR.strings.veil_bk_restore_confirm),
    destructive = true,
    onConfirm = import,
  )
}
