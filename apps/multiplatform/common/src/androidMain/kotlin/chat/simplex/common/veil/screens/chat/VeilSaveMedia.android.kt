package chat.simplex.common.veil.screens.chat

import android.os.Build
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.MsgContent
import chat.simplex.common.platform.FileChooserLauncher
import chat.simplex.common.platform.saveImage
import chat.simplex.common.views.helpers.withLongRunningApi

actual fun veilSaveItemMedia(cItem: ChatItem, launcher: FileChooserLauncher) {
  val name = cItem.file?.fileName ?: ""
  when (cItem.content.msgContent) {
    is MsgContent.MCImage ->
      // From Android 11 images are written to the gallery without a storage permission; before that the file chooser is used
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) saveImage(cItem.file) else withLongRunningApi { launcher.launch(name) }
    is MsgContent.MCFile, is MsgContent.MCVoice, is MsgContent.MCVideo -> withLongRunningApi { launcher.launch(name) }
    else -> {}
  }
}
