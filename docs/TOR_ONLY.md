# Tor-only transport (VEIL-007)

Status 2026-11-04: implemented in code, verified on an **emulator with a host Tor daemon**, not on a real device and not with Orbot.

## Upstream behavior (v7.0.3)

`NetCfg` fields: `socksProxy`, `socksMode` (`always` | `onion`), `hostMode` (`onionViaSocks` | `onion` | `public`), `requiredHostMode`, `smpProxyMode` (private routing), `smpProxyFallback`, `smpWebPortServers`. Upstream defaults send nothing through SOCKS and prefer public hosts; the user can change all of it in Settings > Network. Preset operators publish `.onion` addresses for all 27 preset SMP servers and for XFTP servers (`src/Simplex/Chat/Operators/Presets.hs`; checked by `grep`, availability of the onion services was not measured).

## Veil policy

`VeilNetworkPolicy` (`common/.../model/VeilNetworkPolicy.kt`) is applied inside `ChatController.getNetCfg()`, so every code path that sets the network configuration is overridden regardless of user preferences:

| Field | Value |
|---|---|
| `socksProxy` | `:9050` (127.0.0.1, Orbot default) |
| `socksMode` | `always` |
| `hostMode` / `requiredHostMode` | `onionViaSocks` / `true` (hosts without an onion address are not used) |
| `smpProxyMode` / `smpProxyFallback` | `never` / `prohibit` (no relay-to-relay private routing, no fallback) |
| `smpWebPortServers` | `off` |

Readiness gate: `ChatController.startChat()` first probes the proxy with a SOCKS5 no-auth handshake. If it fails, the chat agent is not started, nothing is sent or received, and an alert ("Tor is not available") with Retry is shown. The probe shows a SOCKS listener exists, not that Tor has circuits; circuit failures appear as connection errors and never as direct connections because the proxy is always configured.

Known gaps: the Settings > Network screens still show upstream controls (their values are overridden; the UI is reworked in VEIL-016). Only the `startChat` path is gated; a Tor drop after start produces connection errors from the core.

## Verification (2026-11-04)

Environment: Android 15 arm64 emulator, host `tor 0.4.x` (Homebrew) on 127.0.0.1:9050 reached through `adb reverse tcp:9050 tcp:9050`, app uid 10208, debug build of this commit. The emulator is not a real device and host Tor is not Orbot.

| Scenario | Result | Evidence |
|---|---|---|
| Policy unit tests (6) | PASS | `:common:testDebugUnitTest --tests chat.simplex.app.VeilNetworkPolicyTest`: enforce closes all clear options; SOCKS5 ready/not-SOCKS/closed-port cases |
| Tor running: create one-time link | PASS once | link created through `smp8.simplex.im`; the app's only socket was `127.0.0.1:<eph> -> 127.0.0.1:9050` (`/proc/net/tcp*`, uid filter) |
| Tor running: repeated link creation | FLAKY | several attempts timed out ("Connection error ... smp18"); Tor log shows hidden-service rendezvous being built and one failed intro-point circuit; relay availability over Tor is unproven |
| Tor stopped, app (re)started | PASS | alert shown, log `startChat: Tor proxy unavailable, chat not started`, app uid had zero sockets, then Retry with Tor running started the chat |
| Direct egress while testing | none seen | iptables rule matching uid 10208 on non-loopback output: 0 packets |
| Two emulators (see `EMULATOR_TESTS.md`): pairing, messages both ways, offline delivery, Tor killed and restarted with a queued message; app sockets only to 127.0.0.1:9050 | PASS (emulator + host Tor, not Orbot, not real devices) |
| Preset servers all carry `.onion` (`scripts/ci/presets-onion-check.sh`, 27 of 27) | PASS (static check of the pinned source) |
| Network policy fields, getNetCfg wrapping, disabled previews/calls and app name guarded in CI (`scripts/ci/policy-check.sh`) | PASS (static grep check; behavior is covered by `VeilNetworkPolicyTest`) |
| Orbot | NOT TESTED | app not exercised against Orbot |
| Real device | NOT TESTED | acceptance item "proxy correctly configured on a real device" is **not met** |
| Tor lost mid-session | NOT TESTED | |
| Server without onion address / wrong proxy in the full app | NOT TESTED (wrong proxy covered by unit test only) | |
| File (XFTP) transfers over Tor | NOT TESTED | |

Do not claim Tor-only operation in any user-facing text until VEIL-008 egress tests and a real-device run pass.
