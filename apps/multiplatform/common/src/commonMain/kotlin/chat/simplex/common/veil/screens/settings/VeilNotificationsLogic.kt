package chat.simplex.common.veil.screens.settings

import chat.simplex.common.model.NotificationsMode

/** Modes offered on the Veil notifications screen, in the order of the onboarding step. Empty off Android, as upstream. */
fun notificationModeOptions(isAndroid: Boolean): List<NotificationsMode> =
  if (isAndroid) listOf(NotificationsMode.SERVICE, NotificationsMode.PERIODIC, NotificationsMode.OFF) else emptyList()

/** Upstream shows the Xiaomi autostart hint only for the two modes that run in the background. */
fun showXiaomiHint(isXiaomi: Boolean, mode: NotificationsMode): Boolean =
  isXiaomi && (mode == NotificationsMode.PERIODIC || mode == NotificationsMode.SERVICE)

/** Picking the mode that is already active does nothing. */
fun shouldChangeNotificationsMode(current: NotificationsMode, picked: NotificationsMode): Boolean = current != picked
