# M1 release report (VEIL-017) — preliminary

Date 2026-11-04 (updated after the second session). **Decision: NO-GO.**

Rule from the issue set: missing security evidence means NO-GO. Required M1 proof was two real Android devices, Tor-only flow, offline delivery, persisted history, groups and leak tests.

| Required proof | Result |
|---|---|
| Two real devices, E2EE 1:1 both ways | NOT TESTED (no devices). Two emulators + host Tor: PASS (`EMULATOR_TESTS.md`), not accepted as device evidence |
| Tor/.onion-only message flow | emulator + host Tor: messaging both ways in 1:1 and group chats, app sockets only to `127.0.0.1:9050` (E3, E4, E11, E19); Orbot and real devices NOT TESTED |
| Sender offline, receiver later | emulator PASS only (E5, E6); real devices NOT TESTED |
| Chat history persists and is encrypted | emulator PASS only (E7, E8); key handling not audited |
| 3-member group and 10-participant load | NOT TESTED. 2-member group via group link: emulator PASS (E11, E19); member role limits observed (E11b) |
| Tor/HTTP/DNS/direct media leak tests | PARTIAL: single emulator run, no app-uid egress; most rows NOT TESTED (`NETWORK_EGRESS.md`) |
| Third-party license inventory | Gradle part done (`THIRD_PARTY_NOTICES.md`); native/Haskell part NOT DONE |
| Native core built from source | NOT DONE, BLOCKED (needs Nix on Linux; `ANDROID_BUILD.md`) |
| Upstream crypto/protocol unchanged | Veil changes are Kotlin app code only (`git diff upstream-v7.0.3 HEAD -- src` empty at time of writing) |
| App branding replaced | PARTIAL (`BRANDING.md`): artwork, promotions, app name in all locales done; "SimpleX Team" contact from the core and non-English strings remain |
| Tor/privacy indicators and licenses in the app | Status screen with proxy reachability and licenses section: emulator PASS (E16, E18); shows a SOCKS listener, not circuit health |
| Delivery-state semantics, wrong-contact and key-change tests | NOT TESTED (E12, E13) |

No security claim from `PRIVACY_CLAIMS.md` may be published. APK status: a debug APK builds locally (arm64, 260 MB, not committed, not signed for release, not distributed).
