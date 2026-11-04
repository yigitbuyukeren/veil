# Development status

Updated 2026-11-04. Base: SimpleX Chat v7.0.3. Evidence rules: PASS only with a recorded test; everything else is NOT TESTED, NOT STARTED or BLOCKED. No issue is closed; all await independent review.

| Issue | State | Commits | Notes |
|---|---|---|---|
| #1 VEIL-000 Epic | open | | M1 not reached |
| #2 VEIL-001 threat model | done, awaiting review | 3f187b9, 37418c3 | S1-S17 all NOT TESTED |
| #3 VEIL-002 licenses and relay terms | docs done, **inventory incomplete** | abbdb07 | third-party dependency licenses not inventoried: distribution NO-GO |
| #4 VEIL-003 pinned base | done, awaiting review | 522639f73 | v7.0.3 b3907c9, history preserved |
| #5 VEIL-004 Android build | arm64 FOSS debug PASS; core not built from source | e1b118568 | native libs taken from official release APK; no real device |
| #6 VEIL-005 CI | done, awaiting review | 4b414aaf8, 6479e06c8 | green on main, red demonstrated on a throwaway PR |
| #7 VEIL-006 package identity | done on emulator; branding incomplete | 0f3bc4b93 | real device NOT TESTED; onboarding/logo still upstream |
| #8 VEIL-007 Tor-only via Orbot | implemented; emulator + host Tor only | 1389eba9f, 5532a63b9 | Orbot and real device NOT TESTED; relay reachability over Tor flaky |
| #9 VEIL-008 egress/fail-closed | partial | 6479e06c8 | gates in code; leak tests single-run on emulator; many rows NOT TESTED |
| #10 VEIL-009 anonymous start | partial | c1759dd5f | incognito default on; effect not observed in UI; invite cancel/block NOT TESTED |
| #11 VEIL-010 E2EE 1:1 on two real devices | **BLOCKED** | | needs two physical Android devices (none available) |
| #12 VEIL-011 offline store-and-forward | NOT STARTED | | depends on VEIL-010 |
| #13 VEIL-012 encrypted persistent history | NOT STARTED | | depends on VEIL-010 |
| #14 VEIL-013 background reliability | NOT STARTED | | depends on VEIL-011 |
| #15 VEIL-014 small groups | NOT STARTED | | depends on VEIL-011, 012 |
| #16 VEIL-015 group permissions | NOT STARTED | | depends on VEIL-014 |
| #17 VEIL-016 minimal UI and indicators | NOT STARTED | | depends on VEIL-009, 011, 014; known upstream branding items listed in `BRANDING.md` |
| #18 VEIL-017 M1 validation, GO/NO-GO | report written: **NO-GO** | | see `M1_RELEASE_REPORT.md` |

## Next step

1. Obtain two real Android devices (and Orbot) and run VEIL-010: pair over Tor, exchange messages both ways, verification code, wrong-contact case. Record devices, Android versions and patch levels in an anonymized report.
2. Then VEIL-011, 012 (use the same pair), 013, then groups 014-015, UI 016, and re-run VEIL-007/008 leak tests on the devices.
3. Build the native core from source (Linux/Nix) and replace the prebuilt libraries; finish the third-party license inventory.

## Environment notes

- JDK 17, Android SDK platform 35, NDK 23.1.7779620; build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (Turkish locale breaks resource generation, see `ANDROID_BUILD.md`).
- Test emulator AVD `veil-test` (arm64, Android 15) created locally; host Tor from Homebrew used as the SOCKS proxy through `adb reverse`.
