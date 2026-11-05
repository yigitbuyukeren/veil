package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.GroupMemberRole
import chat.simplex.common.model.GroupMemberStatus
import chat.simplex.common.veil.screens.chat.QueueSwitch

/*
 * Pure rules of the Veil group "Gelişmiş" page, the add-members page and the member details page. Every condition
 * mirrors one in upstream's `GroupChatInfoLayout`, `GroupMembersToolbar`, `AddGroupMembersView` or
 * `GroupMemberInfoLayout`; nothing here touches the network or the UI.
 */

// ---- group "Gelişmiş" (upstream GroupChatInfoView) ----

/** Which blocks of the group advanced page are shown (plain groups; channels and business chats keep upstream's screens). */
data class GroupAdvancedBlocks(
  /** The list of support chats with members: moderators and above, not in business chats. */
  val memberSupport: Boolean,
  /** Reports from members: `GroupInfo.canModerate`. */
  val reports: Boolean,
  /** The user's own chat with the admins. */
  val userSupport: Boolean,
  /** Selecting several members at once: moderators and above who are active. */
  val bulkMembers: Boolean,
  val developer: Boolean,
)

fun groupAdvancedBlocks(
  userRole: GroupMemberRole,
  userActive: Boolean,
  hasBusinessChat: Boolean,
  canModerate: Boolean,
  supportOn: Boolean,
  hasOwnSupportChat: Boolean,
  developerTools: Boolean,
) = GroupAdvancedBlocks(
  memberSupport = !hasBusinessChat && userRole >= GroupMemberRole.Moderator,
  reports = canModerate,
  userSupport = userActive && ((supportOn && userRole < GroupMemberRole.Moderator) || hasOwnSupportChat),
  bulkMembers = userActive && userRole >= GroupMemberRole.Moderator,
  developer = developerTools,
)

/** Delivery receipts are sent only in groups of at most [limit] current members (upstream `SMALL_GROUPS_RCPS_MEM_LIMIT`). */
fun receiptsAvailable(currentMembers: Int, limit: Int): Boolean = currentMembers <= limit

// ---- several members at once (upstream SelectedItemsMembersToolbar) ----

/** The facts about one member that decide the bulk actions. */
data class BulkMember(val id: Long, val role: GroupMemberRole, val status: GroupMemberStatus, val blockedByAdmin: Boolean)

/** Upstream: a member can be selected when the user is active, is at least as high and the member is below moderator. */
fun canSelectMember(userRole: GroupMemberRole, userActive: Boolean, memberRole: GroupMemberRole): Boolean =
  userActive && userRole >= memberRole && memberRole < GroupMemberRole.Moderator

/** What the toolbar over the selected members offers. */
data class BulkActions(
  val remove: Boolean,
  val blockForAll: Boolean,
  val unblockForAll: Boolean,
  /** Role the selected members are changed to, or null when the role button is off. */
  val roleTarget: GroupMemberRole?,
)

/**
 * Same rules as upstream's `recheckItems`: delete only when nobody selected is already removed or gone; block and
 * unblock need all selected to be in the same state (one of the two is offered); the role button turns members into
 * members if none is a member yet, otherwise into observers if none is an observer yet. Nothing is offered for an
 * empty selection.
 */
fun bulkActions(userRole: GroupMemberRole, userActive: Boolean, selected: List<BulkMember>): BulkActions {
  val valid = selected.filter { canSelectMember(userRole, userActive, it.role) }
  if (valid.isEmpty()) return BulkActions(remove = false, blockForAll = false, unblockForAll = false, roleTarget = null)
  val remove = valid.all { it.status != GroupMemberStatus.MemRemoved && it.status != GroupMemberStatus.MemLeft }
  val allUnblocked = valid.all { !it.blockedByAdmin }
  val allBlocked = valid.all { it.blockedByAdmin }
  val toMember = valid.all { it.role != GroupMemberRole.Member }
  val toObserver = valid.all { it.role != GroupMemberRole.Observer }
  val target = when {
    toMember && !toObserver -> GroupMemberRole.Member
    !toMember && toObserver -> GroupMemberRole.Observer
    else -> null
  }
  return BulkActions(remove = remove, blockForAll = allUnblocked && !allBlocked, unblockForAll = allBlocked && !allUnblocked, roleTarget = target)
}

