# M1 release report (VEIL-017) — preliminary

Date 2026-11-04 (updated after the native core decision). **Decision: NO-GO.** Overall project status is kept in [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md); this report records only the release gate.

Rule from the issue set: missing security evidence means NO-GO. Required M1 proof was two real Android devices, Tor-only flow, offline delivery, persisted history, groups and leak tests.

| Required proof | Result |
|---|---|
| Two real devices, E2EE 1:1 both ways | NOT TESTED (no devices). Two emulators + host Tor: PASS (`EMULATOR_TESTS.md`), not accepted as device evidence |
| Tor/.onion-only message flow | emulator + host Tor: messaging both ways in 1:1 and group chats, app sockets only to `127.0.0.1:9050` (E3, E4, E11, E19); Orbot and real devices NOT TESTED |
| Sender offline, receiver later | emulator PASS only (E5, E6); real devices NOT TESTED |
| Chat history persists and is encrypted | emulator PASS only (E7, E8); key handling not audited |
| 3-member group and 10-participant load | NOT TESTED. 2-member group via group link: emulator PASS (E11, E19); member role limits observed (E11b) |
| Tor/HTTP/DNS/direct media leak tests | PARTIAL: single emulator run, no app-uid egress; most rows NOT TESTED (`NETWORK_EGRESS.md`) |
| Third-party license inventory | Gradle part done (`THIRD_PARTY_NOTICES.md`). Haskell/native part done by approximation only (from build files, not from a build or the binaries). **Open, NO-GO reasons:** `android-support` has no license (to be confirmed with upstream before distribution), the inventory is approximate, LGPL notice duties are not implemented (`UPSTREAM_LICENSES.md`, Conflicts) |
| Native core verification | Prebuilt official `libsimplex.so`/`libsupport.so` of v7.0.3, SHA-256 and GPG signatures verified, enforced in CI (`ANDROID_BUILD.md`). Building from source is **not planned and not a release gate** (decision 2026-11-04, `NATIVE_CORE.md`). License gaps above stay open |
| Upstream crypto/protocol unchanged | Veil changes are Kotlin app code only (`git diff upstream-v7.0.3 HEAD -- src` empty at time of writing) |
| App branding replaced | PARTIAL (`BRANDING.md`): artwork, promotions, app name in all locales done; core-created "SimpleX Team" card hidden in the interface (the core is not changed); non-English strings remain |
| Tor/privacy indicators and licenses in the app | Status screen with proxy reachability and licenses section: emulator PASS (E16, E18); shows a SOCKS listener, not circuit health. Tor-only enforcement at `apiSetNetworkConfig`, read-only network settings and the active-config status check: code and unit tests only, NOT TESTED in the running app |
| Delivery-state semantics, wrong-contact and key-change tests | NOT TESTED (E12, E13) |
| In-app privacy statements | **NO-GO for distribution** until each of the 25 statements listed in "In-app statements pending evidence" in [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md) (Tor-only and fail-closed wording, no-account and incognito wording, no push server, database encryption, security code wording, delivery-state words) is backed by its evidence or softened to what is measured. The copy stays in the app while it is not distributed. Audit of all `veil_` strings: 0 prohibited, 2 softened |
| Redesign "Tül" interface | implemented for all screen groups, **NOT TESTED** on a device or an emulator; the owner has not yet released the emulator run. Does not change the decision |

No security claim from `PRIVACY_CLAIMS.md` may be published. APK status: a debug APK builds locally (arm64, 260 MB, not committed, not signed for release, not distributed).
