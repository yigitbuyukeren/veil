# Development status

**This is the single status source for Veil.** Other documents link here instead of repeating status; evidence documents ([EMULATOR_TESTS.md](EMULATOR_TESTS.md), [TOR_ONLY.md](TOR_ONLY.md), [NETWORK_EGRESS.md](NETWORK_EGRESS.md)) hold the recorded results. Plans for work not done: [ROADMAP.md](ROADMAP.md).

Updated 2026-11-04. Base: SimpleX Chat v7.0.3. Evidence rules: PASS only with a recorded test; everything else is NOT TESTED, NOT STARTED or BLOCKED. No issue is closed; all await independent review.

## Summary

| Area | State |
|---|---|
| Release decision | **NO-GO** ([M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md)). Reasons: no real-device or Orbot evidence, leak matrix mostly NOT TESTED, open license gaps, no independent review |
| Milestone M1 (closed alpha) | gates open, see [ROADMAP.md](ROADMAP.md) |
| Redesign "Tül" | **implemented for all screen groups, NOT TESTED** on a device or emulator (builds, unit tests pass); not "done" until shown working. See the redesign section below |
| Native core | prebuilt official `libsimplex.so`/`libsupport.so` from v7.0.3, verified by SHA-256 and GPG signatures in CI; not built from source by decision ([NATIVE_CORE.md](NATIVE_CORE.md)) |
| Tor-only policy | enforced at the single choke point `apiSetNetworkConfig`, network settings locked read-only, status indicator reads the core's active config: code and unit tests only; device and emulator verification of the new locks and status check NOT TESTED |

## Decisions and changes of 2026-11-04

- **Native core decision:** no build from source; prebuilt official libraries, verified by hash and signature (714d807a9, decision in [NATIVE_CORE.md](NATIVE_CORE.md)). Changes that would need core changes are not done; they are solved in the app layer.
- **Tor-only enforcement** moved to `ChatController.apiSetNetworkConfig`, the only place that builds the network-config command; the network settings screens are read-only; the Tor and privacy status screen checks the core's reported config with `VeilNetworkPolicy.isCompliant` (227de4d4c, 05b3fae8b). Unit tests exist; UI behavior NOT TESTED on a device or emulator.
- **License inventory:** approximate Haskell and native inventory added to `THIRD_PARTY_NOTICES.md` (6704ed0ee). It is derived from build files, not from a build or the binaries.
- **Design system foundation** merged (7e2571835): tokens, Bricolage Grotesque, forced Gece theme, components, debug gallery. Visuals NOT TESTED on a device or emulator.
- **Documentation:** Veil documents moved to `docs/veil/`, one roadmap, vision and design documents added, status consolidated here.

## Redesign "Tül": implemented, untested

All screen groups of [DESIGN.md](DESIGN.md) exist for Android and the arm64 FOSS debug build compiles with the unit tests for logic, design system and contrast. **Nothing has been run on a device or on an emulator**; no screen is recorded as working. Commits (all on `main`):

| Group | Commits | Contents |
|---|---|---|
| Design system | 9ae9853c5, c421cc19f, 7e2571835, 57902d719, 154ea9b79, 9dda9dbd9, e116ef760, 8e41224cf | tokens, Gece palette, components, Bricolage Grotesque and Inter with licenses, avatar generator, debug gallery, `VeilUi` flags |
| A. Onboarding | 7990c875a, d88c2a516, 6ae868368, 9f1585760 | story, Orbot check, name, notifications, conditions; Orbot launcher and conditions adapters |
| B. Chat list | 3b1aa01f6, e1cccaa9e, 6b0aa5ca4, 7d074fe4a | veil reveal, Tor ring, filters, search, avatar actions |
| C. Chat | 03430edef, c3733fb40, b23583178, cd2bde5a3, 43a875d98, 88ae52f54, 76b2f65de | top bar, bubbles, composer frame, message actions, contact info, security code, "Gelişmiş" |
| D. New chat | 620a694f0, 2461654fc | invite link and QR, paste, scan, group entry |
| F. Settings | 037ede712, 8edf1d344, f928fa381, a0a9b32e9 | home, privacy, appearance, Tor and status (read-only network), about and licenses |
| G. System | 5a1175b36, c592a8b19, ada8fcd63 | app-wide alerts, lock and passcode, database error, splash, stopped chat |
| E. Groups | 5df5ec5c3, 3c409360b, b31109ca8 | create, info, members and roles, group link |
| Theme and accessibility | b732f843d, ac5dccce2, 1218c254c | Sis light theme and Sistem mode, labels and 48 dp targets, contrast tests (`VeilContrastTest`) |
| Final audit (this pass) | see `git log` for "Close hidden-feature paths", "Remove unused Veil strings" and the documentation commits | hidden-feature paths closed, unused strings removed, claims audit, docs |

