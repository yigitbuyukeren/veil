package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.*

/*
 * Pure rules of the Veil group preferences screen. They mirror upstream's `GroupPreferencesView` (which features are
 * listed for a group or a channel, what flipping a switch writes into the preferences, when unsaved changes are
 * asked about). Nothing here touches the network or the UI; the save itself is upstream's `apiUpdateGroup`.
 */

/** One preference row. [disabled] rows are shown but cannot be changed; [relayNote] adds upstream's relay explanation. */
data class VeilPrefRow(val feature: GroupFeature, val disabled: Boolean = false, val relayNote: Boolean = false)

/**
 * Features in upstream's order. Groups list ten (reports and chats with admins are shown, locked, as upstream); channels
 * (groups that use relays) list five, and signing messages stays hidden as upstream. There is no calls preference in a
 * group, so nothing here depends on `VeilNetworkPolicy.callsAllowed()`.
 */
fun prefRows(useRelays: Boolean): List<VeilPrefRow> =
  if (!useRelays) listOf(
    VeilPrefRow(GroupFeature.TimedMessages),
    VeilPrefRow(GroupFeature.DirectMessages),
    VeilPrefRow(GroupFeature.FullDelete),
    VeilPrefRow(GroupFeature.Reactions),
    VeilPrefRow(GroupFeature.Voice),
    VeilPrefRow(GroupFeature.Files),
    VeilPrefRow(GroupFeature.SimplexLinks),
    VeilPrefRow(GroupFeature.Reports, disabled = true),
    VeilPrefRow(GroupFeature.History),
    VeilPrefRow(GroupFeature.Support, disabled = true),
  ) else listOf(
    VeilPrefRow(GroupFeature.TimedMessages),
    VeilPrefRow(GroupFeature.FullDelete),
    VeilPrefRow(GroupFeature.Reactions),
    VeilPrefRow(GroupFeature.History),
    VeilPrefRow(GroupFeature.Support, relayNote = true),
  )

/** The member admission entry exists for plain groups only (not channels, not business chats). */
fun showMemberAdmission(useRelays: Boolean, isBusinessChat: Boolean): Boolean = !useRelays && !isBusinessChat

enum class VeilPrefsTitle { Channel, Group, Chat }

fun prefsTitle(useRelays: Boolean, isBusinessChat: Boolean): VeilPrefsTitle =
  if (useRelays) VeilPrefsTitle.Channel else if (!isBusinessChat) VeilPrefsTitle.Group else VeilPrefsTitle.Chat

fun enabledOf(p: FullGroupPreferences, f: GroupFeature): GroupFeatureEnabled = when (f) {
  GroupFeature.TimedMessages -> p.timedMessages.enable
  GroupFeature.DirectMessages -> p.directMessages.enable
  GroupFeature.FullDelete -> p.fullDelete.enable
  GroupFeature.Reactions -> p.reactions.enable
  GroupFeature.Voice -> p.voice.enable
  GroupFeature.Files -> p.files.enable
  GroupFeature.SimplexLinks -> p.simplexLinks.enable
  GroupFeature.Reports -> p.reports.enable
  GroupFeature.History -> p.history.enable
  GroupFeature.Support -> p.support.enable
  GroupFeature.SignMessages -> p.signMessages.enable
}

/** The role a feature is limited to; null for "all members" and for features without a role. */
fun roleOf(p: FullGroupPreferences, f: GroupFeature): GroupMemberRole? = when (f) {
  GroupFeature.DirectMessages -> p.directMessages.role
  GroupFeature.Voice -> p.voice.role
  GroupFeature.Files -> p.files.role
  GroupFeature.SimplexLinks -> p.simplexLinks.role
  else -> null
}

/** Upstream's "Enabled for" choices, in its order. */
val featureRoles: List<GroupMemberRole?> = listOf(null, GroupMemberRole.Moderator, GroupMemberRole.Admin, GroupMemberRole.Owner)

