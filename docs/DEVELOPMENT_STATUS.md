# Development status

Updated 2026-11-04 (second session). Base: SimpleX Chat v7.0.3. Evidence rules: PASS only with a recorded test; everything else is NOT TESTED, NOT STARTED or BLOCKED. No issue is closed; all await independent review.

| Issue | State | Commits | Notes |
|---|---|---|---|
| #1 VEIL-000 Epic | open | | M1 not reached |
| #2 VEIL-001 threat model | done, awaiting review | 3f187b9, 37418c3 | S1-S17 all NOT TESTED |
| #3 VEIL-002 licenses and relay terms | docs done; Gradle inventory done (273 artifacts, 1 LGPL-2.1); **native/Haskell inventory open** | abbdb07, c4856e810 | distribution NO-GO until native part is done |
| #4 VEIL-003 pinned base | done, awaiting review | 522639f73 | v7.0.3 b3907c9, history preserved |
| #5 VEIL-004 Android build | arm64 FOSS debug PASS; core not built from source | e1b118568 | native libs taken from official release APK; no real device |
| #6 VEIL-005 CI | done, awaiting review | 4b414aaf8, 6479e06c8 | green on main, red demonstrated on a throwaway PR |
| #7 VEIL-006 package identity | done on emulator; branding incomplete | 0f3bc4b93 | real device NOT TESTED; onboarding/logo still upstream |
| #8 VEIL-007 Tor-only via Orbot | implemented; emulator + host Tor only | 1389eba9f, 5532a63b9 | Orbot and real device NOT TESTED; relay reachability over Tor flaky |
| #9 VEIL-008 egress/fail-closed | partial | 6479e06c8 | gates in code; leak tests single-run on emulator; many rows NOT TESTED |
| #10 VEIL-009 anonymous start | partial | c1759dd5f | incognito default on; effect not observed in UI; invite cancel/block NOT TESTED |
| #11 VEIL-010 E2EE 1:1 on two real devices | **BLOCKED for the physical criteria**; two-emulator evidence recorded | a2d841cf5 | `EMULATOR_TESTS.md` E1-E4; contact verification and wrong-contact NOT TESTED |
| #12 VEIL-011 offline store-and-forward | emulator evidence only | a2d841cf5 | E5, E6 PASS; delivery-state semantics NOT TESTED; real devices NOT TESTED |
| #13 VEIL-012 encrypted persistent history | emulator evidence only | a2d841cf5 | E7, E8 PASS; key storage not audited; no Veil code change |
| #14 VEIL-013 background reliability | NOT STARTED | | emulator cannot represent vendor battery policy |
| #15 VEIL-014 small groups | group creation only | | E10; member join, 3 and 10 participants NOT TESTED (UI driving failed; contacts connected incognito cannot be invited, so joining needs the group link) |
| #16 VEIL-015 group permissions | NOT STARTED | | |
| #17 VEIL-016 minimal UI and indicators | partial | c4856e810 | brand artwork replaced, upstream promotion surfaces hidden, Tor-unavailable alert; Tor/connection indicator, in-app licenses screen, non-English locale strings, core-created "SimpleX Team" contact still open |
| #18 VEIL-017 M1 validation, GO/NO-GO | report: **NO-GO** | | see `M1_RELEASE_REPORT.md` |

## Next step

1. Obtain two real Android devices (and Orbot) and run VEIL-010: pair over Tor, exchange messages both ways, verification code, wrong-contact case. Record devices, Android versions and patch levels in an anonymized report.
2. Then VEIL-011, 012 (use the same pair), 013, then groups 014-015, UI 016, and re-run VEIL-007/008 leak tests on the devices.
3. Build the native core from source (needs Linux x86_64 with Nix; BLOCKED here) and replace the prebuilt libraries; finish the native part of the license inventory.
4. Drive group join through a group link (copy link on device A, open it on B) to test 3-member and 10-participant groups; needs 3+ devices or a host with more RAM for emulators.

## Environment notes

- JDK 17, Android SDK platform 35, NDK 23.1.7779620; build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (Turkish locale breaks resource generation, see `ANDROID_BUILD.md`).
- Test emulator AVD `veil-test` (arm64, Android 15) created locally; host Tor from Homebrew used as the SOCKS proxy through `adb reverse`.
