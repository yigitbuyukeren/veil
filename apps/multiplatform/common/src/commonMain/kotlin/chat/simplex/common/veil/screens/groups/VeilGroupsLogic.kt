package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.GroupMember
import chat.simplex.common.model.GroupMemberRole
import chat.simplex.common.model.GroupMemberStatus

/*
 * Pure rules of the Veil group screens (create, info, members, link). Nothing here touches the network or the UI; the
 * permission rules mirror upstream's `GroupMember.canBeRemoved` / `canChangeRoleTo` / `canBlockForAll`,
 * `GroupInfo.canAddMembers` and the sections of `GroupMemberInfoLayout`, so the Veil screens offer exactly what upstream offers.
 */

/** Which chip a role gets. Mapped to a label and a chip style by the screens. */
enum class VeilRoleChip { Owner, Admin, Moderator, Member, Observer, Author, Relay }

fun roleChip(role: GroupMemberRole): VeilRoleChip = when (role) {
  GroupMemberRole.Owner -> VeilRoleChip.Owner
  GroupMemberRole.Admin -> VeilRoleChip.Admin
  GroupMemberRole.Moderator -> VeilRoleChip.Moderator
  GroupMemberRole.Member -> VeilRoleChip.Member
  GroupMemberRole.Observer -> VeilRoleChip.Observer
  GroupMemberRole.Author -> VeilRoleChip.Author
  GroupMemberRole.Relay -> VeilRoleChip.Relay
}

/** A short sentence under the member name, by role. */
enum class VeilRoleHint { Manages, Administers, Moderates, Writes, ReadOnly, Relays }

fun roleHint(role: GroupMemberRole): VeilRoleHint = when (role) {
  GroupMemberRole.Owner -> VeilRoleHint.Manages
  GroupMemberRole.Admin -> VeilRoleHint.Administers
  GroupMemberRole.Moderator -> VeilRoleHint.Moderates
  GroupMemberRole.Member, GroupMemberRole.Author -> VeilRoleHint.Writes
  GroupMemberRole.Observer -> VeilRoleHint.ReadOnly
  GroupMemberRole.Relay -> VeilRoleHint.Relays
}

/** The roles an invitation can grant: upstream offers the selectable roles up to the user's own. */
fun invitableRoles(userRole: GroupMemberRole): List<GroupMemberRole> = GroupMemberRole.selectableRoles.filter { it <= userRole }

/** The roles the group link can give to new members (upstream: member or observer only). */
val linkRoles: List<GroupMemberRole> = listOf(GroupMemberRole.Member, GroupMemberRole.Observer)

/** Upstream: `GroupInfo.canAddMembers`. Admins and owners who are active members may add members and manage the link. */
fun canAddMembers(userRole: GroupMemberRole, userActive: Boolean): Boolean = userRole >= GroupMemberRole.Admin && userActive

/** The Veil group screens cover plain groups; channels, business chats, support scopes and groups not yet joined keep upstream's screens. */
fun veilGroupEligible(useRelays: Boolean, hasBusinessChat: Boolean, nextConnectPrepared: Boolean, hasScope: Boolean): Boolean =
  !useRelays && !hasBusinessChat && !nextConnectPrepared && !hasScope

/** Facts about the user and one member that decide which actions are offered. */
data class VeilMemberFacts(
  val userRole: GroupMemberRole,
  val userActive: Boolean,
  val memberRole: GroupMemberRole,
  val memberStatus: GroupMemberStatus,
  val memberPending: Boolean,
  val blockedByAdmin: Boolean,
  val showMessages: Boolean,
)

fun memberFacts(userRole: GroupMemberRole, userActive: Boolean, m: GroupMember) = VeilMemberFacts(
  userRole, userActive, m.memberRole, m.memberStatus, m.memberPending, m.blockedByAdmin, m.memberSettings.showMessages
)

