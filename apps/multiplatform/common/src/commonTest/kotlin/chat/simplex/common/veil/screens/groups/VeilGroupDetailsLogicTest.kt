package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.*
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.*'
class VeilGroupDetailsLogicTest {
  private val base = FullGroupPreferences.sampleData
  private val on = GroupFeatureEnabled.ON
  private val off = GroupFeatureEnabled.OFF

  // ---- preferences ----

  @Test
  fun groupListsTenFeaturesInUpstreamOrder() {
    val rows = prefRows(useRelays = false)
    assertEquals(
      listOf(
        GroupFeature.TimedMessages, GroupFeature.DirectMessages, GroupFeature.FullDelete, GroupFeature.Reactions, GroupFeature.Voice,
        GroupFeature.Files, GroupFeature.SimplexLinks, GroupFeature.Reports, GroupFeature.History, GroupFeature.Support
      ),
      rows.map { it.feature }
    )
    assertEquals(setOf(GroupFeature.Reports, GroupFeature.Support), rows.filter { it.disabled }.map { it.feature }.toSet())
    assertTrue(rows.none { it.relayNote })
  }

  @Test
  fun channelListsFiveAndNeverSignMessages() {
    val rows = prefRows(useRelays = true)
    assertEquals(
      listOf(GroupFeature.TimedMessages, GroupFeature.FullDelete, GroupFeature.Reactions, GroupFeature.History, GroupFeature.Support),
      rows.map { it.feature }
    )
    assertTrue(rows.none { it.disabled })
    assertEquals(listOf(GroupFeature.Support), rows.filter { it.relayNote }.map { it.feature })
    assertTrue(prefRows(false).none { it.feature == GroupFeature.SignMessages })
  }

  @Test
  fun memberAdmissionOnlyForPlainGroups() {
    assertTrue(showMemberAdmission(useRelays = false, isBusinessChat = false))
    assertFalse(showMemberAdmission(useRelays = true, isBusinessChat = false))
    assertFalse(showMemberAdmission(useRelays = false, isBusinessChat = true))
  }

  @Test
  fun titleFollowsKind() {
    assertEquals(VeilPrefsTitle.Channel, prefsTitle(true, false))
    assertEquals(VeilPrefsTitle.Group, prefsTitle(false, false))
    assertEquals(VeilPrefsTitle.Chat, prefsTitle(false, true))
  }

  @Test
  fun switchingOnKeepsRoleAndTime() {
    val p = base.copy(voice = RoleGroupPreference(off, null))
    val voiceOn = toggled(p, base, GroupFeature.Voice, true)
    assertEquals(RoleGroupPreference(on, null), voiceOn.voice)
    val withRole = base.copy(files = RoleGroupPreference(on, GroupMemberRole.Admin))
    // flipping an enabled switch on again keeps the chosen role
    assertEquals(GroupMemberRole.Admin, roleOf(toggled(withRole, base, GroupFeature.Files, true), GroupFeature.Files))
  }

  @Test
  fun switchingOffClearsRole() {
    val p = base.copy(files = RoleGroupPreference(on, GroupMemberRole.Admin))
    val q = toggled(p, base, GroupFeature.Files, false)
    assertEquals(RoleGroupPreference(off, null), q.files)
  }

  @Test
  fun timedMessagesOnFallsBackToOneDay() {
    val p = base.copy(timedMessages = TimedMessagesGroupPreference(off, null))
    assertEquals(TimedMessagesGroupPreference(on, 86400), toggled(p, base, GroupFeature.TimedMessages, true).timedMessages)
    val chosen = base.copy(timedMessages = TimedMessagesGroupPreference(off, 3600))
    assertEquals(3600, toggled(chosen, base, GroupFeature.TimedMessages, true).timedMessages.ttl)
  }

  @Test
  fun timedMessagesOffRestoresSavedTime() {
    val saved = base.copy(timedMessages = TimedMessagesGroupPreference(on, 600))
    val edited = saved.copy(timedMessages = TimedMessagesGroupPreference(on, 7 * 86400))
    val q = toggled(edited, saved, GroupFeature.TimedMessages, false)
    assertEquals(TimedMessagesGroupPreference(off, 600), q.timedMessages)
  }

