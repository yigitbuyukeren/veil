package chat.simplex.common.veil.screens.settings

import SectionView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.views.usersettings.AppearanceScope
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Veil appearance screen. The theme is fixed to Gece for now, so upstream's theme, wallpaper and colour pickers are
 * not offered here (they would have no effect). Language and font size are upstream's controls.
 */
@Composable
fun VeilAppearanceScreen(close: () -> Unit) {
  val applyLanguage = rememberAppLanguageApplier()
  val language = rememberSaveable { mutableStateOf(appPrefs.appLanguage.get() ?: "system") }

  VeilSettingsScaffold(stringResource(MR.strings.veil_set_appearance_title), close) {
    VeilGroup {
      VeilValueRow(
        stringResource(MR.strings.veil_set_appearance_theme),
        sub = stringResource(MR.strings.veil_set_appearance_theme_note),
        readOnly = true,
        value = stringResource(MR.strings.veil_set_appearance_theme_value)
      )
    }

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
