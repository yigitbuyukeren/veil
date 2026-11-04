package chat.simplex.common.veil.screens.newchat

import chat.simplex.common.veil.design.TorRingState
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.*'
class VeilNewChatLogicTest {
  @Test
  fun menuHasTheFourEntriesInOrder() {
    assertEquals(listOf(NewChatEntry.Invite, NewChatEntry.Paste, NewChatEntry.Scan, NewChatEntry.Group), newChatEntries)
  }

  @Test
  fun entriesOpenTheirPageAndGroupLeavesTheSheet() {
    assertEquals(NewChatPage.Invite, NewChatEntry.Invite.page())
    assertEquals(NewChatPage.Paste, NewChatEntry.Paste.page())
    assertEquals(NewChatPage.Scan, NewChatEntry.Scan.page())
    assertNull(NewChatEntry.Group.page())
  }

  @Test
  fun noLinkAndNoTorMeansUnavailable() {
    assertEquals(InviteViewState.TorUnavailable, inviteViewState(TorRingState.Unavailable, hasLink = false, creating = false, failed = false))
    assertEquals(InviteViewState.TorUnavailable, inviteViewState(TorRingState.Unavailable, hasLink = false, creating = true, failed = true))
  }

  @Test
  fun existingLinkIsShownEvenWithoutTor() {
    for (ring in TorRingState.values()) {
      assertEquals(InviteViewState.Ready, inviteViewState(ring, hasLink = true, creating = false, failed = false))
    }
  }

  @Test
  fun creatingAndFailedStates() {
    assertEquals(InviteViewState.Creating, inviteViewState(TorRingState.ProxyAnswers, hasLink = false, creating = true, failed = false))
    assertEquals(InviteViewState.Failed, inviteViewState(TorRingState.ProxyAnswers, hasLink = false, creating = false, failed = true))
    assertEquals(InviteViewState.Failed, inviteViewState(TorRingState.Connecting, hasLink = false, creating = false, failed = true))
    // before creation starts the page already reads as creating
    assertEquals(InviteViewState.Creating, inviteViewState(TorRingState.Connecting, hasLink = false, creating = false, failed = false))
  }

  @Test
  fun creationStartsOnlyOnceAndNeverWithoutTor() {
    assertTrue(shouldCreateInvitation(TorRingState.ProxyAnswers, hasLink = false, creating = false, failed = false))
    assertTrue(shouldCreateInvitation(TorRingState.Connecting, hasLink = false, creating = false, failed = false))
    assertFalse(shouldCreateInvitation(TorRingState.Unavailable, hasLink = false, creating = false, failed = false))
    assertFalse(shouldCreateInvitation(TorRingState.ProxyAnswers, hasLink = true, creating = false, failed = false))
    assertFalse(shouldCreateInvitation(TorRingState.ProxyAnswers, hasLink = false, creating = true, failed = false))
    assertFalse(shouldCreateInvitation(TorRingState.ProxyAnswers, hasLink = false, creating = false, failed = true))
  }

  @Test
  fun retryNeedsTorAndNoCreationInFlight() {
    assertTrue(canRetryInvitation(TorRingState.ProxyAnswers, creating = false))
    assertFalse(canRetryInvitation(TorRingState.Unavailable, creating = false))
    assertFalse(canRetryInvitation(TorRingState.ProxyAnswers, creating = true))
  }

  @Test
  fun onlyAReadyLinkCanBeSharedOrCopied() {
    assertTrue(canShareInvitation(InviteViewState.Ready))
    for (s in listOf(InviteViewState.TorUnavailable, InviteViewState.Creating, InviteViewState.Failed)) assertFalse(canShareInvitation(s))
  }

  @Test
  fun incognitoPrefersTheConnectionsOwnFlag() {
    assertTrue(invitationIncognito(null, true))
    assertFalse(invitationIncognito(null, false))
    assertFalse(invitationIncognito(false, true))
    assertTrue(invitationIncognito(true, false))
  }

  @Test
  fun pastedTextIsTrimmedAndBlankIsNotReady() {
    assertEquals("https://example.org/x", cleanPastedLink("  https://example.org/x\n"))
    assertFalse(pasteInputReady("", connecting = false))
    assertFalse(pasteInputReady(" \n\t ", connecting = false))
    assertTrue(pasteInputReady(" simplex:/contact#abc ", connecting = false))
    assertFalse(pasteInputReady("simplex:/contact#abc", connecting = true))
  }
}