  @Test
  fun plainFeaturesWriteOnlyTheirOwnPreference() {
    for (f in GroupFeature.values()) {
      val q = toggled(base, base, f, enabledOf(base, f) != on)
      assertNotEquals(base, q, "$f")
      assertEquals(enabledOf(base, f) != on, enabledOf(q, f) == on, "$f")
      // every other feature is untouched
      for (g in GroupFeature.values()) if (g != f) assertEquals(enabledOf(base, g), enabledOf(q, g), "$f changed $g")
    }
  }

  @Test
  fun roleRowOnlyForEnabledFeaturesWithRoles() {
    val p = base.copy(directMessages = RoleGroupPreference(on, null), voice = RoleGroupPreference(off, null))
    assertTrue(showRoleRow(p, GroupFeature.DirectMessages))
    assertFalse(showRoleRow(p, GroupFeature.Voice))
    assertFalse(showRoleRow(p, GroupFeature.Reactions))
    assertFalse(showRoleRow(p, GroupFeature.TimedMessages))
  }

  @Test
  fun settingRoleWorksOnlyWhereTheRowShows() {
    val p = base.copy(files = RoleGroupPreference(on, null), voice = RoleGroupPreference(off, null))
    assertEquals(GroupMemberRole.Owner, roleOf(withRole(p, GroupFeature.Files, GroupMemberRole.Owner), GroupFeature.Files))
    assertEquals(null, roleOf(withRole(p, GroupFeature.Files, null), GroupFeature.Files))
    assertEquals(p, withRole(p, GroupFeature.Voice, GroupMemberRole.Admin))
    assertEquals(p, withRole(p, GroupFeature.Reactions, GroupMemberRole.Admin))
  }

  @Test
  fun featureRolesAreUpstreamsFour() {
    assertEquals(listOf(null, GroupMemberRole.Moderator, GroupMemberRole.Admin, GroupMemberRole.Owner), featureRoles)
  }

  @Test
  fun ttlRowOnlyWhileTimedMessagesAreOn() {
    assertTrue(showTtlRow(base.copy(timedMessages = TimedMessagesGroupPreference(on, 600))))
    assertFalse(showTtlRow(base.copy(timedMessages = TimedMessagesGroupPreference(off, 600))))
    assertEquals(7 * 86400, withTtl(base, 7 * 86400).timedMessages.ttl)
  }

  @Test
  fun ttlOptionsAddAnOddCurrentValueOnly() {
    assertEquals(TimedMessagesPreference.ttlValues, ttlOptions(3600))
    assertEquals(TimedMessagesPreference.ttlValues, ttlOptions(null))
    val odd = ttlOptions(5400)
    assertEquals(TimedMessagesPreference.ttlValues + 5400, odd)
  }

  @Test
  fun unsavedPrefsAskBeforeLeaving() {
    assertEquals(VeilPrefsClose.Close, prefsCloseAction(base, base))
    val edited = toggled(base, base, GroupFeature.Reactions, enabledOf(base, GroupFeature.Reactions) != on)
    assertEquals(VeilPrefsClose.AskToSave, prefsCloseAction(edited, base))
    assertTrue(hasUnsavedPrefs(edited, base))
    assertFalse(hasUnsavedPrefs(base, base))
  }

  // ---- profile ----

  private val profile = GroupProfile(displayName = "team", fullName = "My Team", shortDescr = null, image = "img")
  private val valid: (String) -> Boolean = { it.isNotEmpty() && !it.contains('@') }
  private val fits: (String) -> Boolean = { it.length <= 10 }

  @Test
  fun untouchedDraftIsUnchanged() {
    val d = draftOf(profile)
    assertTrue(isDraftUnchanged(d, profile))
    assertFalse(canSaveProfile(d, profile, valid, fits))
    assertEquals(VeilProfileClose.Close, profileCloseAction(d, profile, valid, fits))
  }

  @Test
  fun spacesAroundTextAreNotAChange() {
    val d = draftOf(profile).copy(displayName = " team ", fullName = "My Team ", shortDescr = "  ")
    assertTrue(isDraftUnchanged(d, profile))
  }

  @Test
  fun changedValidDraftCanSaveAndAsksOnLeave() {
    val d = draftOf(profile).copy(displayName = "crew")
    assertTrue(canSaveProfile(d, profile, valid, fits))
    assertEquals(VeilProfileClose.AskToSave, profileCloseAction(d, profile, valid, fits))
  }

