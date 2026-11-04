package chat.simplex.common.veil.screens.chat

import kotlin.test.*

/*
 * The rules of the message sheet, on plain facts (a CIContent needs Android resources, so the unit tests do not build
 * ChatItems). Each case is named after the situation it stands for and mirrors a branch of upstream's item menu.
 *
 * ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.screens.chat.*'
 */
class VeilChatActionsTest {
  private val default = VeilActionFacts()

  // a sent text message in a direct chat
  private val own = VeilItemFacts()
  // a received text message: cannot be edited
  private val theirs = VeilItemFacts(editable = false)

  private fun actions(i: VeilItemFacts, f: VeilActionFacts = default) = veilMessageActions(i, f)

  private fun sorted(vararg a: VeilMsgAction) = a.sortedBy { it.ordinal }

  @Test
  fun ownTextMessage() {
    val a = actions(own)
    assertEquals(
      sorted(
        VeilMsgAction.Reply, VeilMsgAction.Edit, VeilMsgAction.Forward, VeilMsgAction.React, VeilMsgAction.Copy,
        VeilMsgAction.Share, VeilMsgAction.Info, VeilMsgAction.Select, VeilMsgAction.Delete
      ),
      a
    )
  }

  @Test
  fun otherPersonsMessageCannotBeEdited() {
    val a = actions(theirs)
    assertFalse(VeilMsgAction.Edit in a)
    assertTrue(VeilMsgAction.Reply in a)
    assertTrue(VeilMsgAction.Delete in a)
  }

  @Test
  fun orderIsReplyEditForwardReactThenTheRestDangerLast() {
    val a = actions(own)
    assertEquals(VeilMsgAction.Reply, a[0])
    assertEquals(VeilMsgAction.Edit, a[1])
    assertEquals(VeilMsgAction.Forward, a[2])
    assertEquals(VeilMsgAction.React, a[3])
    assertEquals(VeilMsgAction.Delete, a.last())
    assertTrue(VeilMsgAction.Delete.danger)
    assertTrue(VeilMsgAction.Moderate.danger)
    assertFalse(VeilMsgAction.Reply.danger)
  }

  @Test
  fun reactionsOffOrUsedUpHideReact() {
    assertFalse(VeilMsgAction.React in actions(own.copyFacts(reactionsAllowed = false)))
    assertFalse(VeilMsgAction.React in actions(own, VeilActionFacts(reactionsLeft = false)))
  }

  @Test
  fun readOnlyChatCannotReply() {
    assertFalse(VeilMsgAction.Reply in actions(theirs.copyFacts(sendEnabled = false)))
  }

  @Test
  fun noteToSelfHasNoReply() {
    assertFalse(VeilMsgAction.Reply in actions(own.copyFacts(localNote = true)))
  }

  @Test
  fun liveMessageOffersNoReplyEditForward() {
    val a = actions(own, VeilActionFacts(live = true))
    assertFalse(VeilMsgAction.Reply in a)
    assertFalse(VeilMsgAction.Edit in a)
    assertFalse(VeilMsgAction.Forward in a)
    assertTrue(VeilMsgAction.Info in a)
    // the live message being composed cannot be deleted while it is live
    assertFalse(VeilMsgAction.Delete in actions(own.copyFacts(metaLive = true), VeilActionFacts(live = true)))
    assertTrue(VeilMsgAction.Delete in actions(own.copyFacts(metaLive = true), default))
  }

  @Test
  fun voiceMessageCannotBeEdited() {
    assertFalse(VeilMsgAction.Edit in actions(own.copyFacts(isVoice = true)))
  }

  @Test
  fun fileWithoutTextNeedsAForwardableFileToCopyAndShare() {
    val fileOnly = own.copyFacts(hasText = false, hasFile = true, fileForwardingAllowed = false)
    val a = actions(fileOnly)
    assertFalse(VeilMsgAction.Copy in a)
    assertFalse(VeilMsgAction.Share in a)
    assertFalse(VeilMsgAction.Forward in a) // upstream: file must allow forwarding
    val ok = actions(fileOnly.copyFacts(fileForwardingAllowed = true))
    assertTrue(VeilMsgAction.Copy in ok)
    assertTrue(VeilMsgAction.Share in ok)
    assertTrue(VeilMsgAction.Forward in ok)
  }

  @Test
  fun fileSaveOrDownload() {
    val file = theirs.copyFacts(hasFile = true, fileForwardingAllowed = true)
    val notOnDisk = actions(file)
    assertFalse(VeilMsgAction.Save in notOnDisk)
    assertFalse(VeilMsgAction.Download in notOnDisk)
    val onDisk = actions(file, VeilActionFacts(fileLoaded = true))
    assertTrue(VeilMsgAction.Save in onDisk)
    assertFalse(VeilMsgAction.Download in onDisk)
    val invitation = actions(file, VeilActionFacts(fileDownloadable = true))
    assertTrue(VeilMsgAction.Download in invitation)
    assertFalse(VeilMsgAction.Save in invitation)
  }

