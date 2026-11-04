package chat.simplex.common.veil.screens.settings

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import chat.simplex.common.helpers.saveAppLocale
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.views.helpers.withApi
import kotlinx.coroutines.delay

@Composable
actual fun rememberScreenProtectionApplier(): (Boolean) -> Unit {
  val context = LocalContext.current
  return remember(context) {
    { on: Boolean ->
      val window = (context as? Activity)?.window
      if (on) window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
      else window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
  }
}

@Composable
actual fun rememberAppLanguageApplier(): (String) -> Unit {
  val context = LocalContext.current
  return remember(context) {
    { code: String ->
      withApi {
        delay(200)
        val activity = context as? Activity
        if (activity != null) {
          if (code == "system") activity.saveAppLocale(appPrefs.appLanguage)
          else activity.saveAppLocale(appPrefs.appLanguage, code)
        }
      }
    }
  }
}