  @Test
  fun changingOnlyThePhotoIsAChange() {
    val d = draftOf(profile).copy(image = null)
    assertFalse(isDraftUnchanged(d, profile))
    assertTrue(canSaveProfile(d, profile, valid, fits))
  }

  @Test
  fun invalidOrEmptyNameCannotSaveAndLeavesWithoutAsking() {
    val bad = draftOf(profile).copy(displayName = "a@b")
    assertFalse(canSaveProfile(bad, profile, valid, fits))
    assertEquals(VeilProfileClose.Close, profileCloseAction(bad, profile, valid, fits))
    val empty = draftOf(profile).copy(displayName = "  ")
    assertFalse(canSaveProfile(empty, profile, valid, fits))
  }

  @Test
  fun untouchedNameIsAlwaysValid() {
    val odd = profile.copy(displayName = "a@b")
    assertTrue(isValidNewName("a@b", odd, valid))
    assertFalse(isValidNewName("a@c", odd, valid))
  }

  @Test
  fun tooLongDescriptionCannotSave() {
    val d = draftOf(profile).copy(shortDescr = "x".repeat(11))
    assertFalse(canSaveProfile(d, profile, valid, fits))
    assertEquals(VeilProfileClose.Close, profileCloseAction(d, profile, valid, fits))
  }

  @Test
  fun appliedDraftIsTrimmedAndKeepsTheRest() {
    val withDescr = profile.copy(description = "hello", localAlias = "al")
    val d = VeilGroupProfileDraft(" crew ", " Full ", "  ", null)
    val p = applyDraft(withDescr, d)
    assertEquals("crew", p.displayName)
    assertEquals("Full", p.fullName)
    assertEquals(null, p.shortDescr)
    assertEquals(null, p.image)
    assertEquals("hello", p.description)
    assertEquals("al", p.localAlias)
    assertEquals("short", applyDraft(profile, d.copy(shortDescr = " short ")).shortDescr)
  }

  @Test
  fun fullNameFieldOnlyWhenItDiffersFromTheName() {
    assertTrue(showFullNameField(profile))
    assertFalse(showFullNameField(profile.copy(fullName = "team")))
    assertFalse(showFullNameField(profile.copy(fullName = "  ")))
  }

  // ---- welcome message ----

  @Test
  fun welcomeUnchangedTreatsEmptyAsNone() {
    assertTrue(welcomeUnchanged("", null))
    assertTrue(welcomeUnchanged("hi", "hi"))
    assertFalse(welcomeUnchanged("hi", null))
    assertFalse(welcomeUnchanged("", "hi"))
  }

  @Test
  fun welcomeLimitIs1200Bytes() {
    assertTrue(welcomeFits(1200))
    assertFalse(welcomeFits(1201))
  }

  @Test
  fun welcomeIsTrimmedAndEmptyBecomesNull() {
    assertEquals("hi", welcomeToSave("\n hi \n"))
    assertEquals(null, welcomeToSave(" \n "))
    assertEquals("a b", welcomeToSave("a b"))
  }

  @Test
  fun onlyOwnersOfPlainGroupsEditWelcome() {
    assertTrue(canEditWelcome(isOwner = true, isBusinessChat = false))
    assertFalse(canEditWelcome(isOwner = false, isBusinessChat = false))
    assertFalse(canEditWelcome(isOwner = true, isBusinessChat = true))
  }

  @Test
  fun welcomeSaveAndLeaveRules() {
    assertTrue(canSaveWelcome(unchanged = false, fits = true))
    assertFalse(canSaveWelcome(unchanged = true, fits = true))
    assertFalse(canSaveWelcome(unchanged = false, fits = false))
    assertEquals(VeilWelcomeClose.Close, welcomeCloseAction(unchanged = true, fits = true))
    assertEquals(VeilWelcomeClose.Close, welcomeCloseAction(unchanged = true, fits = false))
    assertEquals(VeilWelcomeClose.TooLong, welcomeCloseAction(unchanged = false, fits = false))
    assertEquals(VeilWelcomeClose.AskToSave, welcomeCloseAction(unchanged = false, fits = true))
    assertTrue(canTogglePreview("x"))
    assertFalse(canTogglePreview(""))
  }
}
