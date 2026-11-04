package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.GroupMemberRole
import chat.simplex.common.model.GroupMemberRole.*
import chat.simplex.common.model.GroupMemberStatus
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.*'
class VeilGroupsLogicTest {
  private fun facts(
    user: GroupMemberRole,
    member: GroupMemberRole,
    status: GroupMemberStatus = GroupMemberStatus.MemComplete,
    userActive: Boolean = true,
    pending: Boolean = false,
    blockedByAdmin: Boolean = false,
    showMessages: Boolean = true,
  ) = VeilMemberFacts(user, userActive, member, status, pending, blockedByAdmin, showMessages)

  @Test
  fun everyRoleHasItsOwnChip() {
    assertEquals(VeilRoleChip.Owner, roleChip(Owner))
    assertEquals(VeilRoleChip.Admin, roleChip(Admin))
    assertEquals(VeilRoleChip.Moderator, roleChip(Moderator))
    assertEquals(VeilRoleChip.Member, roleChip(Member))
    assertEquals(VeilRoleChip.Observer, roleChip(Observer))
    assertEquals(VeilRoleChip.Author, roleChip(Author))
    assertEquals(VeilRoleChip.Relay, roleChip(Relay))
    assertEquals(GroupMemberRole.values().size, GroupMemberRole.values().map { roleChip(it) }.toSet().size)
  }

  @Test
  fun observerIsReadOnlyAndOwnerManages() {
    assertEquals(VeilRoleHint.ReadOnly, roleHint(Observer))
    assertEquals(VeilRoleHint.Manages, roleHint(Owner))
    assertEquals(VeilRoleHint.Writes, roleHint(Member))
  }

  @Test
  fun onlyAdminsAndOwnersAddMembers() {
    assertTrue(canAddMembers(Owner, true))
    assertTrue(canAddMembers(Admin, true))
    assertFalse(canAddMembers(Moderator, true))
    assertFalse(canAddMembers(Member, true))
    assertFalse(canAddMembers(Observer, true))
    assertFalse(canAddMembers(Owner, false))
  }

  @Test
  fun invitationRolesStopAtTheUsersOwnRole() {
    assertEquals(listOf(Observer, Member, Moderator, Admin, Owner), invitableRoles(Owner))
    assertEquals(listOf(Observer, Member, Moderator, Admin), invitableRoles(Admin))
    assertEquals(listOf(Observer, Member), invitableRoles(Member))
  }

  @Test
  fun groupLinkOffersMemberAndObserver() {
    assertEquals(listOf(Member, Observer), linkRoles)
  }

  @Test
  fun ownerCanDoEverythingToAMember() {
    val p = memberPermissions(facts(Owner, Member))
    assertEquals(listOf(Observer, Member, Moderator, Admin, Owner), p.roles)
    assertTrue(p.canRemove)
    assertTrue(p.canBlockForAll)
    assertFalse(p.canUnblockForAll)
    assertFalse(p.canBlockLocal)
    assertFalse(p.canDeleteMessages)
  }

  @Test
  fun adminCannotTouchOwnersButCanManagePeers() {
    val ownerTarget = memberPermissions(facts(Admin, Owner))
    assertFalse(ownerTarget.any)
    val adminTarget = memberPermissions(facts(Admin, Admin))
    assertTrue(adminTarget.canRemove)
    // an admin cannot hand out the owner role
    assertEquals(listOf(Observer, Member, Moderator, Admin), adminTarget.roles)
  }

  @Test
  fun moderatorBlocksForAllButCannotRemoveOrChangeRoles() {
    val p = memberPermissions(facts(Moderator, Member))
    assertTrue(p.roles.isEmpty())
    assertFalse(p.canRemove)
    assertTrue(p.canBlockForAll)
    assertFalse(p.canBlockLocal)
    // moderators do not block other moderators for all
    assertFalse(memberPermissions(facts(Moderator, Moderator)).canBlockForAll)
  }

  @Test
  fun adminBlockedMemberCanBeUnblockedForAllByModerators() {
    val p = memberPermissions(facts(Admin, Member, blockedByAdmin = true))
    assertFalse(p.canBlockForAll)
    assertTrue(p.canUnblockForAll)
  }

