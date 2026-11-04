package chat.simplex.common.veil.screens.settings

import SectionView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.veil.design.*
import chat.simplex.common.views.usersettings.AppearanceScope
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * Veil appearance screen: theme (Gece, Sis, Sistem), language and font size. The theme is stored in the Veil
 * preference `veilTheme` ([VeilThemeMode]); upstream's wallpaper and colour pickers are not offered (they have no effect).
 * Language and font size are upstream's controls.
 */
@Composable
fun VeilAppearanceScreen(close: () -> Unit) {
  val applyLanguage = rememberAppLanguageApplier()
  val language = rememberSaveable { mutableStateOf(appPrefs.appLanguage.get() ?: "system") }
  var mode by remember { mutableStateOf(VeilTheme.mode()) }

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_appearance_title), close) {
    VeilSectionLabel(stringResource(MR.strings.veil_set_appearance_theme))
    VeilGroup {
      Column(Modifier.selectableGroup()) {
        ThemeOption(MR.strings.veil_theme_gece, MR.strings.veil_theme_gece_desc, mode == VeilThemeMode.Gece) { mode = VeilThemeMode.Gece; VeilTheme.setMode(mode) }
        VeilRowDivider()
        ThemeOption(MR.strings.veil_theme_sis, MR.strings.veil_theme_sis_desc, mode == VeilThemeMode.Sis) { mode = VeilThemeMode.Sis; VeilTheme.setMode(mode) }
        VeilRowDivider()
        ThemeOption(MR.strings.veil_theme_sistem, MR.strings.veil_theme_sistem_desc, mode == VeilThemeMode.Sistem) { mode = VeilThemeMode.Sistem; VeilTheme.setMode(mode) }
      }
    }
    VeilNote(stringResource(MR.strings.veil_set_appearance_theme_note))

    VeilSectionLabel(stringResource(MR.strings.veil_set_appearance_language))
    VeilGroup {
      SectionView(contentPadding = PaddingValues()) {
        AppearanceScope.LangSelector(language) {
          language.value = it
          applyLanguage(it)
        }
      }
    }

    VeilSectionLabel(stringResource(MR.strings.veil_set_appearance_font))
    VeilGroup {
      AppearanceScope.FontScaleSection()
    }
  }
}

@Composable
private fun ThemeOption(title: StringResource, description: StringResource, selected: Boolean, onSelect: () -> Unit) {
  val colors = VeilTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f)) {
      BasicText(stringResource(title), style = VeilTheme.type.bodyLarge.copy(color = colors.ink))
      BasicText(stringResource(description), style = VeilTheme.type.caption.copy(color = colors.ink2))
    }
    if (selected) BasicText("●", style = VeilTheme.type.bodyLarge.copy(color = colors.accent))
  }
}
