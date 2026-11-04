package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.ChatItem
import chat.simplex.common.platform.FileChooserLauncher

/**
 * "Kaydet" for a received or sent file / image: the same thing upstream's `SaveContentItemAction` does when tapped
 * (images go to the gallery on Android, other files through the system file chooser [launcher]).
 */
expect fun veilSaveItemMedia(cItem: ChatItem, launcher: FileChooserLauncher)
