# Development status

Updated 2026-11-04 (third session). Base: SimpleX Chat v7.0.3. Evidence rules: PASS only with a recorded test; everything else is NOT TESTED, NOT STARTED or BLOCKED. No issue is closed; all await independent review.

| Issue | State | Commits | Notes |
|---|---|---|---|
| #1 VEIL-000 Epic | open | | M1 not reached |
| #2 VEIL-001 threat model | done, awaiting review | 3f187b9, 37418c3 | S1-S17 all NOT TESTED |
| #3 VEIL-002 licenses and relay terms | docs done; Gradle inventory done (273 artifacts, 1 LGPL-2.1); **native/Haskell inventory open** | abbdb07, c4856e810 | distribution NO-GO until native part is done |
| #4 VEIL-003 pinned base | done, awaiting review | 522639f73 | v7.0.3 b3907c9, history preserved |
| #5 VEIL-004 Android build | arm64 FOSS debug PASS; core not built from source | e1b118568 | native libs taken from official release APK; no real device |
| #6 VEIL-005 CI | done, awaiting review | 4b414aaf8, 6479e06c8 | green on main, red demonstrated on a throwaway PR |
| #7 VEIL-006 package identity | done on emulator; branding partial | 0f3bc4b93, c4856e810, f0add36b9 | real device NOT TESTED; see `BRANDING.md` for remaining items |
| #8 VEIL-007 Tor-only via Orbot | implemented; emulator + host Tor only | 1389eba9f, 5532a63b9 | Orbot and real device NOT TESTED; relay reachability over Tor flaky |
| #9 VEIL-008 egress/fail-closed | partial | 6479e06c8 | gates in code; leak tests single-run on emulator; many rows NOT TESTED |
| #10 VEIL-009 anonymous start | partial | c1759dd5f | incognito default on and observed on the joining side (E2, E11); invite cancel/block NOT TESTED |
| #11 VEIL-010 E2EE 1:1 on two real devices | **BLOCKED for the physical criteria**; two-emulator evidence recorded | a2d841cf5 | `EMULATOR_TESTS.md` E1-E4; security code match + mark/clear verified PASS at group-member level (E12); wrong-contact and key change NOT TESTED |
| #12 VEIL-011 offline store-and-forward | emulator evidence only | a2d841cf5 | E5, E6 PASS; delivery-state semantics NOT TESTED; real devices NOT TESTED |
| #13 VEIL-012 encrypted persistent history | emulator evidence only | a2d841cf5 | E7, E8 PASS; key storage not audited; no Veil code change |
| #14 VEIL-013 background reliability | NOT STARTED | | emulator cannot represent vendor battery policy |
| #15 VEIL-014 small groups | 2-member group via group link: emulator PASS | cfead9cfc | E10, E11, E19 (scripted); 3-member and 10-participant NOT TESTED (needs a third emulator or devices) |
| #16 VEIL-015 group permissions | partial | cfead9cfc, this session | E11b, E22-E24: link deletion, observer role and member removal observed on emulators; removal is not a ban while the link is active; anyone-can-join link abuse beyond that NOT TESTED |
| #17 VEIL-016 minimal UI and indicators | partial | c4856e810, bc40525b3, f0add36b9 | artwork, promotions hidden, Tor-unavailable alert, Tor and privacy status screen with licenses section (E16-E18), app name in all locales; no always-visible indicator in the chat list, non-English strings, core-created "SimpleX Team" contact still open |
| #18 VEIL-017 M1 validation, GO/NO-GO | report: **NO-GO** | | see `M1_RELEASE_REPORT.md` |

## Next step

1. Obtain two real Android devices (and Orbot) and run VEIL-010: pair over Tor, exchange messages both ways, verification code, wrong-contact case. Record devices, Android versions and patch levels in an anonymized report. Then VEIL-011, 012, 013, 014-015, 016, and re-run VEIL-007/008 leak tests on the devices.
2. Build the native core from source on Linux x86_64 (`NATIVE_BUILD_PLAN.md`; BLOCKED here) and finish the native part of the license inventory.
3. With a third emulator or device: 3-member and 10-participant groups, group-link deletion/invite revocation, anyone-can-join abuse controls (`scripts/emulator/scenario_group.py` covers the 2-member case).
4. Not yet tried or inconclusive: delivery-state semantics (icons not readable), wrong-contact and key-change warning (E26), background/doze (E27 inconclusive; rerun `scenario_background.py` on a clean pair).

## Environment notes

- JDK 17, Android SDK platform 35, NDK 23.1.7779620; build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (Turkish locale breaks resource generation, see `ANDROID_BUILD.md`).
- Test emulator AVD `veil-test` (arm64, Android 15) created locally; host Tor from Homebrew used as the SOCKS proxy through `adb reverse`.
