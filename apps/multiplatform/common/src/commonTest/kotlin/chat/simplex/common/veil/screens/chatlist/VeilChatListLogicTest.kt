package chat.simplex.common.veil.screens.chatlist

import chat.simplex.common.veil.design.TorRingState
import chat.simplex.common.views.chatlist.ActiveFilter
import chat.simplex.common.views.chatlist.PresetTagKind
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.*'
class VeilChatListLogicTest {
  @Test
  fun ringIsUnavailableWhenProbeFailsWhateverTheCoreDoes() {
    for (running in listOf(true, false, null)) assertEquals(TorRingState.Unavailable, torRingStateFor(false, running))
  }

  @Test
  fun ringShowsProxyAnswersOnlyWhenProbeAnswersAndCoreRuns() {
    assertEquals(TorRingState.ProxyAnswers, torRingStateFor(true, true))
    assertEquals(TorRingState.Connecting, torRingStateFor(true, false))
    assertEquals(TorRingState.Connecting, torRingStateFor(true, null))
  }

  @Test
  fun ringIsConnectingBeforeFirstProbe() {
    for (running in listOf(true, false, null)) assertEquals(TorRingState.Connecting, torRingStateFor(null, running))
  }

  @Test
  fun filtersMapToUpstreamFilters() {
    assertNull(VeilListFilter.All.toActiveFilter())
    assertEquals(ActiveFilter.Unread, VeilListFilter.Unread.toActiveFilter())
    assertEquals(ActiveFilter.PresetTag(PresetTagKind.GROUPS), VeilListFilter.Groups.toActiveFilter())
  }

  @Test
  fun previewSearchIsCaseAndTurkishIFolded() {
    assertTrue(previewMatchesQuery("Akşam yemeğe geliyor musun?", "  yemeğe "))
    assertTrue(previewMatchesQuery("Akşam yemeğe geliyor musun?", "AKŞAM"))
    assertTrue(previewMatchesQuery("IŞIK yanıyor", "ışık"))
    assertTrue(previewMatchesQuery("ISIK yanıyor", "ısık"))
    assertTrue(previewMatchesQuery("İyi geceler", "iyi"))
    assertFalse(previewMatchesQuery("İyi geceler", "gündüz"))
  }

  @Test
  fun blankQueryMatchesAndMissingPreviewNeverMatches() {
    assertTrue(previewMatchesQuery("anything", "   "))
    assertTrue(previewMatchesQuery(null, ""))
    assertFalse(previewMatchesQuery(null, "a"))
  }

  @Test
  fun searchKeepsNameMatchesAndPreviewMatchesInOrder() {
    val previews = mapOf("@1" to "Cumartesi buluşuyoruz", "@2" to "Tamam", "#3" to "dosya geliyor", "@4" to null)
    val ids = listOf("@1", "@2", "#3", "@4")
    // "@2" matches by name only, "#3" by preview only
    assertEquals(listOf("@2", "#3"), searchResultIds(ids, setOf("@2"), { previews[it] }, "dosya"))
    assertEquals(listOf("#3"), searchResultIds(ids, emptySet(), { previews[it] }, "dosya"))
    assertEquals(listOf("@2", "#3"), searchResultIds(ids, setOf("@2", "#3"), { previews[it] }, "tamam"))
    assertEquals(ids, searchResultIds(ids, emptySet(), { previews[it] }, "  "))
    assertEquals(emptyList(), searchResultIds(ids, emptySet(), { previews[it] }, "yok"))
  }

  @Test
  fun avatarSeedIsStableAndUsesIdAndProfileName() {
    assertEquals("@12|Elif Kaya", avatarSeed("@12", "Elif Kaya"))
    assertEquals(avatarSeed("@12", "Elif Kaya"), avatarSeed("@12", "Elif Kaya"))
    assertNotEquals(avatarSeed("@12", "Elif Kaya"), avatarSeed("@13", "Elif Kaya"))
    assertNotEquals(avatarSeed("@12", "Elif Kaya"), avatarSeed("#12", "Elif Kaya"))
  }

  @Test
  fun emptyStatePicksTheRightMessage() {
    assertEquals(VeilListEmpty.None, listEmptyKind(5, 2, VeilListFilter.All, ""))
    assertEquals(VeilListEmpty.NoChats, listEmptyKind(0, 0, VeilListFilter.All, ""))
    assertEquals(VeilListEmpty.NoChats, listEmptyKind(0, 0, VeilListFilter.Unread, ""))
    assertEquals(VeilListEmpty.NoUnread, listEmptyKind(4, 0, VeilListFilter.Unread, ""))
    assertEquals(VeilListEmpty.NoGroups, listEmptyKind(4, 0, VeilListFilter.Groups, ""))
    assertEquals(VeilListEmpty.NoSearchResults, listEmptyKind(4, 0, VeilListFilter.Unread, "xyz"))
    assertEquals(VeilListEmpty.NoSearchResults, listEmptyKind(0, 0, VeilListFilter.All, "xyz"))
  }

  @Test
  fun revealsAreDroppedWhenTheAppLeavesTheForeground() {
    assertTrue(shouldDropReveals(wasVisible = true, isVisible = false))
    assertFalse(shouldDropReveals(wasVisible = false, isVisible = true))
    assertFalse(shouldDropReveals(wasVisible = true, isVisible = true))
  }

  @Test
  fun unreadCountShownOnlyWhenPositive() {
    assertFalse(showUnreadCount(0))
    assertTrue(showUnreadCount(3))
  }
}
