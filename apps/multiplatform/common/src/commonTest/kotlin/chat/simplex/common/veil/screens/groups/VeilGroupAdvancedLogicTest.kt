package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.GroupMemberRole
import chat.simplex.common.model.GroupMemberStatus
import chat.simplex.common.veil.screens.chat.QueueSwitch
import kotlin.test.*

class VeilGroupAdvancedLogicTest {
  private val owner = GroupMemberRole.Owner
  private val admin = GroupMemberRole.Admin
  private val member = GroupMemberRole.Member
  private val observer = GroupMemberRole.Observer

  private fun m(id: Long, role: GroupMemberRole, status: GroupMemberStatus = GroupMemberStatus.MemComplete, blocked: Boolean = false) =
    BulkMember(id, role, status, blocked)

  @Test
  fun advancedBlocksFollowUpstreamConditions() {
    val mod = groupAdvancedBlocks(GroupMemberRole.Moderator, true, false, canModerate = true, supportOn = true, hasOwnSupportChat = false, developerTools = false)
    assertTrue(mod.memberSupport && mod.reports && mod.bulkMembers)
    assertFalse(mod.userSupport)
    assertFalse(mod.developer)

    val plain = groupAdvancedBlocks(member, true, false, canModerate = false, supportOn = true, hasOwnSupportChat = false, developerTools = true)
    assertTrue(plain.userSupport && plain.developer)
    assertFalse(plain.memberSupport || plain.reports || plain.bulkMembers)

    // support off: the own support chat shows only when one already exists
    assertFalse(groupAdvancedBlocks(member, true, false, false, supportOn = false, hasOwnSupportChat = false, developerTools = false).userSupport)
    assertTrue(groupAdvancedBlocks(member, true, false, false, supportOn = false, hasOwnSupportChat = true, developerTools = false).userSupport)

    // a business chat has no member support list; an inactive user gets no support chat and no bulk actions
    assertFalse(groupAdvancedBlocks(owner, true, hasBusinessChat = true, true, true, false, false).memberSupport)
    val left = groupAdvancedBlocks(owner, false, false, true, true, true, false)
    assertFalse(left.userSupport || left.bulkMembers)
  }

  @Test
  fun receiptsStopAboveTheLimit() {
    assertTrue(receiptsAvailable(20, 20))
    assertFalse(receiptsAvailable(21, 20))
  }

  @Test
  fun onlyMembersBelowModeratorCanBeSelected() {
    assertTrue(canSelectMember(admin, true, member))
    assertTrue(canSelectMember(owner, true, observer))
    assertFalse(canSelectMember(admin, true, GroupMemberRole.Moderator))
    assertFalse(canSelectMember(admin, true, admin))
    assertFalse(canSelectMember(admin, false, member))
    assertFalse(canSelectMember(observer, true, member))
  }

  @Test
  fun bulkActionsNeedASelection() {
    val none = bulkActions(admin, true, emptyList())
    assertFalse(none.remove || none.blockForAll || none.unblockForAll)
    assertNull(none.roleTarget)
    // members the user cannot select are ignored
    val ignored = bulkActions(admin, true, listOf(m(1, GroupMemberRole.Moderator)))
    assertFalse(ignored.remove)
    assertNull(ignored.roleTarget)
  }

  @Test
  fun bulkBlockAndUnblockNeedOneState() {
    assertTrue(bulkActions(admin, true, listOf(m(1, member), m(2, observer))).blockForAll)
    assertFalse(bulkActions(admin, true, listOf(m(1, member), m(2, observer))).unblockForAll)
    val blocked = bulkActions(admin, true, listOf(m(1, member, blocked = true), m(2, observer, blocked = true)))
    assertTrue(blocked.unblockForAll)
    assertFalse(blocked.blockForAll)
    val mixed = bulkActions(admin, true, listOf(m(1, member, blocked = true), m(2, member)))
    assertFalse(mixed.blockForAll || mixed.unblockForAll)
  }

  @Test
  fun bulkRoleGoesToTheOtherRoleOnly() {
    assertEquals(GroupMemberRole.Member, bulkActions(admin, true, listOf(m(1, observer))).roleTarget)
    assertEquals(GroupMemberRole.Observer, bulkActions(admin, true, listOf(m(1, member))).roleTarget)
    // observers and members together: no single target
    assertNull(bulkActions(admin, true, listOf(m(1, member), m(2, observer))).roleTarget)
    // an author could go either way, so no single target
    val author = bulkActions(admin, true, listOf(m(1, GroupMemberRole.Author)))
    assertNull(author.roleTarget)
  }

  @Test
  fun bulkRemoveNeedsEveryoneStillInTheGroup() {
    assertTrue(bulkActions(admin, true, listOf(m(1, member))).remove)
    assertFalse(bulkActions(admin, true, listOf(m(1, member), m(2, member, GroupMemberStatus.MemRemoved))).remove)
    assertFalse(bulkActions(admin, true, listOf(m(1, member, GroupMemberStatus.MemLeft))).remove)
  }

  @Test
  fun selectAllTogglesBetweenAllAndNone() {
    val selectable = setOf(1L, 2L, 3L)
    assertEquals(selectable, toggleSelectAll(selectable, emptySet()))
    assertEquals(selectable, toggleSelectAll(selectable, setOf(1L)))
    assertEquals(setOf(9L), toggleSelectAll(selectable, selectable + 9L))
    assertEquals(emptySet(), toggleSelectAll(emptySet(), emptySet()))
    assertEquals(setOf(1L), toggleSelected(emptySet(), 1))
    assertEquals(emptySet(), toggleSelected(setOf(1L), 1))
  }

