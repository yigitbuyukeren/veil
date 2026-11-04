package chat.simplex.common.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.veil.VeilUi
import chat.simplex.common.veil.screens.system.VeilSplash

@Composable
fun SplashView(nonTransparent: Boolean = false) {
  // Veil: night background and the Veil mark (Android).
  if (VeilUi.system && appPlatform.isAndroid) {
    VeilSplash()
    return
  }
  Surface(
    Modifier
      .fillMaxSize(),
    color = if (nonTransparent) MaterialTheme.colors.background.copy(1f) else MaterialTheme.colors.background,
    contentColor = LocalContentColor.current
  ) {
//    Image(
//      painter = painterResource(MR.images.logo),
//      contentDescription = "Simplex Icon",
//      modifier = Modifier
//        .height(230.dp)
//        .align(Alignment.Center)
//    )
  }
}