Final audit result of 2026-11-04: navigation was traced from every Veil screen (table in [DESIGN.md](DESIGN.md), "Reachable upstream screens"). Closed with `// Veil:` guards: call buttons and the calls preference in the upstream contact and member info, the WebRTC servers section, the terminal and terminal-app link outside debug builds, and a stale device-migration state in the Android routing. All `veil_` strings were audited against [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md): none prohibited, two softened, 25 allowed statements still lack device evidence and are listed with their required evidence (distribution NO-GO, [M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md)). Eleven unused `veil_` keys were removed from base and tr.

## Phone-test fixes (2026-11-05)

- **Bubble width:** the meta line of the Veil text bubble no longer fills the width, so short messages wrap their content; build and unit tests pass, appearance NOT TESTED on a device.
- **Live message:** the Veil path was read end to end (button, `startLiveMessage`, `sendLiveMessage`, `apiSendMessages(live = true)`); no Veil hook blocks it. Live items are drawn by upstream's item view, not the Veil bubble (`veilBubbleEligible` excludes `isLive`). Cause of the reported "nothing happens" not found without a device; NOT FIXED.

## Chat pinning (N1, 2026-11-05)

Chats can be pinned from the avatar sheet (up to 5, shown first in pin order, pin marker in the row). The core has no pinning, so pins are kept on the device per profile (`veil_pinned_chats_<userId>`, `VeilPins`); known limit: they are not part of a database export and do not move with a device migration. Unit tests for the ordering and the limit pass; behavior on a device NOT TESTED.

## Gelişmiş and device migration (N2, 2026-11-05)

Settings > Gelişmiş is a Veil page (`VeilAdvancedScreen`): move to another device, version info, developer tools (terminal still debug builds only). Upstream's `MigrateFromDeviceView` / `MigrateToDeviceView` do the work; the receiving side is a button on the onboarding name step. Behavior on phones NOT TESTED (needs two phones).

- **Tor evidence:** both migration flows run a temporary chat controller and set its network config with `ChatController.apiSetNetworkConfig(netCfg, ctrl = ctrl)` (`startChatWithTemporaryDatabase`, `SimpleXAPI.kt`), which wraps the config in `VeilNetworkPolicy.enforce` for every controller. The sender also checks `VeilNetworkPolicy.torProxyReady()` before starting and shows an explanation instead when Orbot does not answer. The sentence "the archive is encrypted and uploaded over Tor" is a claim pending device evidence ([PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md)).
- **Upstream issue simplex-chat/simplex-chat#7696** ("Android migration fails, exported archive corrupted"): the report is about one very large profile (about 5.6 GB, 3000 files) on 7.0.2; small profiles migrate. No fix exists upstream and it is not in the core we ship; very large profiles may fail to migrate, small ones are expected to work. NOT verified on 7.0.3.

## Removed-member note (N3, 2026-11-05)

Removing a member from the member screen while the group link is open first shows a Veil alert with the rejoin note, then upstream's unchanged removal dialog. Build and unit tests pass; behavior NOT TESTED on a device.

## Deep link and relay list (N5, 2026-11-05)

The manifest no longer claims `simplex.chat` and relay-host web links (they open in the browser); the `simplex:` scheme stays without auto-verify, so Android lets the user choose between apps when SimpleX is installed too. Pasted `https://simplex.chat/...` invitations are parsed in the app and do not depend on the manifest; NOT TESTED. Tor screen: "Sunucular ve ağ" opens a read-only Veil list of the relay servers in use with `.onion` marked (`VeilServersScreen`, host parsing unit-tested); NOT TESTED on a device.

