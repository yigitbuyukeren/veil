# Development status

**This is the single status source for Veil.** Other documents link here instead of repeating status; evidence documents ([EMULATOR_TESTS.md](EMULATOR_TESTS.md), [TOR_ONLY.md](TOR_ONLY.md), [NETWORK_EGRESS.md](NETWORK_EGRESS.md)) hold the recorded results. Plans for work not done: [ROADMAP.md](ROADMAP.md).

Updated 2026-11-25 (evidence from 2026-11-04 to 2026-11-10). Base: SimpleX Chat v7.0.3 (`b3907c9`). Evidence rules: PASS only with a recorded test; everything else is NOT TESTED, NOT STARTED or BLOCKED. No issue is closed; all await independent review.

## Summary

| Area | State |
|---|---|
| Release decision | **NO-GO** ([M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md)). Reasons: no real-device or Orbot evidence, leak matrix mostly NOT TESTED, open license gaps, no independent review |
| Milestone M1 (closed alpha) | Gates open; see [ROADMAP.md](ROADMAP.md) |
| Redesign "Tül" & Veil screens | **Implemented in code, NOT TESTED** on a device or emulator (builds and Veil unit tests pass); not "done" until shown working |
| Native core | Prebuilt official `libsimplex.so`/`libsupport.so` from v7.0.3, verified by SHA-256 and GPG signatures in CI; not built from source by decision ([NATIVE_CORE.md](NATIVE_CORE.md)) |
| Tor-only policy | Enforced at `ChatController.apiSetNetworkConfig`, network settings locked read-only, status screen checks active config via `VeilNetworkPolicy.isCompliant`; code and unit tests only (UI/device NOT TESTED) |

## Implemented app-layer work (2026-11-04 to 2026-11-10, NOT TESTED on devices)

Detailed per-commit logs live in Git history. Summary of what exists in code on `main`:

