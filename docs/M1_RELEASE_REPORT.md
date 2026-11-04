# M1 release report (VEIL-017) — preliminary

Date 2026-11-04. **Decision: NO-GO.**

Rule from the issue set: missing security evidence means NO-GO. Required M1 proof was two real Android devices, Tor-only flow, offline delivery, persisted history, groups and leak tests.

| Required proof | Result |
|---|---|
| Two real devices, E2EE 1:1 both ways | NOT TESTED (no devices) |
| Tor/.onion-only message flow | emulator + host Tor: one link creation succeeded through a relay, only loopback SOCKS socket; messaging not exercised |
| Sender offline, receiver later | NOT STARTED |
| Chat history persists and is encrypted | NOT STARTED |
| 3-member group and 10-participant load | NOT STARTED |
| Tor/HTTP/DNS/direct media leak tests | PARTIAL: single emulator run, no app-uid egress; most rows NOT TESTED (`NETWORK_EGRESS.md`) |
| Third-party license inventory | NOT DONE (`UPSTREAM_LICENSES.md`) |
| Native core built from source | NOT DONE (prebuilt official libraries used) |
| Upstream crypto/protocol unchanged | Veil changes are Kotlin app code only (`git diff upstream-v7.0.3 HEAD -- src` empty at time of writing) |
| App branding replaced | NOT DONE (`BRANDING.md`) |

No security claim from `PRIVACY_CLAIMS.md` may be published. APK status: a debug APK builds locally (arm64, 260 MB, not committed, not signed for release, not distributed).
