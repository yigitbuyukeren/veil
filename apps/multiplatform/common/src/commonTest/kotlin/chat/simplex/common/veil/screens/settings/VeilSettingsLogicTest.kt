package chat.simplex.common.veil.screens.settings

import chat.simplex.common.model.*
import kotlin.test.*

class VeilSettingsLogicTest {
  @Test
  fun profilesSummaryShowsHiddenOnlyWhenThereAreSome() {
    assertEquals(listOf(SummaryPart(SummaryKey.Profiles, "2")), profilesSummary(2, 0))
    assertEquals(
      listOf(SummaryPart(SummaryKey.Profiles, "3"), SummaryPart(SummaryKey.ProfilesHidden, "1")),
      profilesSummary(3, 1)
    )
  }

  @Test
  fun privacySummaryListsOnlyWhatIsOn() {
    assertEquals(
      listOf(SummaryPart(SummaryKey.PreviewsOff)),
      privacySummary(false, false, false, NotificationPreviewMode.MESSAGE)
    )
    assertEquals(
      listOf(
        SummaryPart(SummaryKey.PreviewsOn),
        SummaryPart(SummaryKey.NotificationPreviewHidden),
        SummaryPart(SummaryKey.ScreenProtected),
        SummaryPart(SummaryKey.AppLock),
      ),
      privacySummary(true, true, true, NotificationPreviewMode.HIDDEN)
    )
  }

  @Test
  fun notificationsSummaryCoversEveryMode() {
    val keys = NotificationsMode.values().map { notificationsSummary(it).single().key }
    assertEquals(
      listOf(SummaryKey.NotificationsOff, SummaryKey.NotificationsPeriodic, SummaryKey.NotificationsService),
      keys
    )
  }

  @Test
  fun databaseSummaryHasAnUnknownState() {
    assertEquals(SummaryKey.DbEncrypted, databaseSummary(true).single().key)
    assertEquals(SummaryKey.DbPlain, databaseSummary(false).single().key)
    assertEquals(SummaryKey.DbUnknown, databaseSummary(null).single().key)
  }

  @Test
  fun aboutAndAppearanceSummaries() {
    assertEquals(SummaryPart(SummaryKey.Version, "0.1"), aboutSummary("0.1").single())
    assertEquals(SummaryKey.ThemeNight, appearanceSummary().single().key)
  }

  @Test
  fun joinSummaryUsesTheSeparator() {
    val parts = privacySummary(true, true, false, NotificationPreviewMode.MESSAGE)
    assertEquals("a · b", parts.joinSummary({ if (it.key == SummaryKey.PreviewsOn) "a" else "b" }))
    assertEquals("a, b", parts.joinSummary({ if (it.key == SummaryKey.PreviewsOn) "a" else "b" }, ", "))
  }

  @Test
  fun settingsRowsAreInPrototypeOrder() {
    assertEquals(
      listOf("Profiles", "Privacy", "Notifications", "Appearance", "Database", "Tor", "About"),
      VeilSettingsRow.values().map { it.name }
    )
  }

  @Test
  fun networkRowsAreFourAndAllLocked() {
    val rows = veilNetworkRows()
    assertEquals(listOf(NetRowKind.Path, NetRowKind.Socks, NetRowKind.Direct, NetRowKind.NoTor), rows.map { it.kind })
    assertTrue(rows.all { it.readOnly })
    assertEquals("127.0.0.1:9050", rows.first { it.kind == NetRowKind.Socks }.value)
    assertTrue(rows.filter { it.kind != NetRowKind.Socks }.all { it.value == null })
  }

  @Test
  fun policyLineIsCheckingUntilTheCoreAnswers() {
    assertEquals(PolicyLineState.Checking, policyLineState(false, null))
    assertEquals(PolicyLineState.Checking, policyLineState(false, VeilNetworkPolicy.enforce(NetCfg.defaults)))
  }

  @Test
  fun policyLineIsUnknownWhenTheCallFailed() {
    assertEquals(PolicyLineState.Unknown, policyLineState(true, null))
  }

  @Test
  fun policyLineIsOkOnlyForACompliantConfig() {
    assertEquals(PolicyLineState.Ok, policyLineState(true, VeilNetworkPolicy.enforce(NetCfg.defaults)))
    assertEquals(PolicyLineState.NotCompliant, policyLineState(true, NetCfg.defaults.copy(socksProxy = null, hostMode = HostMode.Public)))
  }

  @Test
  fun privacyDefaultsFollowThePolicyFlags() {
    val rows = privacyDefaultRows(incognito = true)
    assertEquals(listOf(PrivacyDefaultKind.Incognito, PrivacyDefaultKind.LinkPreviewsOff, PrivacyDefaultKind.CallsOff), rows.map { it.kind })
    assertTrue(rows.all { it.ok })
    assertFalse(privacyDefaultRows(incognito = false).first().ok)
    assertFalse(privacyDefaultRows(true, linkPreviewsEnabled = true).first { it.kind == PrivacyDefaultKind.LinkPreviewsOff }.ok)
    assertFalse(privacyDefaultRows(true, callsEnabled = true).first { it.kind == PrivacyDefaultKind.CallsOff }.ok)
  }

  @Test
  fun migrationStartsOnlyWhileTheProxyAnswers() {
    assertTrue(veilMigrationMayStart(true))
    assertFalse(veilMigrationMayStart(false))
  }

  @Test
  fun serverRowsMarkOnionHosts() {
    assertEquals(
      listOf(VeilServerRow("abc.onion", true), VeilServerRow("smp.example.org", false)),
      veilServerRows("smp://key@abc.onion,smp.example.org:5223")
    )
    assertEquals(listOf(VeilServerRow("x.onion", true)), veilServerRows("xftp://key@x.onion/"))
    assertEquals(emptyList(), veilServerRows("not an address"))
  }
}
