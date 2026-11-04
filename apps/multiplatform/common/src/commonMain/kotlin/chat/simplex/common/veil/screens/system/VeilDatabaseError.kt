package chat.simplex.common.veil.screens.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.appVersionInfo
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.views.database.mtrErrorDescription
import chat.simplex.common.views.database.validKey
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

/**
 * Veil look of upstream's `DatabaseErrorView` (Android). `DatabaseErrorView` keeps all state and logic and calls this
 * with its callbacks, so no option can go missing here without the test in `VeilSystemLogicTest` noticing:
 * every state renders exactly the options listed by [veilDbOptions].
 */
@Composable
fun VeilDatabaseErrorView(
  status: DBMigrationResult?,
  useKeychain: Boolean,
  storedKeyPresent: Boolean,
  dbKey: MutableState<String>,
  busy: Boolean,
  restoreAvailable: Boolean,
  onSaveAndOpen: () -> Unit,
  onOpen: () -> Unit,
  onUpgrade: () -> Unit,
  onDowngrade: () -> Unit,
  onRestore: () -> Unit,
) {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  val kind = veilDbError(status, useKeychain, storedKeyPresent)
  val options = veilDbOptions(kind, restoreAvailable)
  val keyOk = validKey(dbKey.value) && !busy
  val dbFile: String? = when (status) {
    is DBMigrationResult.ErrorNotADatabase -> status.dbFile
    is DBMigrationResult.ErrorMigration -> status.dbFile
    is DBMigrationResult.ErrorSQL -> status.dbFile
    else -> null
  }
  val titleRes: StringResource = when (kind) {
    VeilDbError.WrongPassphrase -> MR.strings.wrong_passphrase
    VeilDbError.PassphraseRequiredStore, VeilDbError.PassphraseRequired -> MR.strings.encrypted_database
    VeilDbError.Upgrade -> MR.strings.database_upgrade
    VeilDbError.Downgrade -> MR.strings.database_downgrade
    VeilDbError.IncompatibleVersion -> MR.strings.incompatible_database_version
    VeilDbError.Keychain -> MR.strings.keychain_error
    VeilDbError.InvalidConfirmation -> MR.strings.invalid_migration_confirmation
    VeilDbError.DatabaseError, VeilDbError.Unknown, VeilDbError.None -> MR.strings.database_error
  }
  val migrationError = (status as? DBMigrationResult.ErrorMigration)?.migrationError

  Box(Modifier.fillMaxSize().background(colors.night)) {
    Column(
      Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = VeilGutter, vertical = 24.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      BasicText(stringResource(titleRes), style = type.title.copy(color = colors.ink))
      Spacer(Modifier.height(16.dp))
      VeilCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          for (opt in options) {
            when (opt) {
              VeilDbOption.PassphraseField -> {
                Body(stringResource(if (kind == VeilDbError.WrongPassphrase) MR.strings.passphrase_is_different else MR.strings.database_passphrase_is_required))
                PassphraseInput(dbKey, keyOk) { if (kind == VeilDbError.PassphraseRequired) onOpen() else onSaveAndOpen() }
              }
              VeilDbOption.SaveAndOpen ->
                VeilButton(stringResource(MR.strings.save_passphrase_and_open_chat), onSaveAndOpen, enabled = keyOk)
              VeilDbOption.Open ->
                VeilButton(stringResource(MR.strings.open_chat), onOpen, enabled = keyOk)
              VeilDbOption.UpgradeAndOpen ->
                VeilButton(stringResource(MR.strings.upgrade_and_open_chat), onUpgrade, enabled = !busy)
              VeilDbOption.DowngradeAndOpen ->
                VeilButton(stringResource(MR.strings.downgrade_and_open_chat), onDowngrade, enabled = !busy, style = VeilButtonStyle.Danger)
              VeilDbOption.DowngradeWarning -> {
                VeilWarningNote(stringResource(MR.strings.database_downgrade_warning))
                val down = (migrationError as? MigrationError.Downgrade)?.downMigrations
                if (down != null) for (w in downMigrationWarnings(down).reversed()) VeilWarningNote(w)
              }
              VeilDbOption.FileName -> if (dbFile != null)
                Detail(String.format(stringResource(MR.strings.file_with_path), dbFile.split("/").lastOrNull() ?: dbFile))
              VeilDbOption.Migrations -> when (migrationError) {
                is MigrationError.Upgrade -> Detail(String.format(stringResource(MR.strings.database_migrations), migrationError.upMigrations.joinToString(", ") { it.upName }))
                is MigrationError.Downgrade -> Detail(String.format(stringResource(MR.strings.database_migrations), migrationError.downMigrations.joinToString(", ")))
                else -> {}
              }
              VeilDbOption.AppVersion ->
                Detail(appVersionInfo.first + (if (appVersionInfo.second != null) " (" + appVersionInfo.second + ")" else ""))
              VeilDbOption.ErrorInfo -> Detail(errorInfo(status, migrationError))
              VeilDbOption.RestoreBackup -> {
                Body(stringResource(MR.strings.database_backup_can_be_restored))
                VeilButton(stringResource(MR.strings.restore_database), onRestore, style = VeilButtonStyle.Danger)
              }
            }
          }
          // States without options still say what happened.
          if (kind == VeilDbError.Keychain) Body(stringResource(MR.strings.cannot_access_keychain))
        }
      }
    }
    if (busy) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(30.dp), color = colors.accent, strokeWidth = 2.5.dp)
      }
    }
  }
}

@Composable
private fun errorInfo(status: DBMigrationResult?, migrationError: MigrationError?): String = when {
  status is DBMigrationResult.ErrorSQL -> String.format(stringResource(MR.strings.error_with_info), status.migrationSQLError)
  status is DBMigrationResult.Unknown -> String.format(stringResource(MR.strings.unknown_database_error_with_info), status.json)
  migrationError is MigrationError.Error -> String.format(stringResource(MR.strings.error_with_info), mtrErrorDescription(migrationError.mtrError))
  else -> ""
}

@Composable
private fun Body(text: String) {
  BasicText(text, style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink))
}

@Composable
private fun Detail(text: String) {
  BasicText(text, style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink2))
}

@Composable
private fun PassphraseInput(value: MutableState<String>, enabled: Boolean, onDone: () -> Unit) {
  val colors = VeilTheme.colors
  val focus = remember { FocusRequester() }
  var show by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(100L)
    runCatching { focus.requestFocus() }
  }
  VeilTextField(
    value = value.value,
    onValueChange = { value.value = it },
    modifier = Modifier.focusRequester(focus).onPreviewKeyEvent {
      if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp && enabled) {
        onDone(); true
      } else false
    },
    placeholder = stringResource(MR.strings.enter_passphrase),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrect = false, keyboardType = KeyboardType.Password),
    keyboardActions = KeyboardActions(onDone = { if (enabled) onDone() }),
    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
    trailing = {
      val valid = validKey(value.value)
      val toggleDescription = stringResource(if (show) MR.strings.veil_a11y_pass_hide else MR.strings.veil_a11y_pass_show)
      Box(
        Modifier
          .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .clickable(role = Role.Button) { show = !show }
          .semantics { contentDescription = toggleDescription },
        contentAlignment = Alignment.Center
      ) {
        VeilIcon(
          if (!valid) MR.images.ic_error else if (show) MR.images.ic_visibility_off_filled else MR.images.ic_visibility_filled,
          if (valid) colors.ink2 else colors.err, size = 22.dp
        )
      }
    },
  )
}