/** Select all / clear (upstream's counter toolbar): when every selectable member is already selected they are cleared, otherwise all are added. */
fun toggleSelectAll(selectable: Set<Long>, selected: Set<Long>): Set<Long> =
  if (selectable.isNotEmpty() && selected.containsAll(selectable)) selected - selectable else selected + selectable

fun toggleSelected(selected: Set<Long>, id: Long): Set<Long> = if (id in selected) selected - id else selected + id

// ---- add members (upstream AddGroupMembersView) ----

/** A contact reached with a random profile cannot be invited from a group made with the main profile (upstream alert `invite_prohibited`). */
fun prohibitedToInviteIncognito(membershipIncognito: Boolean, contactConnIncognito: Boolean): Boolean =
  !membershipIncognito && contactConnIncognito

fun toggleContact(selected: List<Long>, id: Long): List<Long> = if (id in selected) selected - id else selected + id

/** The invite button needs a selection and no invitation under way. */
fun inviteEnabled(selectedCount: Int, inviting: Boolean): Boolean = selectedCount > 0 && !inviting

// ---- member details (upstream GroupMemberInfoView) ----

/** What the message button of a member does. */
enum class MemberMessage { OpenChat, CreateContact, Prohibited }

/**
 * Upstream: a member with a known direct chat opens it; otherwise, if the group allows direct messages, an old contact
 * opens and a member without one gets a new contact created; if direct messages are off an alert says so.
 */
fun memberMessage(hasKnownDirectChat: Boolean, hasContactId: Boolean, directMessagesOn: Boolean): MemberMessage = when {
  hasKnownDirectChat -> MemberMessage.OpenChat
  !directMessagesOn -> MemberMessage.Prohibited
  hasContactId -> MemberMessage.OpenChat
  else -> MemberMessage.CreateContact
}

/** The message button looks off for a new contact while the connection cannot send and cannot be fixed. */
fun messageLooksDisabled(message: MemberMessage, sendMsgEnabled: Boolean, ratchetSyncAllowed: Boolean): Boolean =
  message == MemberMessage.CreateContact && !(sendMsgEnabled || ratchetSyncAllowed)

/** What happens when a new member contact is requested. */
enum class CreateContactStep { Create, OfferFix, RenegotiationInProgress, NotReady, Nothing }

fun createContactStep(sendMsgEnabled: Boolean, statsKnown: Boolean, ratchetSyncAllowed: Boolean, ratchetSyncInProgress: Boolean): CreateContactStep = when {
  sendMsgEnabled -> CreateContactStep.Create
  !statsKnown -> CreateContactStep.Nothing
  ratchetSyncAllowed -> CreateContactStep.OfferFix
  ratchetSyncInProgress -> CreateContactStep.RenegotiationInProgress
  else -> CreateContactStep.NotReady
}

/** The support chat with this member (upstream `showMemberSupportChat`, for a member who is not opened from a support chat). */
fun memberSupportShown(userRole: GroupMemberRole, memberRole: GroupMemberRole, supportOn: Boolean, memberHasSupportChat: Boolean, memberActive: Boolean): Boolean =
  memberActive && userRole >= GroupMemberRole.Moderator && memberRole != GroupMemberRole.Relay &&
    ((supportOn && memberRole < GroupMemberRole.Moderator) || memberHasSupportChat)

/** Fix connection: only for an active member whose connection the agent says can be re-synced. */
fun memberFixShown(memberActive: Boolean, ratchetSyncAllowed: Boolean): Boolean = memberActive && ratchetSyncAllowed

/** The "connect via address" button: for a member whose address is known and who has no direct chat with the user yet (upstream's rule). */
fun connectViaAddressShown(hasContactId: Boolean, hasKnownDirectChat: Boolean, directMessagesOn: Boolean): Boolean =
  if (hasContactId) !hasKnownDirectChat && !directMessagesOn else true

/** Changing the receiving address is off while any queue switches or while the member cannot be written to. */
fun memberSwitchDisabled(queues: List<QueueSwitch>, sendMsgEnabled: Boolean): Boolean =
  queues.any { it.switching } || !sendMsgEnabled

fun memberAbortDisabled(queues: List<QueueSwitch>, sendMsgEnabled: Boolean): Boolean =
  queues.any { it.switching && !it.canAbort } || !sendMsgEnabled

/** The connection level line of the developer block: level 0 is a direct connection. */
fun connectionIsDirect(connLevel: Int): Boolean = connLevel == 0
