# Tor-only transport (VEIL-007)

Overall status: [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md). Evidence in this file: the earlier version was verified on an **emulator with a host Tor daemon** (2026-11-04), not on a real device and not with Orbot; the choke-point enforcement, locks and status check described below are code and unit tests only.

## Upstream behavior (v7.0.3)

`NetCfg` fields: `socksProxy`, `socksMode` (`always` | `onion`), `hostMode` (`onionViaSocks` | `onion` | `public`), `requiredHostMode`, `smpProxyMode` (private routing), `smpProxyFallback`, `smpWebPortServers`. Upstream defaults send nothing through SOCKS and prefer public hosts; the user can change all of it in Settings > Network. Preset operators publish `.onion` addresses for all 27 preset SMP servers and for XFTP servers (`src/Simplex/Chat/Operators/Presets.hs`; checked by `grep`, availability of the onion services was not measured).

## Veil policy

`VeilNetworkPolicy` (`common/.../model/VeilNetworkPolicy.kt`) is applied inside `ChatController.getNetCfg()` and, as the final guard, at `apiSetNetworkConfig` (see "Single choke point" below), so every code path that sets the network configuration is overridden regardless of user preferences:

| Field | Value |
|---|---|
| `socksProxy` | `:9050` (127.0.0.1, Orbot default) |
| `socksMode` | `always` |
| `hostMode` / `requiredHostMode` | `onionViaSocks` / `true` (hosts without an onion address are not used) |
| `smpProxyMode` / `smpProxyFallback` | `never` / `prohibit` (no relay-to-relay private routing, no fallback) |
| `smpWebPortServers` | `off` |

Readiness gate: `ChatController.startChat()` first probes the proxy with a SOCKS5 no-auth handshake. If it fails, the chat is not started, nothing is sent or received, and an alert ("Tor is not available") with Retry is shown. The probe shows a SOCKS listener exists, not that Tor has circuits; circuit failures appear as connection errors and never as direct connections because the proxy is always configured.

Single choke point: `ChatController.apiSetNetworkConfig` is the only place that builds `CC.APISetNetworkConfig`, and it always sends `VeilNetworkPolicy.enforce(cfg)`, whoever the caller is (startup, the network settings screens, the migration temporary database). `setNetCfg` also stores the enforced config, so saved preferences never show a non-compliant value. `scripts/ci/policy-check.sh` fails if `CC.APISetNetworkConfig(` appears anywhere else or if the function stops calling `enforce`.

Locked settings: in Settings > Network & servers the SOCKS switch (always on), proxy host/port/authentication and onion host mode (read-only, fixed to 127.0.0.1:9050 with required onion hosts), private routing mode and fallback, and web port servers are shown disabled with the note "Veil yalnızca Tor üzerinden bağlanır; bu ayar değiştirilemez." Timeouts and keep-alive remain editable; they do not change routing.

Status indicator: "Tor-only network settings enforced" in the Tor and privacy status screen reads the config the core reports as active (`APIGetNetworkConfig`) and checks it with `VeilNetworkPolicy.isCompliant`; if the call fails it shows "bilinmiyor" (unknown), never "yes". Web links in messages show the external-link warning (opens outside Tor) in the confirmation dialog.

Status of this behaviour: implemented at code level and covered by unit tests of `enforce`/`isCompliant`; the UI locks, the active-config status check and the dialog text are NOT TESTED on a device. Known gaps: a Tor drop after start produces connection errors from the core (only the `startChat` path is gated); link previews, which bypass the core, are disabled rather than routed. Calls are the one deliberate exception, see "Calls" below.


## Calls: opt-in, outside Tor (N7)

Messages and files go only through Tor. Voice and video calls do not: call signalling (ringing, accept) is an ordinary encrypted SimpleX message and goes through Tor, but the media is sent by WebRTC, which does not use the SOCKS proxy.

- Default: off. `VeilNetworkPolicy.CALLS_ENABLED` stays `false`; the user preference `veilCallsOptIn` (default `false`) turns calls on, and every call surface reads `VeilNetworkPolicy.callsAllowed() = CALLS_ENABLED || veilCallsOptIn`. `policy-check.sh` fails if the constant or the default changes.
- Turned on in Settings > Privacy > "Sesli ve görüntülü aramalar" after a one-time explanation. Each outgoing call is confirmed ("Bu arama Tor üzerinden gitmez"), an incoming call shows the "Tor dışı arama" label and is confirmed before accepting, and a fixed "Tor dışı" strip is shown during the call. The Tor and status screen shows "Aramalar: açık, Tor dışı" while calls are on.
- Relay only: `VeilNetworkPolicy.callRelayPolicy` forces `iceTransportPolicy: relay` for both directions (`CallManager`, `ChatController` call offer handling); the user preference `webrtcPolicyRelay` and the ICE server screen cannot weaken it (the screen stays hidden). The other party never sees the user's IP address; the call server (TURN) does, with timing. The call content is end-to-end encrypted.
- Off: call buttons and the calls preference hidden, incoming calls rejected before ringing, a call attempt explains where to turn calls on.
- Status: code and unit tests (`callsAllowed`, relay policy) only; the dialogs, label, strip and the relay connection type in the call info are NOT TESTED on a device.

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
