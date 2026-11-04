package chat.simplex.common.veil.screens.settings

import chat.simplex.common.model.NetCfg
import chat.simplex.common.model.NotificationPreviewMode
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.model.VeilNetworkPolicy

/*
 * Non-UI logic of the Veil settings (screen group F). Free of Compose and of resources so it can be unit tested:
 * builders return keys, the screens turn keys into Turkish strings.
 */

/** The rows of the settings home, in display order. */
enum class VeilSettingsRow { Profiles, Privacy, Notifications, Appearance, Database, Tor, About }

/** One piece of a one-line row summary; [arg] carries a number or a version where the text needs one. */
enum class SummaryKey {
  Profiles, ProfilesHidden,
  PreviewsOn, PreviewsOff, ScreenProtected, AppLock, NotificationPreviewHidden,
  NotificationsOff, NotificationsPeriodic, NotificationsService,
  ThemeNight,
  DbEncrypted, DbPlain, DbUnknown,
  Version,
}

data class SummaryPart(val key: SummaryKey, val arg: String? = null)

/** Joins rendered parts into the one-line summary. */
fun List<SummaryPart>.joinSummary(render: (SummaryPart) -> String, separator: String = " · "): String =
  joinToString(separator) { render(it) }

fun profilesSummary(total: Int, hidden: Int): List<SummaryPart> =
  buildList {
    add(SummaryPart(SummaryKey.Profiles, total.toString()))
    if (hidden > 0) add(SummaryPart(SummaryKey.ProfilesHidden, hidden.toString()))
  }

fun privacySummary(
  showPreviewsInList: Boolean,
  screenProtected: Boolean,
  appLockOn: Boolean,
  notificationPreview: NotificationPreviewMode,
): List<SummaryPart> =
  buildList {
    add(SummaryPart(if (showPreviewsInList) SummaryKey.PreviewsOn else SummaryKey.PreviewsOff))
    if (notificationPreview == NotificationPreviewMode.HIDDEN) add(SummaryPart(SummaryKey.NotificationPreviewHidden))
    if (screenProtected) add(SummaryPart(SummaryKey.ScreenProtected))
    if (appLockOn) add(SummaryPart(SummaryKey.AppLock))
  }

fun notificationsSummary(mode: NotificationsMode): List<SummaryPart> = listOf(
  SummaryPart(
    when (mode) {
      NotificationsMode.OFF -> SummaryKey.NotificationsOff
      NotificationsMode.PERIODIC -> SummaryKey.NotificationsPeriodic
      NotificationsMode.SERVICE -> SummaryKey.NotificationsService
    }
  )
)

/** The theme is fixed to Gece for now. */
fun appearanceSummary(): List<SummaryPart> = listOf(SummaryPart(SummaryKey.ThemeNight))

/** [encrypted] is null while the app has not read the database state. */
fun databaseSummary(encrypted: Boolean?): List<SummaryPart> = listOf(
  SummaryPart(
    when (encrypted) {
      true -> SummaryKey.DbEncrypted
      false -> SummaryKey.DbPlain
      null -> SummaryKey.DbUnknown
    }
  )
)

fun aboutSummary(version: String): List<SummaryPart> = listOf(SummaryPart(SummaryKey.Version, version))

/** The locked network rows of the Tor screen. */
enum class NetRowKind { Path, Socks, Direct, NoTor }

/** [value] is set where the value is data (the proxy address); other rows get their fixed text from resources. */
data class NetRow(val kind: NetRowKind, val value: String?, val readOnly: Boolean = true)

/** The four rows, all read-only: the policy cannot be changed from the UI (see docs/TOR_ONLY.md). */
fun veilNetworkRows(): List<NetRow> = listOf(
  NetRow(NetRowKind.Path, null),
  NetRow(NetRowKind.Socks, "${VeilNetworkPolicy.SOCKS_HOST}:${VeilNetworkPolicy.SOCKS_PORT}"),
  NetRow(NetRowKind.Direct, null),
  NetRow(NetRowKind.NoTor, null),
)

/** State of the "active core policy" line. */
enum class PolicyLineState { Checking, Ok, NotCompliant, Unknown }

/**
 * [checked] is false until the core has been asked; [activeConfig] is what the core reports as active
 * (`apiGetNetworkConfig`), null when the call failed.
 */
fun policyLineState(checked: Boolean, activeConfig: NetCfg?): PolicyLineState = when {
  !checked -> PolicyLineState.Checking
  activeConfig == null -> PolicyLineState.Unknown
  VeilNetworkPolicy.isCompliant(activeConfig) -> PolicyLineState.Ok
  else -> PolicyLineState.NotCompliant
}

enum class PrivacyDefaultKind { Incognito, LinkPreviewsOff, CallsOff }

/** [ok] is true when the default holds as Veil intends it. */
data class PrivacyDefaultRow(val kind: PrivacyDefaultKind, val ok: Boolean)

fun privacyDefaultRows(
  incognito: Boolean,
  linkPreviewsEnabled: Boolean = VeilNetworkPolicy.LINK_PREVIEWS_ENABLED,
  callsEnabled: Boolean = VeilNetworkPolicy.CALLS_ENABLED,
): List<PrivacyDefaultRow> = listOf(
  PrivacyDefaultRow(PrivacyDefaultKind.Incognito, incognito),
  PrivacyDefaultRow(PrivacyDefaultKind.LinkPreviewsOff, !linkPreviewsEnabled),
  PrivacyDefaultRow(PrivacyDefaultKind.CallsOff, !callsEnabled),
)
