package chat.simplex.common.veil.screens.chat

import chat.simplex.common.model.ChatFeature
import chat.simplex.common.model.ChatItemTTL
import kotlin.test.*

class VeilContactLogicTest {
  @Test
  fun advancedBlocksFollowUpstreamConditions() {
    val live = advancedBlocks(ready = true, active = true, ratchetSyncAllowed = true, hasContactLink = true, hasActiveConn = true, developerTools = false)
    assertTrue(live.sendReceipts && live.fixConnection && live.address && live.encryption && live.servers)
    assertFalse(live.developer)

    val connecting = advancedBlocks(ready = false, active = true, ratchetSyncAllowed = true, hasContactLink = false, hasActiveConn = false, developerTools = true)
    assertFalse(connecting.sendReceipts)
    assertFalse(connecting.fixConnection)
    assertFalse(connecting.servers)
    assertFalse(connecting.address)
    assertFalse(connecting.encryption)
    assertTrue(connecting.developer)

    val deleted = advancedBlocks(ready = true, active = false, ratchetSyncAllowed = false, hasContactLink = false, hasActiveConn = true, developerTools = false)
    assertFalse(deleted.sendReceipts)
    assertFalse(deleted.servers)
  }

  @Test
  fun fixConnectionNeedsTheAgentToAllowIt() {
    assertFalse(advancedBlocks(true, true, false, false, true, false).fixConnection)
  }

  @Test
  fun switchAddressIsOffWhileSwitchingOrSyncing() {
    val idle = listOf(QueueSwitch(false, false), QueueSwitch(false, true))
    assertFalse(switchAddressDisabled(idle, syncSendProhibited = false))
    assertTrue(switchAddressDisabled(idle, syncSendProhibited = true))
    assertTrue(switchAddressDisabled(listOf(QueueSwitch(true, true)), syncSendProhibited = false))
    assertFalse(switchAddressDisabled(emptyList(), syncSendProhibited = false))
  }

  @Test
  fun abortSwitchShowsOnlyWhileSwitchingAndNeedsEveryQueueAbortable() {
    assertFalse(abortSwitchShown(listOf(QueueSwitch(false, false))))
    assertTrue(abortSwitchShown(listOf(QueueSwitch(false, false), QueueSwitch(true, true))))
    assertFalse(abortSwitchDisabled(listOf(QueueSwitch(true, true)), syncSendProhibited = false))
    assertTrue(abortSwitchDisabled(listOf(QueueSwitch(true, true), QueueSwitch(true, false)), syncSendProhibited = false))
    assertTrue(abortSwitchDisabled(listOf(QueueSwitch(true, true)), syncSendProhibited = true))
    // a queue that is not switching does not block aborting the others
    assertFalse(abortSwitchDisabled(listOf(QueueSwitch(false, false), QueueSwitch(true, true)), syncSendProhibited = false))
  }

  @Test
  fun serverTextDropsTheKeyPartButCopiesFullAddresses() {
    val servers = listOf("smp://key1@host1.example", "smp://key2@host2.example")
    assertEquals("host1.example", serverHost(servers[0]))
    assertEquals("host1.example, host2.example", serverHosts(servers))
    assertEquals("smp://key1@host1.example,smp://key2@host2.example", serverCopyText(servers))
    assertEquals("nokey.example", serverHost("nokey.example"))
  }

  @Test
  fun chatTtlOptionsKeepUpstreamsOrder() {
    assertEquals(
      listOf(ChatItemTTL.None, ChatItemTTL.Year, ChatItemTTL.Month, ChatItemTTL.Week, ChatItemTTL.Day, null),
      chatTtlOptions(ChatItemTTL.Week, hasDefault = true),
    )
    assertEquals(
      listOf(ChatItemTTL.None, ChatItemTTL.Year, ChatItemTTL.Month, ChatItemTTL.Week, ChatItemTTL.Day),
      chatTtlOptions(null, hasDefault = false),
    )
  }

  @Test
  fun aCustomChatTtlIsKeptAsAnExtraChoice() {
    val custom = ChatItemTTL.Seconds(3600)
    val options = chatTtlOptions(custom, hasDefault = true)
    assertEquals(7, options.size)
    assertEquals(custom, options[5])
    assertNull(options.last())
  }