## Calls as an opt-in (N7, 2026-11-05)

Voice and video calls are back as an explicit opt-in, **off by default**, and they are the one thing that does not go through Tor. `VeilNetworkPolicy.CALLS_ENABLED` stays `false`; the preference `veilCallsOptIn` (default `false`) is read through `VeilNetworkPolicy.callsAllowed()` at every call surface (call buttons, calls preference, incoming calls, starting a call, Tor status row). Relay-only ICE is forced for both directions (`callRelayPolicy`). The switch is in Settings > Privacy with a one-time explanation; each outgoing call and each accept is confirmed, an incoming call is labelled "Tor dışı arama", and the active call shows a fixed "Tor dışı" strip. Texts and claims were rewritten ("messages and files go only through Tor; calls (if you turn them on) are the exception", [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md)); the call server sees the IP and timing ([THREAT_MODEL.md](THREAT_MODEL.md), [TOR_ONLY.md](TOR_ONLY.md)). `policy-check.sh` also checks that the opt-in defaults to `false` and the relay policy stays forced; unit tests cover `callsAllowed` and the relay policy. Dialogs, label, strip and the relay type in the call info are NOT TESTED on a device. Known limit: a custom ICE server list already stored in preferences (the screen is hidden, so only a settings import could set it) is not cleared.

## Wording pass on upstream screens (N4b, 2026-11-05)

28 strings in `MR/base` and `MR/tr` that name SimpleX and that the reachable upstream screens use were reworded ("invite links", "public name", "Veil"); the ratchet is lowered to 72 (base) and 61 (tr). Relay, operator, About and badge strings were kept. Build and string checks only; the new wording is NOT seen on a device.

## Notifications screen (N6b, 2026-11-05)

Settings > Notifications is a Veil page (`VeilNotificationsScreen`). It does what upstream's `NotificationsSettingsView` does and nothing else: choose the mode (instant, periodic, only while the app is open; Android only) through `changeNotificationsMode`, and show the Xiaomi autostart hint for the two background modes. The wording is the onboarding step's, so "SimpleX runs in background" no longer appears here. Decision helpers (`VeilNotificationsLogic.kt`) are unit-tested; appearance and behavior NOT TESTED on a device.

## Open items

- **Device and emulator tests of the whole redesign are pending.** The emulator run waits for the owner's go; real devices and Orbot are needed for the M1 gates and for the evidence in the claims list.

- **`android-support` license (must be settled before any distribution).** The package that provides the glue code of `libsupport.so` declares no license and has no license file at the pinned tag. It has to be confirmed with upstream before Veil is distributed. Nobody has been contacted; this only records the item. See [UPSTREAM_LICENSES.md](UPSTREAM_LICENSES.md), Conflicts.
- Haskell and native license inventory is approximate; LGPL notice and replace duties (GMP, libiconv, trove4j) are not implemented for the native libraries.
- In-app statements pending evidence: 25 `veil_` strings ([PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md)); distribution is NO-GO until each is backed or softened.
- Upstream screens opened on purpose still have the upstream look and SimpleX wording; system notification texts, lock texts, non-English locales and deep-link filters still carry upstream branding ([BRANDING.md](BRANDING.md), [DESIGN.md](DESIGN.md)).

