package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.*
import chat.simplex.common.veil.design.fnv1a
import chat.simplex.common.veil.screens.chatlist.avatarSeed
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/*
 * Non-UI logic of the Veil chat screen, free of Compose so it can be unit tested.
 */

/**
 * What an outgoing message says about its delivery. The words never go beyond what the protocol status means:
 * SimpleX has no "read" state for outgoing messages. `sndRcvd` is a delivery receipt (the recipient's device has the
 * message), so the strongest word is "Teslim edildi" (delivered); "okundu" (read) is never shown.
 */
enum class VeilDeliveryState {
  /** `sndNew`: queued locally, not yet accepted by a relay. */
  Sending,
  /** `sndSent` + complete progress: accepted by the relay. */
  Sent,
  /** `sndSent` + partial progress (groups): accepted for some members only. */
  SentPartly,
  /** `sndRcvd(ok)` + complete: delivery receipt from the recipient. */
  Delivered,
  /** `sndRcvd(ok)` + partial (groups): receipts from some members only. */
  DeliveredPartly,
  /** `sndRcvd(badMsgHash)`: delivered, but the recipient reports an integrity mismatch. */
  DeliveredIntegrityWarning,
  /** `sndWarning`: temporary delivery problem, the agent keeps retrying. */
  Retrying,
  /** `sndErrorAuth` / `sndError`: permanent failure. */
  Failed,
  /** `invalid`: status the app cannot interpret. */
  Unknown,
}

/** Delivery state of an outgoing item; null for incoming statuses (no state is shown on received messages). */
fun deliveryStateOf(status: CIStatus): VeilDeliveryState? = when (status) {
  is CIStatus.SndNew -> VeilDeliveryState.Sending
  is CIStatus.SndSent -> if (status.sndProgress == SndCIStatusProgress.Complete) VeilDeliveryState.Sent else VeilDeliveryState.SentPartly
  is CIStatus.SndRcvd -> when (status.msgRcptStatus) {
    MsgReceiptStatus.Ok -> if (status.sndProgress == SndCIStatusProgress.Complete) VeilDeliveryState.Delivered else VeilDeliveryState.DeliveredPartly
    MsgReceiptStatus.BadMsgHash -> VeilDeliveryState.DeliveredIntegrityWarning
  }
  is CIStatus.SndWarning -> VeilDeliveryState.Retrying
  is CIStatus.SndErrorAuth, is CIStatus.CISSndError -> VeilDeliveryState.Failed
  is CIStatus.Invalid -> VeilDeliveryState.Unknown
  is CIStatus.RcvNew, is CIStatus.RcvRead -> null
}

/** What a date divider says. */
sealed class VeilDateLabel {
  object Today : VeilDateLabel()
  object Yesterday : VeilDateLabel()
  /** [month] is 1..12. */
  data class Date(val day: Int, val month: Int, val year: Int) : VeilDateLabel()
}

fun dateLabelFor(date: LocalDate, today: LocalDate): VeilDateLabel {
  val days = today.toEpochDays() - date.toEpochDays()
  return when (days) {
    0 -> VeilDateLabel.Today
    1 -> VeilDateLabel.Yesterday
    else -> VeilDateLabel.Date(date.dayOfMonth, date.monthNumber, date.year)
  }
}

fun localDateOf(ts: Instant, zone: TimeZone): LocalDate = ts.toLocalDateTime(zone).date

/**
 * Indices (into [timestamps], oldest first) of items that start a new calendar day and therefore get a divider
 * above them. The first item always gets one.
 */
fun dateDividerIndices(timestamps: List<Instant>, zone: TimeZone): List<Int> {
  val out = ArrayList<Int>()
  var prev: LocalDate? = null
  timestamps.forEachIndexed { i, ts ->
    val d = localDateOf(ts, zone)
    if (d != prev) out += i
    prev = d
  }
  return out
}

/**
 * For items oldest first, each entry is the sender key of a message (member id, or "me") or null for anything
 * that is not a message (events, dividers). Result: true where the item starts a run of one sender, which is where
 * the group sender name and avatar are shown. A non-message item (null) ends the run.
 */
fun senderRunStarts(senders: List<String?>): List<Boolean> {
  var prev: String? = null
  return senders.map { s ->
    val starts = s != null && s != prev
    prev = s
    starts
  }
}

/** Palette for group sender names (ARGB), all legible on the dark bubbles. */
val SENDER_NAME_COLORS: List<Long> = listOf(0xFF7CF5D3, 0xFFB9B4E6, 0xFFFF8FB1, 0xFFFFD58A, 0xFF8FD3FF, 0xFFC6F28B)

/** Stable palette slot of a group member, so a name keeps its colour across sessions. */
fun senderColorIndex(memberId: String): Int = (fnv1a(memberId) % SENDER_NAME_COLORS.size).toInt()

/**
 * Seed of a chat's mask avatar in the top bar. It is the same rule as the chat list ([avatarSeed]: chat id plus the
 * profile's own display name, no local alias), so a contact's mask is identical in the list and in the chat.
 */
fun chatAvatarSeed(chatId: String, profileDisplayName: String): String = avatarSeed(chatId, profileDisplayName)

/** What the top bar shows under the name. */
sealed class VeilBarSubtitle {
  data class Verified(val verified: Boolean) : VeilBarSubtitle()
  data class Members(val count: Long) : VeilBarSubtitle()
  object Incognito : VeilBarSubtitle()
}

/** Subtitle chips for a chat: direct = verification chip; group = member count; incognito adds its own chip. */
fun barSubtitles(isDirect: Boolean, isGroup: Boolean, verified: Boolean, memberCount: Long, incognito: Boolean): List<VeilBarSubtitle> = buildList {
  if (isDirect) add(VeilBarSubtitle.Verified(verified))
  if (isGroup) add(VeilBarSubtitle.Members(memberCount))
  if (incognito) add(VeilBarSubtitle.Incognito)
}

/** The composer is locked while no Tor proxy answers: nothing can be typed into the send path then. */
fun composerLocked(torUnavailable: Boolean): Boolean = torUnavailable