/** What the signed-in user may do with one member. */
data class VeilMemberPermissions(
  /** Roles the member can be changed to; empty = no role change. */
  val roles: List<GroupMemberRole>,
  val canRemove: Boolean,
  /** Only for a member who was removed: delete their messages. */
  val canDeleteMessages: Boolean,
  val canBlockForAll: Boolean,
  val canUnblockForAll: Boolean,
  /** Local block: only users below moderator get it (moderators block for everyone). */
  val canBlockLocal: Boolean,
  val canUnblockLocal: Boolean,
  /** Blocked by an admin: a read-only note for users who cannot unblock. */
  val blockedByAdminNote: Boolean,
) {
  val any: Boolean get() = roles.isNotEmpty() || canRemove || canDeleteMessages || canBlockForAll || canUnblockForAll || canBlockLocal || canUnblockLocal
}

fun memberPermissions(f: VeilMemberFacts): VeilMemberPermissions {
  val relay = f.memberRole == GroupMemberRole.Relay
  // upstream GroupMember.canBeRemoved
  val canBeRemoved = f.userRole >= GroupMemberRole.Admin && f.userRole >= f.memberRole && f.userActive
  val gone = f.memberStatus == GroupMemberStatus.MemRemoved || f.memberStatus == GroupMemberStatus.MemLeft
  // upstream GroupMember.canChangeRoleTo (plain groups)
  val roles = if (relay || !canBeRemoved || gone || f.memberPending) emptyList()
    else GroupMemberRole.selectableRoles.filter { it <= f.userRole }
  // upstream GroupMember.canBlockForAll
  val canBlockForAll = !relay && f.memberRole < GroupMemberRole.Moderator && f.userRole >= GroupMemberRole.Moderator &&
      f.userRole >= f.memberRole && f.userActive && !f.memberPending
  val moderator = f.userRole >= GroupMemberRole.Moderator
  return VeilMemberPermissions(
    roles = roles,
    canRemove = moderator && canBeRemoved && !gone && !relay,
    canDeleteMessages = moderator && canBeRemoved && f.memberStatus == GroupMemberStatus.MemRemoved,
    canBlockForAll = canBlockForAll && !f.blockedByAdmin,
    canUnblockForAll = canBlockForAll && f.blockedByAdmin,
    canBlockLocal = !moderator && !f.blockedByAdmin && f.showMessages,
    canUnblockLocal = !moderator && !f.blockedByAdmin && !f.showMessages,
    blockedByAdminNote = !moderator && f.blockedByAdmin,
  )
}

/** State of the group link card. */
enum class VeilLinkState { None, Creating, Active, Deleting }

fun linkCardState(hasLink: Boolean, creating: Boolean, deleting: Boolean): VeilLinkState = when {
  deleting -> VeilLinkState.Deleting
  creating -> VeilLinkState.Creating
  hasLink -> VeilLinkState.Active
  else -> VeilLinkState.None
}

/** Removed members can rejoin through a still-active link (finding E24), so the note shows while a link exists and anyone was removed. */
fun showRejoinNote(linkActive: Boolean, removedMembers: Int): Boolean = linkActive && removedMembers > 0

fun removedMembersCount(statuses: List<GroupMemberStatus>): Int = statuses.count { it == GroupMemberStatus.MemRemoved }

/** Members listed in the group: not those who left or were removed. Highest role first. */
fun activeSorted(members: List<GroupMember>): List<GroupMember> =
  members.filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
    .sortedByDescending { it.memberRole }

/** Create button rule (upstream `canCreateProfile`): a non-empty name that passes [isValid] (upstream's `isValidDisplayName`). */
fun canCreateGroup(name: String, isValid: (String) -> Boolean): Boolean = name.trim().isNotEmpty() && isValid(name.trim())

/** Contacts can be invited at creation only when the group is not incognito (upstream creates incognito groups with a link instead). */
fun memberPickerAvailable(incognito: Boolean): Boolean = !incognito