  @Test
  fun incognitoContactsCannotJoinAMainProfileGroup() {
    assertTrue(prohibitedToInviteIncognito(membershipIncognito = false, contactConnIncognito = true))
    assertFalse(prohibitedToInviteIncognito(membershipIncognito = true, contactConnIncognito = true))
    assertFalse(prohibitedToInviteIncognito(membershipIncognito = false, contactConnIncognito = false))
  }

  @Test
  fun inviteNeedsSelectionAndNoRunningInvite() {
    assertFalse(inviteEnabled(0, false))
    assertTrue(inviteEnabled(2, false))
    assertFalse(inviteEnabled(2, true))
    assertEquals(listOf(1L, 2L), toggleContact(listOf(1L), 2))
    assertEquals(listOf(1L), toggleContact(listOf(1L, 2L), 2))
  }

  @Test
  fun messageButtonFollowsUpstream() {
    assertEquals(MemberMessage.OpenChat, memberMessage(hasKnownDirectChat = true, hasContactId = true, directMessagesOn = false))
    assertEquals(MemberMessage.Prohibited, memberMessage(hasKnownDirectChat = false, hasContactId = true, directMessagesOn = false))
    assertEquals(MemberMessage.OpenChat, memberMessage(false, hasContactId = true, directMessagesOn = true))
    assertEquals(MemberMessage.CreateContact, memberMessage(false, hasContactId = false, directMessagesOn = true))
  }

  @Test
  fun newContactLooksOffOnlyWhenItCannotBeCreatedOrFixed() {
    assertTrue(messageLooksDisabled(MemberMessage.CreateContact, sendMsgEnabled = false, ratchetSyncAllowed = false))
    assertFalse(messageLooksDisabled(MemberMessage.CreateContact, sendMsgEnabled = false, ratchetSyncAllowed = true))
    assertFalse(messageLooksDisabled(MemberMessage.CreateContact, sendMsgEnabled = true, ratchetSyncAllowed = false))
    assertFalse(messageLooksDisabled(MemberMessage.OpenChat, sendMsgEnabled = false, ratchetSyncAllowed = false))
  }

  @Test
  fun createContactStepsFollowUpstreamOrder() {
    assertEquals(CreateContactStep.Create, createContactStep(true, false, false, false))
    assertEquals(CreateContactStep.Nothing, createContactStep(false, false, true, true))
    assertEquals(CreateContactStep.OfferFix, createContactStep(false, true, true, true))
    assertEquals(CreateContactStep.RenegotiationInProgress, createContactStep(false, true, false, true))
    assertEquals(CreateContactStep.NotReady, createContactStep(false, true, false, false))
  }

  @Test
  fun memberSupportAndFixNeedAnActiveMember() {
    assertTrue(memberSupportShown(GroupMemberRole.Moderator, member, supportOn = true, memberHasSupportChat = false, memberActive = true))
    assertFalse(memberSupportShown(GroupMemberRole.Moderator, member, supportOn = true, memberHasSupportChat = false, memberActive = false))
    assertFalse(memberSupportShown(member, member, true, true, true))
    assertFalse(memberSupportShown(owner, GroupMemberRole.Relay, true, true, true))
    // a moderator is reached only if a support chat already exists
    assertFalse(memberSupportShown(owner, GroupMemberRole.Moderator, true, false, true))
    assertTrue(memberSupportShown(owner, GroupMemberRole.Moderator, true, true, true))
    assertTrue(memberFixShown(memberActive = true, ratchetSyncAllowed = true))
    assertFalse(memberFixShown(memberActive = false, ratchetSyncAllowed = true))
    assertFalse(memberFixShown(memberActive = true, ratchetSyncAllowed = false))
  }

  @Test
  fun connectViaAddressMatchesUpstream() {
    assertTrue(connectViaAddressShown(hasContactId = false, hasKnownDirectChat = false, directMessagesOn = true))
    assertTrue(connectViaAddressShown(hasContactId = true, hasKnownDirectChat = false, directMessagesOn = false))
    assertFalse(connectViaAddressShown(hasContactId = true, hasKnownDirectChat = false, directMessagesOn = true))
    assertFalse(connectViaAddressShown(hasContactId = true, hasKnownDirectChat = true, directMessagesOn = false))
  }

  @Test
  fun addressSwitchIsOffWhileSwitchingOrWhenSendingIsOff() {
    val idle = listOf(QueueSwitch(switching = false, canAbort = false))
    val busy = listOf(QueueSwitch(switching = true, canAbort = true))
    val stuck = listOf(QueueSwitch(switching = true, canAbort = false))
    assertFalse(memberSwitchDisabled(idle, true))
    assertTrue(memberSwitchDisabled(busy, true))
    assertTrue(memberSwitchDisabled(idle, false))
    assertFalse(memberAbortDisabled(busy, true))
    assertTrue(memberAbortDisabled(stuck, true))
    assertTrue(memberAbortDisabled(busy, false))
    assertTrue(connectionIsDirect(0))
    assertFalse(connectionIsDirect(2))
  }
}