| Issue | State | Commits | Notes |
|---|---|---|---|
| #1 VEIL-000 Epic | open | | M1 not reached |
| #2 VEIL-001 threat model | done, awaiting review | 3f187b9, 37418c3 | S1-S17 all NOT TESTED |
| #3 VEIL-002 licenses and relay terms | docs done; Gradle inventory done (273 artifacts, 1 LGPL-2.1); Haskell/native inventory done by approximation; **license gaps open** | abbdb07, c4856e810, 6704ed0ee | distribution NO-GO: `android-support` has no license, inventory approximate, LGPL duties open |
| #4 VEIL-003 pinned base | done, awaiting review | 522639f73 | v7.0.3 b3907c9, history preserved |
| #5 VEIL-004 Android build | arm64 FOSS debug PASS; core prebuilt by decision | e1b118568, 714d807a9 | official libs verified by SHA-256 and GPG signatures in CI; no from-source build planned; no real device |
| #6 VEIL-005 CI | done, awaiting review | 4b414aaf8, 6479e06c8 | green on main, red demonstrated on a throwaway PR |
| #7 VEIL-006 package identity | done on emulator; branding partial | 0f3bc4b93, c4856e810, f0add36b9 | real device NOT TESTED; see `BRANDING.md` for remaining items |
| #8 VEIL-007 Tor-only via Orbot | implemented (enforced at `apiSetNetworkConfig`, settings locked); emulator + host Tor only for the earlier version | 1389eba9f, 5532a63b9, 227de4d4c, 05b3fae8b | locks and active-config status check: code + unit tests, NOT TESTED in the running app; Orbot and real device NOT TESTED; relay reachability over Tor flaky |
| #9 VEIL-008 egress/fail-closed | partial | 6479e06c8 | gates in code; leak tests single-run on emulator; many rows NOT TESTED |
| #10 VEIL-009 anonymous start | partial | c1759dd5f | incognito default on and observed on the joining side (E2, E11); invite cancel/block NOT TESTED |
| #11 VEIL-010 E2EE 1:1 on two real devices | **BLOCKED for the physical criteria**; two-emulator evidence recorded | a2d841cf5 | `EMULATOR_TESTS.md` E1-E4; security code match + mark/clear verified PASS at group-member level (E12); wrong-contact and key change NOT TESTED |
| #12 VEIL-011 offline store-and-forward | emulator evidence only | a2d841cf5 | E5, E6 PASS; delivery-state semantics NOT TESTED; real devices NOT TESTED |
| #13 VEIL-012 encrypted persistent history | emulator evidence only | a2d841cf5 | E7, E8 PASS; key storage not audited; no Veil code change |
| #14 VEIL-013 background reliability | NOT STARTED | | emulator cannot represent vendor battery policy |
| #15 VEIL-014 small groups | 2-member group via group link: emulator PASS | cfead9cfc | E10, E11, E19 (scripted); 3-member and 10-participant NOT TESTED (needs a third emulator or devices) |
| #16 VEIL-015 group permissions | partial | cfead9cfc, this session | E11b, E22-E24: link deletion, observer role and member removal observed on emulators; removal is not a ban while the link is active; anyone-can-join link abuse beyond that NOT TESTED |
| #17 VEIL-016 minimal UI and indicators | partial | c4856e810, bc40525b3, f0add36b9, 72a0ad9b4 | artwork, promotions hidden, Tor-unavailable alert, Tor and privacy status screen with licenses section (E16-E18), chat-list SOCKS indicator (E20), core-created "SimpleX Team" card hidden in the interface (E21), app name in all locales; non-English strings still open. Superseded in scope by the redesign (see [ROADMAP.md](ROADMAP.md)) |
| #18 VEIL-017 M1 validation, GO/NO-GO | report: **NO-GO** | | see `M1_RELEASE_REPORT.md` |

## Next step

The ordered list of remaining work, with acceptance gates, is [ROADMAP.md](ROADMAP.md) (M1 gates and the redesign). The blocking items right now:

1. Two real Android devices and Orbot, to run VEIL-010 (pair over Tor, messages both ways, verification code, wrong-contact case) and then VEIL-011 to 016 and the leak tests again on the devices. Record models, Android versions and patch levels in an anonymized report.
2. Settle the open license items above (`android-support` first).
3. A third emulator or device for 3-member and 10-participant groups (`scripts/emulator/scenario_group.py` covers the 2-member case).
4. Not yet tried or inconclusive: delivery-state semantics (icons not readable), wrong-contact and key-change warning (E26), background/doze (E27 inconclusive; rerun `scenario_background.py` on a clean pair).

## Environment notes

- JDK 17, Android SDK platform 35, NDK 23.1.7779620; build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (Turkish locale breaks resource generation, see [ANDROID_BUILD.md](ANDROID_BUILD.md)).
- Test emulator AVD `veil-test` (arm64, Android 15) created locally; host Tor from Homebrew used as the SOCKS proxy through `adb reverse`.
