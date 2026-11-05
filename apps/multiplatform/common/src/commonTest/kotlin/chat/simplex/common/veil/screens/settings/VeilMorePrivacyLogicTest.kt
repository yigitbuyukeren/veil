package chat.simplex.common.veil.screens.settings

import kotlin.test.*

class VeilMorePrivacyLogicTest {
  @Test
  fun blurChoicesAreTheFourStandardOnesInOrder() {
    assertEquals(listOf(0, 12, 24, 48), blurChoices(24).map { it.radius })
    assertTrue(blurChoices(12).all { it.level != null })
  }

  @Test
  fun anImportedRadiusIsKeptAsAnExtraChoice() {
    val choices = blurChoices(30)
    assertEquals(listOf(0, 12, 24, 48, 30), choices.map { it.radius })
    assertNull(choices.last().level)
  }

  @Test
  fun onlyTurningTheDraftOffClearsIt() {
    assertTrue(draftToggleClearsDraft(false))
    assertFalse(draftToggleClearsDraft(true))
  }

  @Test
  fun relayNoteFollowsTheSwitch() {
    assertEquals(RelayNote.Asks, relayNote(true))
    assertEquals(RelayNote.DoesNotAsk, relayNote(false))
  }

  @Test
  fun profileSectionsNeedAProfile() {
    assertTrue(profileSectionsShown(hasUser = true, desktopNoUserNoRemote = false))
    assertFalse(profileSectionsShown(hasUser = false, desktopNoUserNoRemote = false))
    assertFalse(profileSectionsShown(hasUser = true, desktopNoUserNoRemote = true))
  }

  @Test
  fun receiptOverridesCountOnlyChatsThatDifferFromTheNewChoice() {
    val perChat = listOf(null, true, false, false, null)
    assertEquals(1, receiptOverrides(perChat, enable = false))
    assertEquals(2, receiptOverrides(perChat, enable = true))
    assertEquals(0, receiptOverrides(emptyList(), enable = true))
    assertEquals(0, receiptOverrides(listOf(null, null), enable = false))
  }

  @Test
  fun receiptsAreAppliedDirectlyOnlyWhenNothingOverrides() {
    assertEquals(ReceiptsStep.Apply, receiptsStep(0))
    assertEquals(ReceiptsStep.AskAboutOverrides, receiptsStep(3))
  }
}
