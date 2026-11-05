package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.ChatFeature
import chat.simplex.common.model.ChatItemTTL

/*
 * Non-UI decisions of the Veil contact detail screens (advanced, preferences, pending connection), free of Compose so
 * they can be unit tested. Every rule here mirrors a condition in upstream's `ChatInfoView`, `ContactPreferencesView`
 * or `ContactConnectionInfoView`.
 */

// ---- advanced (upstream ChatInfoView) ----

/** Which blocks of the advanced screen are shown. Same conditions as upstream's `ChatInfoLayout`. */
data class AdvancedBlocks(
  /** Delivery receipt choice: only for a contact that is connected and not deleted. */
  val sendReceipts: Boolean,
  /** "Fix connection": only when the agent says an encryption re-sync is allowed. */
  val fixConnection: Boolean,
  /** Contact address with QR and share. */
  val address: Boolean,
  /** Encryption line. */
  val encryption: Boolean,
  /** Network status, address switch and server lists. */
  val servers: Boolean,
  /** Local name, database id and delivery debug: only with developer tools on. */
  val developer: Boolean,
)

fun advancedBlocks(
  ready: Boolean,
  active: Boolean,
  ratchetSyncAllowed: Boolean,
  hasContactLink: Boolean,
  hasActiveConn: Boolean,
  developerTools: Boolean,
): AdvancedBlocks {
  val live = ready && active
  return AdvancedBlocks(
    sendReceipts = live,
    fixConnection = live && ratchetSyncAllowed,
    address = hasContactLink,
    encryption = hasActiveConn,
    servers = live,
    developer = developerTools,
  )
}

/** What the receiving queues say about an address switch (one entry per queue). */
data class QueueSwitch(val switching: Boolean, val canAbort: Boolean)

/** "Change receiving address" is off while any queue is switching or while encryption is being re-synced. */
fun switchAddressDisabled(queues: List<QueueSwitch>, syncSendProhibited: Boolean): Boolean =
  queues.any { it.switching } || syncSendProhibited

/** "Abort changing address" appears only while a switch is under way. */
fun abortSwitchShown(queues: List<QueueSwitch>): Boolean = queues.any { it.switching }

/** ... and works only when every switching queue can still be aborted. */
fun abortSwitchDisabled(queues: List<QueueSwitch>, syncSendProhibited: Boolean): Boolean =
  queues.any { it.switching && !it.canAbort } || syncSendProhibited

/** Server address as upstream lists it: without the key part before the `@`. */
fun serverHost(server: String): String = server.substringAfter("@")

fun serverHosts(servers: List<String>): String = servers.joinToString(separator = ", ") { serverHost(it) }

/** What a tap on a server line copies: the full addresses, comma separated (upstream copies them the same way). */
fun serverCopyText(servers: List<String>): String = servers.joinToString(separator = ",")

/**
 * Values offered for "delete messages after" on a chat, in upstream's order: never, year, month, week, day; a custom
 * value already set is kept as an extra choice; `null` (follow the app default) comes last when there is a default.
 */
fun chatTtlOptions(current: ChatItemTTL?, hasDefault: Boolean): List<ChatItemTTL?> {
  val all = mutableListOf<ChatItemTTL?>(ChatItemTTL.None, ChatItemTTL.Year, ChatItemTTL.Month, ChatItemTTL.Week, ChatItemTTL.Day)
  if (current is ChatItemTTL.Seconds) all.add(current)
  if (hasDefault) all.add(null)
  return all
}

/** Whether picking [selected] needs upstream's confirmation: only a change does. */
fun chatTtlChanged(selected: ChatItemTTL?, current: ChatItemTTL?): Boolean = selected != current

// ---- preferences (upstream ContactPreferencesView) ----

/** The features shown on the preferences screen, in upstream's order. Calls appear only when calls are turned on. */
fun preferenceFeatures(callsAllowed: Boolean): List<ChatFeature> = buildList {
  add(ChatFeature.TimedMessages)
  add(ChatFeature.FullDelete)
  add(ChatFeature.Reactions)
  add(ChatFeature.Voice)
  if (callsAllowed) add(ChatFeature.Calls)
}

enum class PrefsBack { Close, AskToSave }

/** Back with unsaved changes asks upstream's "Save preferences?" question; without changes it just closes. */
fun prefsBackAction(changed: Boolean): PrefsBack = if (changed) PrefsBack.AskToSave else PrefsBack.Close

/** Reset and save are available only while something differs from what is stored. */
fun prefsActionsEnabled(changed: Boolean): Boolean = changed

/** Turning disappearing messages on keeps the chosen time, or one day when none was chosen (upstream's default). */
const val DEFAULT_TIMED_TTL_SECONDS = 86400

fun timedMessagesTtlOnToggle(allow: Boolean, current: Int?): Int? = if (allow) current ?: DEFAULT_TIMED_TTL_SECONDS else null

/**
 * The time stored after the "you allow" switch of disappearing messages changes: [timedMessagesTtlOnToggle], and when that
 * is none (switched off) the time saved on the contact, so switching on again brings it back (as upstream does).
 */
fun timedMessagesTtlAfterToggle(allow: Boolean, current: Int?, stored: Int?): Int? = timedMessagesTtlOnToggle(allow, current) ?: stored

/** Times offered for disappearing messages: upstream's list; a stored custom time is kept as an extra choice before "off". */
fun timedTtlOptions(standard: List<Int?>, current: Int?): List<Int?> {
  if (standard.contains(current)) return standard
  val withoutOff = standard.filterNotNull()
  return withoutOff + current + standard.filter { it == null }
}

/** The contact's own disappearing time is shown when they allow the feature and you do not. */
fun showContactTtl(youAllow: Boolean, contactAllows: Boolean): Boolean = !youAllow && contactAllows

// ---- pending connection (upstream ContactConnectionInfoView) ----

enum class ConnectionWait { GroupHost, ConnectionRequest, ContactDevice }

/** Which "you will be connected when ..." sentence goes under the title. */
fun connectionWait(viaContactUri: Boolean, hasGroupLink: Boolean): ConnectionWait = when {
  !viaContactUri -> ConnectionWait.ContactDevice
  hasGroupLink -> ConnectionWait.GroupHost
  else -> ConnectionWait.ConnectionRequest
}

/** A connection made through a group link has no alias to set. */
fun connectionAliasEditable(hasGroupLink: Boolean): Boolean = !hasGroupLink

/** The invitation link block is shown for a link you created and that has content. */
fun connectionLinkShown(linkHasContent: Boolean, initiated: Boolean): Boolean = linkHasContent && initiated

/** The short/full link toggle is a developer option and exists only when the link has both forms. */
fun connectionLinkToggleShown(developerTools: Boolean, hasShortLink: Boolean): Boolean = developerTools && hasShortLink