/** The "Enabled for" row shows only for an enabled feature that has a role. */
fun showRoleRow(p: FullGroupPreferences, f: GroupFeature): Boolean = f.hasRole && enabledOf(p, f) == GroupFeatureEnabled.ON

/** The "Delete after" row shows only while disappearing messages are on. */
fun showTtlRow(p: FullGroupPreferences): Boolean = p.timedMessages.enable == GroupFeatureEnabled.ON

private fun write(p: FullGroupPreferences, f: GroupFeature, enable: GroupFeatureEnabled, role: GroupMemberRole?): FullGroupPreferences = when (f) {
  GroupFeature.TimedMessages -> p.copy(timedMessages = p.timedMessages.copy(enable = enable))
  GroupFeature.DirectMessages -> p.copy(directMessages = RoleGroupPreference(enable = enable, role))
  GroupFeature.FullDelete -> p.copy(fullDelete = GroupPreference(enable = enable))
  GroupFeature.Reactions -> p.copy(reactions = GroupPreference(enable = enable))
  GroupFeature.Voice -> p.copy(voice = RoleGroupPreference(enable = enable, role))
  GroupFeature.Files -> p.copy(files = RoleGroupPreference(enable = enable, role))
  GroupFeature.SimplexLinks -> p.copy(simplexLinks = RoleGroupPreference(enable = enable, role))
  GroupFeature.Reports -> p.copy(reports = GroupPreference(enable = enable))
  GroupFeature.History -> p.copy(history = GroupPreference(enable = enable))
  GroupFeature.Support -> p.copy(support = GroupPreference(enable = enable))
  GroupFeature.SignMessages -> p.copy(signMessages = GroupPreference(enable = enable))
}

/** Default disappearing time when the feature is switched on without one (upstream: one day). */
const val DEFAULT_GROUP_TTL_SECONDS = 86400

/**
 * Flipping a switch, as upstream writes it. Switching disappearing messages on keeps the chosen time or falls back to one
 * day; switching it off restores the saved time ([current]). Switching a role feature off clears its role; switching it on
 * keeps the role it has.
 */
fun toggled(p: FullGroupPreferences, current: FullGroupPreferences, f: GroupFeature, on: Boolean): FullGroupPreferences {
  val enable = if (on) GroupFeatureEnabled.ON else GroupFeatureEnabled.OFF
  if (f == GroupFeature.TimedMessages) {
    return p.copy(timedMessages = TimedMessagesGroupPreference(enable = enable, ttl = if (on) p.timedMessages.ttl ?: DEFAULT_GROUP_TTL_SECONDS else current.timedMessages.ttl))
  }
  return write(p, f, enable, if (on) roleOf(p, f) else null)
}

/** Choosing the role of an enabled feature (only features that have a role). */
fun withRole(p: FullGroupPreferences, f: GroupFeature, role: GroupMemberRole?): FullGroupPreferences =
  if (showRoleRow(p, f)) write(p, f, enabledOf(p, f), role) else p

fun withTtl(p: FullGroupPreferences, ttl: Int?): FullGroupPreferences = p.copy(timedMessages = p.timedMessages.copy(ttl = ttl))

/** Values of the time choice: upstream's fixed list, plus the current one when it is not in it. "Custom" is added by the screen. */
fun ttlOptions(selected: Int?): List<Int?> {
  val base = TimedMessagesPreference.ttlValues
  return if (base.contains(selected)) base else base + selected
}

fun hasUnsavedPrefs(p: FullGroupPreferences, current: FullGroupPreferences): Boolean = p != current

enum class VeilPrefsClose { Close, AskToSave }

/** Leaving with edits asks first; the choices are upstream's (save and notify, or exit without saving). */
fun prefsCloseAction(p: FullGroupPreferences, current: FullGroupPreferences): VeilPrefsClose =
  if (hasUnsavedPrefs(p, current)) VeilPrefsClose.AskToSave else VeilPrefsClose.Close
