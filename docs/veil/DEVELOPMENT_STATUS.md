# Development status

**This is the single status source for Veil.** Other documents link here instead of repeating status; evidence documents ([EMULATOR_TESTS.md](EMULATOR_TESTS.md), [TOR_ONLY.md](TOR_ONLY.md), [NETWORK_EGRESS.md](NETWORK_EGRESS.md)) hold the recorded results. Plans for work not done: [ROADMAP.md](ROADMAP.md).

Updated 2026-11-04. Base: SimpleX Chat v7.0.3. Evidence rules: PASS only with a recorded test; everything else is NOT TESTED, NOT STARTED or BLOCKED. No issue is closed; all await independent review.

## Summary

| Area | State |
|---|---|
| Release decision | **NO-GO** ([M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md)). Reasons: no real-device or Orbot evidence, leak matrix mostly NOT TESTED, open license gaps, no independent review |
| Milestone M1 (closed alpha) | gates open, see [ROADMAP.md](ROADMAP.md) |
| Redesign "Tül" | in progress; no screen group is recorded as done here yet (nothing to claim until it is shown working on an emulator) |
| Native core | prebuilt official `libsimplex.so`/`libsupport.so` from v7.0.3, verified by SHA-256 and GPG signatures in CI; not built from source by decision ([NATIVE_CORE.md](NATIVE_CORE.md)) |
| Tor-only policy | enforced at the single choke point `apiSetNetworkConfig`, network settings locked read-only, status indicator reads the core's active config: code and unit tests only; device and emulator verification of the new locks and status check NOT TESTED |

## Decisions and changes of 2026-11-04

- **Native core decision:** no build from source; prebuilt official libraries, verified by hash and signature (714d807a9, decision in [NATIVE_CORE.md](NATIVE_CORE.md)). Changes that would need core changes are not done; they are solved in the app layer.
- **Tor-only enforcement** moved to `ChatController.apiSetNetworkConfig`, the only place that builds the network-config command; the network settings screens are read-only; the Tor and privacy status screen checks the core's reported config with `VeilNetworkPolicy.isCompliant` (227de4d4c, 05b3fae8b). Unit tests exist; UI behavior NOT TESTED on a device or emulator.
- **License inventory:** approximate Haskell and native inventory added to `THIRD_PARTY_NOTICES.md` (6704ed0ee). It is derived from build files, not from a build or the binaries.
- **Documentation:** Veil documents moved to `docs/veil/`, one roadmap, vision and design documents added, status consolidated here.

## Open items

- **`android-support` license (must be settled before any distribution).** The package that provides the glue code of `libsupport.so` declares no license and has no license file at the pinned tag. It has to be confirmed with upstream before Veil is distributed. Nobody has been contacted; this only records the item. See [UPSTREAM_LICENSES.md](UPSTREAM_LICENSES.md), Conflicts.
- Haskell and native license inventory is approximate; LGPL notice and replace duties (GMP, libiconv, trove4j) are not implemented for the native libraries.
- Non-English strings, onboarding artwork and deep-link filters still carry upstream branding ([BRANDING.md](BRANDING.md)); the redesign is expected to replace most of this.

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
