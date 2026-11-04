package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.CIStatus
import chat.simplex.common.model.MsgReceiptStatus
import chat.simplex.common.model.SndCIStatusProgress
import chat.simplex.common.model.SndError
import chat.simplex.common.veil.screens.chatlist.avatarSeed
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.screens.chat.*'
class VeilChatTest {
  @Test
  fun deliveryMapping() {
    val complete = SndCIStatusProgress.Complete
    val partial = SndCIStatusProgress.Partial
    assertEquals(VeilDeliveryState.Sending, deliveryStateOf(CIStatus.SndNew()))
    assertEquals(VeilDeliveryState.Sent, deliveryStateOf(CIStatus.SndSent(complete)))
    assertEquals(VeilDeliveryState.SentPartly, deliveryStateOf(CIStatus.SndSent(partial)))
    assertEquals(VeilDeliveryState.Delivered, deliveryStateOf(CIStatus.SndRcvd(MsgReceiptStatus.Ok, complete)))
    assertEquals(VeilDeliveryState.DeliveredPartly, deliveryStateOf(CIStatus.SndRcvd(MsgReceiptStatus.Ok, partial)))
    assertEquals(VeilDeliveryState.DeliveredIntegrityWarning, deliveryStateOf(CIStatus.SndRcvd(MsgReceiptStatus.BadMsgHash, complete)))
    assertEquals(VeilDeliveryState.Retrying, deliveryStateOf(CIStatus.SndWarning(SndError.Quota())))
    assertEquals(VeilDeliveryState.Failed, deliveryStateOf(CIStatus.SndErrorAuth()))
    assertEquals(VeilDeliveryState.Failed, deliveryStateOf(CIStatus.CISSndError(SndError.Expired())))
    assertEquals(VeilDeliveryState.Unknown, deliveryStateOf(CIStatus.Invalid("x")))
  }

  @Test
  fun incomingStatusesShowNoState() {
    assertNull(deliveryStateOf(CIStatus.RcvNew()))
    assertNull(deliveryStateOf(CIStatus.RcvRead()))
  }

  @Test
  fun noStateMeansRead() {
    // there is no "read" word for outgoing messages: every state is one of the delivery words
    assertEquals(9, VeilDeliveryState.values().size)
  }

  private val utc = TimeZone.UTC

  @Test
  fun dateDividersStartEachDay() {
    val ts = listOf(
      Instant.parse("2026-03-01T10:00:00Z"),
      Instant.parse("2026-03-01T23:59:00Z"),
      Instant.parse("2026-03-02T00:01:00Z"),
      Instant.parse("2026-03-02T12:00:00Z"),
      Instant.parse("2026-03-05T08:00:00Z"),
    )
    assertEquals(listOf(0, 2, 4), dateDividerIndices(ts, utc))
    assertEquals(emptyList(), dateDividerIndices(emptyList(), utc))
  }

  @Test
  fun dateDividerRespectsTimeZone() {
    val ts = listOf(Instant.parse("2026-03-01T23:30:00Z"), Instant.parse("2026-03-02T00:30:00Z"))
    assertEquals(listOf(0, 1), dateDividerIndices(ts, utc))
    assertEquals(listOf(0), dateDividerIndices(ts, TimeZone.of("Europe/Istanbul"))) // both on 2 March, +03:00
  }

  @Test
  fun dateLabels() {
    val today = LocalDate(2026, 3, 10)
    assertEquals(VeilDateLabel.Today, dateLabelFor(today, today))
    assertEquals(VeilDateLabel.Yesterday, dateLabelFor(LocalDate(2026, 3, 9), today))
    assertEquals(VeilDateLabel.Date(1, 3, 2026), dateLabelFor(LocalDate(2026, 3, 1), today))
    assertEquals(VeilDateLabel.Date(31, 12, 2025), dateLabelFor(LocalDate(2025, 12, 31), today))
  }

  @Test
  fun senderRuns() {
    assertEquals(
      listOf(true, false, true, true, false, false, true),
      senderRunStarts(listOf("a", "a", "b", "me", "me", null, "me"))
    )
    assertEquals(emptyList(), senderRunStarts(emptyList()))
  }

  @Test
  fun senderColorsAreStableAndInRange() {
    val ids = listOf("m1", "m2", "abcDEF", "ç")
    for (id in ids) {
      val i = senderColorIndex(id)
      assertTrue(i in SENDER_NAME_COLORS.indices)
      assertEquals(i, senderColorIndex(id))
    }
  }

  @Test
  fun avatarSeedMatchesChatListRule() {
    // same function as the chat list: chat id + profile display name, alias not involved
    assertEquals(avatarSeed("@12", "Elif Kaya"), chatAvatarSeed("@12", "Elif Kaya"))
    assertEquals("@12|Elif Kaya", chatAvatarSeed("@12", "Elif Kaya"))
    assertNotEquals(chatAvatarSeed("@12", "Elif Kaya"), chatAvatarSeed("@13", "Elif Kaya"))
  }

  @Test
  fun barSubtitleChips() {
    assertEquals(listOf(VeilBarSubtitle.Verified(true)), barSubtitles(true, false, true, 0, false))
    assertEquals(listOf(VeilBarSubtitle.Verified(false)), barSubtitles(true, false, false, 0, false))
    assertEquals(listOf(VeilBarSubtitle.Members(6)), barSubtitles(false, true, false, 6, false))
    assertEquals(listOf(VeilBarSubtitle.Verified(false), VeilBarSubtitle.Incognito), barSubtitles(true, false, false, 0, true))
    assertEquals(emptyList(), barSubtitles(false, false, false, 0, false))
  }
}