  @Test
  fun onlyAChangedChatTtlAsksForConfirmation() {
    assertFalse(chatTtlChanged(ChatItemTTL.Day, ChatItemTTL.Day))
    assertTrue(chatTtlChanged(ChatItemTTL.Day, ChatItemTTL.Week))
    assertTrue(chatTtlChanged(null, ChatItemTTL.Week))
    assertFalse(chatTtlChanged(null, null))
  }

  @Test
  fun callsPreferenceAppearsOnlyWhenCallsAreAllowed() {
    val off = preferenceFeatures(callsAllowed = false)
    assertEquals(listOf(ChatFeature.TimedMessages, ChatFeature.FullDelete, ChatFeature.Reactions, ChatFeature.Voice), off)
    assertFalse(off.contains(ChatFeature.Calls))
    assertEquals(ChatFeature.Calls, preferenceFeatures(callsAllowed = true).last())
  }

  @Test
  fun leavingPreferencesAsksOnlyWithUnsavedChanges() {
    assertEquals(PrefsBack.Close, prefsBackAction(changed = false))
    assertEquals(PrefsBack.AskToSave, prefsBackAction(changed = true))
    assertFalse(prefsActionsEnabled(false))
    assertTrue(prefsActionsEnabled(true))
  }

  @Test
  fun disappearingMessagesToggleKeepsOrRestoresTheTime() {
    // switching on without a chosen time uses one day
    assertEquals(86400, timedMessagesTtlOnToggle(true, null))
    assertEquals(3600, timedMessagesTtlOnToggle(true, 3600))
    assertNull(timedMessagesTtlOnToggle(false, 3600))
    // switching off falls back to the time stored on the contact
    assertEquals(600, timedMessagesTtlAfterToggle(false, 3600, 600))
    assertNull(timedMessagesTtlAfterToggle(false, 3600, null))
    assertEquals(3600, timedMessagesTtlAfterToggle(true, 3600, 600))
  }

  @Test
  fun aCustomDisappearingTimeIsKeptBeforeOff() {
    val standard = listOf(600, 3600, null)
    assertEquals(standard, timedTtlOptions(standard, 3600))
    assertEquals(standard, timedTtlOptions(standard, null))
    assertEquals(listOf(600, 3600, 7200, null), timedTtlOptions(standard, 7200))
  }

  @Test
  fun theContactsTimeShowsOnlyWhenOnlyTheyAllowDisappearingMessages() {
    assertTrue(showContactTtl(youAllow = false, contactAllows = true))
    assertFalse(showContactTtl(youAllow = true, contactAllows = true))
    assertFalse(showContactTtl(youAllow = false, contactAllows = false))
  }

  @Test
  fun connectionWaitSentenceFollowsHowTheConnectionWasMade() {
    assertEquals(ConnectionWait.ContactDevice, connectionWait(viaContactUri = false, hasGroupLink = false))
    assertEquals(ConnectionWait.ConnectionRequest, connectionWait(viaContactUri = true, hasGroupLink = false))
    assertEquals(ConnectionWait.GroupHost, connectionWait(viaContactUri = true, hasGroupLink = true))
    assertEquals(ConnectionWait.ContactDevice, connectionWait(viaContactUri = false, hasGroupLink = true))
  }

  @Test
  fun connectionAliasAndLinkRules() {
    assertTrue(connectionAliasEditable(hasGroupLink = false))
    assertFalse(connectionAliasEditable(hasGroupLink = true))
    assertTrue(connectionLinkShown(linkHasContent = true, initiated = true))
    assertFalse(connectionLinkShown(linkHasContent = true, initiated = false))
    assertFalse(connectionLinkShown(linkHasContent = false, initiated = true))
    assertTrue(connectionLinkToggleShown(developerTools = true, hasShortLink = true))
    assertFalse(connectionLinkToggleShown(developerTools = false, hasShortLink = true))
    assertFalse(connectionLinkToggleShown(developerTools = true, hasShortLink = false))
  }
}