  @Test
  fun plainMembersOnlyBlockLocally() {
    val p = memberPermissions(facts(Member, Member))
    assertTrue(p.roles.isEmpty())
    assertFalse(p.canRemove)
    assertTrue(p.canBlockLocal)
    assertFalse(p.canUnblockLocal)
    val blocked = memberPermissions(facts(Observer, Member, showMessages = false))
    assertTrue(blocked.canUnblockLocal)
    assertFalse(blocked.canBlockLocal)
  }

  @Test
  fun blockedByAdminIsAReadOnlyNoteForPlainMembers() {
    val p = memberPermissions(facts(Member, Member, blockedByAdmin = true))
    assertTrue(p.blockedByAdminNote)
    assertFalse(p.canBlockLocal)
    assertFalse(p.canUnblockLocal)
  }

  @Test
  fun removedMemberOffersOnlyMessageDeletion() {
    val p = memberPermissions(facts(Owner, Member, status = GroupMemberStatus.MemRemoved))
    assertTrue(p.roles.isEmpty())
    assertFalse(p.canRemove)
    assertTrue(p.canDeleteMessages)
    val left = memberPermissions(facts(Owner, Member, status = GroupMemberStatus.MemLeft))
    assertFalse(left.canRemove)
    assertFalse(left.canDeleteMessages)
  }

  @Test
  fun pendingMemberHasNoRoleChangeOrBlockForAll() {
    val p = memberPermissions(facts(Owner, Member, status = GroupMemberStatus.MemPendingApproval, pending = true))
    assertTrue(p.roles.isEmpty())
    assertFalse(p.canBlockForAll)
  }

  @Test
  fun inactiveUserCannotManage() {
    val p = memberPermissions(facts(Owner, Member, userActive = false))
    assertFalse(p.canRemove)
    assertFalse(p.canBlockForAll)
    assertTrue(p.roles.isEmpty())
  }

  @Test
  fun linkCardStates() {
    assertEquals(VeilLinkState.None, linkCardState(hasLink = false, creating = false, deleting = false))
    assertEquals(VeilLinkState.Active, linkCardState(hasLink = true, creating = false, deleting = false))
    assertEquals(VeilLinkState.Creating, linkCardState(hasLink = false, creating = true, deleting = false))
    assertEquals(VeilLinkState.Deleting, linkCardState(hasLink = true, creating = false, deleting = true))
    // deleting wins over everything else
    assertEquals(VeilLinkState.Deleting, linkCardState(hasLink = true, creating = true, deleting = true))
  }

  @Test
  fun rejoinNoteNeedsAnActiveLinkAndARemovedMember() {
    assertTrue(showRejoinNote(linkActive = true, removedMembers = 1))
    assertFalse(showRejoinNote(linkActive = false, removedMembers = 3))
    assertFalse(showRejoinNote(linkActive = true, removedMembers = 0))
  }

  @Test
  fun removedMembersAreCountedByStatus() {
    val statuses = listOf(GroupMemberStatus.MemComplete, GroupMemberStatus.MemRemoved, GroupMemberStatus.MemLeft, GroupMemberStatus.MemRemoved)
    assertEquals(2, removedMembersCount(statuses))
  }

  @Test
  fun createNeedsAValidName() {
    val valid = { s: String -> !s.contains("@") }
    assertFalse(canCreateGroup("", valid))
    assertFalse(canCreateGroup("   ", valid))
    assertFalse(canCreateGroup("a@b", valid))
    assertTrue(canCreateGroup("  Hafta Sonu  ", valid))
  }

  @Test
  fun incognitoGroupsHaveNoMemberPicker() {
    assertTrue(memberPickerAvailable(incognito = false))
    assertFalse(memberPickerAvailable(incognito = true))
  }

  @Test
  fun onlyPlainJoinedGroupsUseTheVeilScreens() {
    assertTrue(veilGroupEligible(useRelays = false, hasBusinessChat = false, nextConnectPrepared = false, hasScope = false))
    assertFalse(veilGroupEligible(useRelays = true, hasBusinessChat = false, nextConnectPrepared = false, hasScope = false))
    assertFalse(veilGroupEligible(useRelays = false, hasBusinessChat = true, nextConnectPrepared = false, hasScope = false))
    assertFalse(veilGroupEligible(useRelays = false, hasBusinessChat = false, nextConnectPrepared = true, hasScope = false))
    assertFalse(veilGroupEligible(useRelays = false, hasBusinessChat = false, nextConnectPrepared = false, hasScope = true))
  }
}
