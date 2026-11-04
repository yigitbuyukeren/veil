package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.MsgContent
import chat.simplex.common.platform.FileChooserLauncher
import chat.simplex.common.views.helpers.withLongRunningApi

actual fun veilSaveItemMedia(cItem: ChatItem, launcher: FileChooserLauncher) {
  when (cItem.content.msgContent) {
    is MsgContent.MCImage, is MsgContent.MCFile, is MsgContent.MCVoice, is MsgContent.MCVideo -> withLongRunningApi { launcher.launch(cItem.file?.fileName ?: "") }
    else -> {}
  }
}
