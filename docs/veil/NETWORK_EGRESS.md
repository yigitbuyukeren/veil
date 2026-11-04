# Network egress inventory (VEIL-008)

Date 2026-11-04, base `v7.0.3` plus Veil changes. Scope: the Android FOSS build. Inventory comes from source review (`scripts/ci/egress-check.sh` fails CI when a new network API appears outside the files listed in `scripts/ci/egress-allowlist.txt`), upstream docs and the tests below. It is not a proof of completeness for the prebuilt native core (see "Not covered").

## Egress paths

| # | Path | Destination / port / protocol | Mechanism | Veil handling | Status |
|---|---|---|---|---|---|
| 1 | SMP messaging (core) | preset or user SMP servers, TCP, via `.onion` | core with `socksProxy 127.0.0.1:9050`, `socksMode always`, `hostMode onionViaSocks`, `requiredHostMode` | forced by `VeilNetworkPolicy.enforce`; chat not started without proxy | emulator PASS (limited), device NOT TESTED |
| 2 | SMP private routing (relay to relay) | other SMP servers | core `smpProxyMode` | `never`, fallback `prohibit` | configured, NOT TESTED separately |
| 3 | SMP over web port | TCP 443 on SMP hosts | core `smpWebPortServers` | `off` | configured, NOT TESTED |
| 4 | XFTP file transfer (core) | preset XFTP servers, HTTP/2 over TLS, `.onion` | same SOCKS config | covered by the same policy; M1 does not ship file features in the UI review (VEIL-016) | NOT TESTED |
| 5 | Notification server (core) | upstream ntf server | only in "push" mode; Veil flow uses instant/periodic/off | not enabled by the onboarding choices used | NOT TESTED |
| 6 | Link previews | arbitrary https hosts, HTTP via Jsoup/URL | app process, upstream default **direct** | disabled (`LINK_PREVIEWS_ENABLED=false`, function returns null) | code-gated; NOT TESTED in UI |
| 7 | Voice/video calls | STUN/TURN (UDP), WebRTC in WebView | WebView, direct | disabled: outgoing blocked with alert, incoming rejected before ringing (`CALLS_ENABLED=false`) | code-gated; NOT TESTED in UI |
| 8 | External links | any site, opened in another app | `UriHandler.openUri` | confirmation dialog on every http/https link, warns it opens outside Tor and reveals the IP | code only; NOT TESTED in UI |
| 9 | Deep links `simplex:` / simplex.chat | handled locally to start a connection | intent filter | no fetch by the app itself | review only |
| 10 | Images/video playback (Coil, ExoPlayer) | local file URIs only (downloaded via path 4) | local | no remote URI is passed by app code (review) | review only |
| 11 | App updates | desktop only (`desktopMain/AppUpdater`) | n/a on Android | not compiled into the APK | review only |
| 12 | Telemetry / crash reporting / ads SDKs | none found in the dependency list (`android/build.gradle.kts`, `common/build.gradle.kts`) | n/a | CI check on future additions pending | review only |
| 13 | Loopback probe | `127.0.0.1:9050`, TCP | `VeilNetworkPolicy.torProxyReady` | local only | unit tested |
| 14 | Android OS / Google services | outside the app | emulator `google_apis` image shows traffic from other uids | not controllable from the app | out of scope; do not rely on it |

## Tests run

Environment as in `TOR_ONLY.md` (emulator, host Tor, uid 10208, root iptables inside the emulator: a counter rule on non-loopback output of the app uid, and a counter on all UDP/TCP port 53 output).

| Test | Result |
|---|---|
| Tor stopped, app started: alert, chat not started, app sockets = 0, egress counter = 0 | PASS |
| Tor killed while the app runs, link creation attempted for 60 s: only "Connection error", app sockets = 0 at 10 s samples, non-loopback packets from the app uid = 0, DNS packets = 0 | PASS (single run, emulator) |
| Tor running, link creation: the only app socket is `127.0.0.1 -> 127.0.0.1:9050` | PASS (single run) |
| Direct TCP/UDP/DNS/HTTP/QUIC/IPv6 matrix, long sessions, background mode, file transfer, group traffic | NOT TESTED |
| Real device | NOT TESTED |

The packet counter proves the absence of non-loopback packets owned by the app uid in these runs. DNS resolution performed by the system resolver on behalf of the app may not be attributed to the app uid; the DNS counter was global and read 0 in the second test, but it does not cover all resolver paths.

## Not covered (must not be described as protected)

- Behavior of the prebuilt `libsimplex.so` for any network call not controlled by `NetCfg` (unknown; upstream Haskell source not audited here).
- Everything under "NOT TESTED" above, in particular XFTP, notifications and the calls/link-preview gates in the running UI.
- Traffic from other apps and the OS on the device.
- A user who opens a link in the browser after the warning.

Veil must not claim "no network leaks" or "Tor-only" in any user text until the NOT TESTED rows are PASS on a real device (VEIL-017 gate).