  @Test
  fun transferInProgressCanBeCancelled() {
    val sending = own.copyFacts(hasFile = true, fileCancellable = true, fileForwardingAllowed = true)
    val a = actions(sending)
    assertTrue(VeilMsgAction.CancelFile in a)
    assertTrue(VeilMsgAction.CancelFile.danger)
    assertFalse(VeilMsgAction.CancelFile in actions(sending.copyFacts(localNote = true)))
  }

  @Test
  fun deletedMessageCollapsedOffersRevealInfoDeleteSelect() {
    val deleted = theirs.copyFacts(markedDeleted = true)
    assertEquals(
      sorted(VeilMsgAction.Reveal, VeilMsgAction.Info, VeilMsgAction.Delete, VeilMsgAction.Select),
      actions(deleted, VeilActionFacts(revealed = false))
    )
  }

  @Test
  fun deletedMessageRevealedOffersHideAndNoReplyEditForward() {
    val deleted = theirs.copyFacts(markedDeleted = true)
    val a = actions(deleted, VeilActionFacts(revealed = true))
    assertTrue(VeilMsgAction.Hide in a)
    assertFalse(VeilMsgAction.Reveal in a)
    assertFalse(VeilMsgAction.Reply in a)
    assertFalse(VeilMsgAction.Edit in a)
    assertFalse(VeilMsgAction.Forward in a)
    assertTrue(VeilMsgAction.Delete in a)
  }

  @Test
  fun deletedContentHasNoRevealToggle() {
    val gone = theirs.copyFacts(markedDeleted = true, deletedContent = true, hasMsgContent = false)
    assertEquals(sorted(VeilMsgAction.Info, VeilMsgAction.Delete, VeilMsgAction.Select), actions(gone))
  }

  @Test
  fun legacyDeletedItem() {
    val legacy = VeilItemFacts(content = VeilContentKind.LegacyDeleted, hasMsgContent = false)
    assertEquals(sorted(VeilMsgAction.Info, VeilMsgAction.Delete, VeilMsgAction.Select), actions(legacy))
  }

  @Test
  fun moderatedPlaceholderCanBeRevealedHiddenOrExpanded() {
    val moderated = VeilItemFacts(content = VeilContentKind.Moderated, hasMsgContent = false)
    assertTrue(VeilMsgAction.Reveal in actions(moderated))
    assertTrue(VeilMsgAction.Hide in actions(moderated, VeilActionFacts(revealed = true)))
    val gone = moderated.copyFacts(deletedContent = true)
    assertTrue(VeilMsgAction.Expand in actions(gone, VeilActionFacts(inRange = true)))
    assertFalse(VeilMsgAction.Expand in actions(gone))
    assertFalse(VeilMsgAction.Reveal in actions(gone))
  }

  @Test
  fun itemsWithoutAnUpstreamMenuGetNoSheet() {
    assertTrue(actions(VeilItemFacts(content = VeilContentKind.NoMenu, hasMsgContent = false)).isEmpty())
  }

  @Test
  fun integrityErrorsOnlyWithDeveloperTools() {
    val err = VeilItemFacts(content = VeilContentKind.IntegrityError, hasMsgContent = false)
    assertTrue(actions(err).isEmpty())
    assertEquals(sorted(VeilMsgAction.Select, VeilMsgAction.Delete), actions(err, VeilActionFacts(developerTools = true)))
  }

  @Test
  fun deleteOnlyItemsOfferDeleteAndSelect() {
    val call = VeilItemFacts(content = VeilContentKind.DeleteOnly, hasMsgContent = false)
    assertEquals(sorted(VeilMsgAction.Select, VeilMsgAction.Delete), actions(call))
    assertEquals(listOf(VeilMsgAction.Delete), actions(call.copyFacts(canBeDeletedForSelf = false)))
  }

  @Test
  fun eventItemsFallToTheLastBranches() {
    val event = VeilItemFacts(content = VeilContentKind.Event, hasMsgContent = false, canBeDeletedForSelf = false)
    assertEquals(sorted(VeilMsgAction.Delete, VeilMsgAction.Select), actions(event))
    assertEquals(listOf(VeilMsgAction.Delete), actions(event, VeilActionFacts(selectionActive = true)))
    val collapsedRun = event.copyFacts(merged = true)
    assertEquals(sorted(VeilMsgAction.Expand, VeilMsgAction.Delete), actions(collapsedRun, VeilActionFacts(rangeCount = 3)))
    assertEquals(sorted(VeilMsgAction.Shrink, VeilMsgAction.Delete), actions(collapsedRun, VeilActionFacts(revealed = true)))
  }