- **Core decisions (2026-11-04):** Prebuilt official native core verified by SHA-256/GPG (`NATIVE_CORE.md`); Tor-only choke point at `apiSetNetworkConfig`; approximate Haskell/native license inventory in `THIRD_PARTY_NOTICES.md`; Veil docs consolidated under `docs/veil/`.
- **Redesign "Tül" (all screen groups):** Design system (tokens, Gece dark / Sis light / Sistem modes, Bricolage Grotesque & Inter, 48 dp targets, `VeilContrastTest`), Onboarding, Chat list, Chat, New chat/QR, Groups, Settings, System screens. Hidden-feature paths closed with `// Veil:` guards; 11 unused `veil_` strings removed; 28 reachable upstream SimpleX strings reworded (ratchet lowered to 72 base / 61 tr).
- **Calls opt-in, chat bar & availability (N7, 2026-11-05 to 2026-11-10):** Voice/video calls remain **off by default** (`VeilNetworkPolicy.CALLS_ENABLED = false`, `veilCallsOptIn = false`) and are the sole outside-Tor exception; relay-only ICE is forced, incoming/outgoing calls confirm and display "Tor dışı" indicators (`policy-check.sh` enforces default-off and relay-only). Chat bar exposes audio/video/end-call buttons only when opted in and permitted by direct-contact preferences. Toggling calls off publishes a `NO` calls preference to ready active contacts (saving per-host/profile/contact overrides locally, not in DB exports, and restoring them on re-enable; reconciled on switch, startup, profile load and contact connect). Known limit: custom ICE lists from imported settings are not cleared.
- **Additional Veil screens (N1–N6f):** Per-profile local chat pinning (`veil_pinned_chats_<userId>`, up to 5, excluded from DB export); Gelişmiş & device migration (Tor-enforced temporary controller; upstream #7696 large-profile risk unverified on 7.0.3); removed-member rejoin warning; `simplex.chat` web deep links removed from manifest (`simplex:` kept without auto-verify) and read-only relay list (`VeilServersScreen`); Veil screens for Notifications, Profiles, More Privacy, Contact Details/Preferences/Pending Connection, Group Preferences/Profile/Welcome/Advanced/Bulk/Add Members/Member Details, and Database/Database Management (reusing upstream action lambdas).
- **M3 privacy & data lifecycle (M3-1 to M3-5):** Hidden notification preview for new installs, per-chat notification content hiding (`VeilNtfPrivacy`), lock-screen PRIVATE visibility, lazy call notification channel; app lock delay/screen-protection prompt and self-destruct passcode warning (auth flows stay in upstream `SimplexLockView`); backup/restore passphrase gate and warnings ([BACKUP_TESTS.md](BACKUP_TESTS.md)); "Tüm verileri sil" wipes `veil_*` preference keys (`veilWipeDeviceLists`); local file encryption locked on once enabled; pending invitation cancel wording ("Daveti iptal et").
- **Fixes & known test status:**
  - `ChatItemView` ART `VerifyError` fixed by extracting non-inline `ChatItemContainer` (verified on Android 15 arm64 emulator with `dalvikvm -Xverify:all`; phone chat rendering NOT TESTED).
  - Bubble meta-line width fixed; `VeilPins.save` persistence bug fixed; live-message "nothing happens" report remains **NOT FIXED** (needs device).
  - Latest recorded validation (2026-11-06 / 2026-11-10): arm64 FOSS debug build, **273 Veil Android common unit tests** (264 on the chat-bar commit), Android unit test, `docs-check.sh`, `strings-check.sh`, `policy-check.sh`, `egress-check.sh` **PASS**. Unresolved: full upstream common suite has 5 failures in `ChatItemsMergerTest`, `ProviderForGalleryTest` and `ThemesTest` (`androidAppContext` uninitialized).

## Open items

- **Device and emulator tests of the redesign and M3/call flows are pending.** Real Android devices and Orbot are required for M1 gates and in-app claim evidence.
- **`android-support` license (distribution blocker):** Declares no license and has no license file at the pinned tag; must be settled with upstream before distribution ([UPSTREAM_LICENSES.md](UPSTREAM_LICENSES.md)).
- **Approximate license inventory & LGPL duties:** Haskell/native inventory is approximate; LGPL notice/replace duties (GMP, libiconv, trove4j) are not implemented.
- **In-app claims pending evidence:** 25 `veil_` strings ([PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md)) keep distribution NO-GO until backed or softened.
- **Remaining upstream branding:** Intentionally opened upstream screens, system notification/lock texts and non-English locales still carry upstream wording ([BRANDING.md](BRANDING.md), [DESIGN.md](DESIGN.md)).

| Issue | State | Commits | Notes |
|---|---|---|---|
| #1 VEIL-000 Epic | open | | M1 not reached |
| #2 VEIL-001 threat model | done, awaiting review | 3f187b9, 37418c3 | S1–S17 all NOT TESTED |
| #3 VEIL-002 licenses and relay terms | Gradle done (273 artifacts, 1 LGPL-2.1); Haskell/native approximate; **gaps open** | abbdb07, c4856e810, 6704ed0ee | Distribution NO-GO: `android-support` unlicensed, approximate inventory, LGPL duties open |
| #4 VEIL-003 pinned base | done, awaiting review | 522639f73 | v7.0.3 `b3907c9`, history preserved |
| #5 VEIL-004 Android build | arm64 FOSS debug PASS; prebuilt core by decision | e1b118568, 714d807a9 | Verified by SHA-256 + GPG in CI; no from-source build planned; no real device |
| #6 VEIL-005 CI | done, awaiting review | 4b414aaf8, 6479e06c8 | Green on `main` |
| #7 VEIL-006 package identity | emulator done; branding partial | 0f3bc4b93, c4856e810, f0add36b9 | Real device NOT TESTED; see `BRANDING.md` |
| #8 VEIL-007 Tor-only via Orbot | enforced at `apiSetNetworkConfig`, settings locked | 1389eba9f, 5532a63b9, 227de4d4c, 05b3fae8b | New locks/status check NOT TESTED in running app; Orbot and real devices NOT TESTED |
| #9 VEIL-008 egress/fail-closed | partial | 6479e06c8 | Gates in code; single emulator run; most leak matrix rows NOT TESTED |
| #10 VEIL-009 anonymous start | partial | c1759dd5f | Incognito default on (E2, E11); invite cancel/block NOT TESTED |
| #11 VEIL-010 E2EE 1:1 on two real devices | **BLOCKED** (physical criteria); 2-emulator PASS | a2d841cf5 | `EMULATOR_TESTS.md` E1–E4, E12; wrong-contact and key-change NOT TESTED |
| #12 VEIL-011 offline store-and-forward | emulator evidence only | a2d841cf5 | E5, E6 PASS; delivery-state semantics and real devices NOT TESTED |
| #13 VEIL-012 encrypted persistent history | emulator evidence only | a2d841cf5 | E7, E8 PASS; key storage not audited |
| #14 VEIL-013 background reliability | NOT STARTED | | Emulator cannot represent OEM battery policy |
| #15 VEIL-014 small groups | 2-member emulator PASS | cfead9cfc | E10, E11, E19; 3-member and 10-participant NOT TESTED |
| #16 VEIL-015 group permissions | partial | cfead9cfc | E11b, E22–E24 observed; link abuse beyond that NOT TESTED |
| #17 VEIL-016 minimal UI and indicators | partial (superseded by Tül redesign) | c4856e810, bc40525b3, f0add36b9, 72a0ad9b4 | E16–E21; non-English strings open; see [ROADMAP.md](ROADMAP.md) |
| #18 VEIL-017 M1 validation, GO/NO-GO | **NO-GO** | | See [M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md) |

## Next step

Ordered remaining work and acceptance gates live in [ROADMAP.md](ROADMAP.md). Immediate blockers:

1. Two real Android devices + Orbot for VEIL-010 through VEIL-016 and full leak matrix (anonymized device report).
2. Settle open license items (`android-support` first).
3. Third emulator or device for 3-member and 10-participant groups (`scripts/emulator/scenario_group.py` covers 2 members).
4. Delivery-state semantics, wrong-contact/key-change warning (E26), and clean-pair rerun of `scenario_background.py` (E27).

## Environment notes

- JDK 17, Android SDK platform 35, NDK 23.1.7779620; build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (Turkish locale breaks resource generation, see [ANDROID_BUILD.md](ANDROID_BUILD.md)).
- Test emulator AVD `veil-test` (arm64, Android 15); host Tor via `adb reverse`.
