package chat.simplex.common.veil.data

import chat.simplex.common.model.NotificationPreviewMode

/** Chat ids after switching the "no content in this chat's notifications" setting of [chatId]. */
fun hiddenAfterToggle(hidden: List<String>, chatId: String): List<String> =
  if (chatId in hidden) hidden - chatId else hidden + chatId

/**
 * Preview mode a notification of one chat uses. A chat with hidden content never shows more than the contact name;
 * a global mode that is already stricter wins.
 */
fun effectivePreviewMode(global: NotificationPreviewMode, chatContentHidden: Boolean): NotificationPreviewMode =
  if (chatContentHidden && global == NotificationPreviewMode.MESSAGE) NotificationPreviewMode.CONTACT else global

/** Same decision from the stored mode name; an unknown or missing value is treated as the most hidden mode. */
fun effectivePreviewModeName(globalName: String?, chatContentHidden: Boolean): NotificationPreviewMode {
  val global = NotificationPreviewMode.values().firstOrNull { it.name == globalName } ?: NotificationPreviewMode.HIDDEN
  return effectivePreviewMode(global, chatContentHidden)
}