  @Test
  fun groupAdminCanModerateAMembersMessage() {
    val admin = VeilItemFacts(editable = false, inGroup = true, moderatorOrAbove = true, canModerate = true)
    val a = actions(admin)
    assertTrue(VeilMsgAction.Moderate in a)
    assertFalse(VeilMsgAction.Report in a)
    assertTrue(a.indexOf(VeilMsgAction.Moderate) > a.indexOf(VeilMsgAction.Delete))
  }

  @Test
  fun adminHasNoModerateOnOwnMessagesOrOnOwners() {
    // own group message: upstream skips the moderate / report block entirely
    val mine = VeilItemFacts(inGroup = true, ownGroupMessage = true, moderatorOrAbove = true, canModerate = true)
    val a = actions(mine)
    assertFalse(VeilMsgAction.Moderate in a)
    assertTrue(VeilMsgAction.Delete in a)
    // memberToModerate is null for a higher role
    val owner = VeilItemFacts(editable = false, inGroup = true, moderatorOrAbove = true, canModerate = false)
    assertFalse(VeilMsgAction.Moderate in actions(owner))
    assertFalse(VeilMsgAction.Report in actions(owner))
  }

  @Test
  fun plainMemberReportsOnlyWhenTheGroupAllowsIt() {
    val member = VeilItemFacts(editable = false, inGroup = true, plainMember = true, reportsEnabled = true)
    val a = actions(member)
    assertTrue(VeilMsgAction.Report in a)
    assertFalse(VeilMsgAction.Moderate in a)
    assertFalse(VeilMsgAction.Report in actions(member.copyFacts(reportsEnabled = false)))
    assertFalse(VeilMsgAction.Report in actions(member.copyFacts(markedDeleted = true), VeilActionFacts(revealed = true)))
    assertFalse(VeilMsgAction.Report in actions(member, VeilActionFacts(live = true)))
  }

  @Test
  fun reportItemInAGroup() {
    val report = VeilItemFacts(isReport = true, inGroup = true, hasMsgContent = true, editable = false)
    // a moderator can archive and delete the report
    assertEquals(
      sorted(VeilMsgAction.ArchiveReport, VeilMsgAction.DeleteReport, VeilMsgAction.Select),
      actions(report.copyFacts(moderatorOrAbove = true))
    )
    // a plain member can only delete it
    assertEquals(sorted(VeilMsgAction.DeleteReport, VeilMsgAction.Select), actions(report))
    // the sender's own report cannot be archived
    assertFalse(VeilMsgAction.ArchiveReport in actions(report.copyFacts(moderatorOrAbove = true, ownGroupMessage = true)))
  }

  @Test
  fun selectOnlyWhereItemCanBeDeletedForSelf() {
    assertFalse(VeilMsgAction.Select in actions(own.copyFacts(canBeDeletedForSelf = false)))
  }

  @Test
  fun previewsHaveNoDelete() {
    assertFalse(VeilMsgAction.Delete in actions(own, VeilActionFacts(preview = true)))
  }
}

private fun VeilItemFacts.copyFacts(
  content: VeilContentKind = this.content,
  markedDeleted: Boolean = this.markedDeleted,
  deletedContent: Boolean = this.deletedContent,
  hasMsgContent: Boolean = this.hasMsgContent,
  idNonNegative: Boolean = this.idNonNegative,
  isReport: Boolean = this.isReport,
  isVoice: Boolean = this.isVoice,
  isLiveDummy: Boolean = this.isLiveDummy,
  metaLive: Boolean = this.metaLive,
  localNote: Boolean = this.localNote,
  canBeDeletedForSelf: Boolean = this.canBeDeletedForSelf,
  merged: Boolean = this.merged,
  editable: Boolean = this.editable,
  hasText: Boolean = this.hasText,
  hasFile: Boolean = this.hasFile,
  fileForwardingAllowed: Boolean = this.fileForwardingAllowed,
  fileCancellable: Boolean = this.fileCancellable,
  reactionsAllowed: Boolean = this.reactionsAllowed,
  sendEnabled: Boolean = this.sendEnabled,
  inGroup: Boolean = this.inGroup,
  ownGroupMessage: Boolean = this.ownGroupMessage,
  moderatorOrAbove: Boolean = this.moderatorOrAbove,
  plainMember: Boolean = this.plainMember,
  reportsEnabled: Boolean = this.reportsEnabled,
  canModerate: Boolean = this.canModerate,
) = VeilItemFacts(
  content, markedDeleted, deletedContent, hasMsgContent, idNonNegative, isReport, isVoice, isLiveDummy, metaLive, localNote,
  canBeDeletedForSelf, merged, editable, hasText, hasFile, fileForwardingAllowed, fileCancellable, reactionsAllowed, sendEnabled,
  inGroup, ownGroupMessage, moderatorOrAbove, plainMember, reportsEnabled, canModerate
)
