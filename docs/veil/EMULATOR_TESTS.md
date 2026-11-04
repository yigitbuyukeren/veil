# Emulator test evidence (not device evidence)

Recorded 2026-11-04. Setup: two instances of one arm64 Android 15 `google_apis` AVD (`sdk_gphone64_arm64`, security patch 2024-09-05) on one Mac, debug APK of this repository, host `tor` (Homebrew) as the SOCKS proxy through `adb reverse tcp:9050`. **Host Tor, not Orbot; emulators, not real devices.** Nothing here satisfies the real-device acceptance criteria of VEIL-010, 011, 012, 013. Harness: `scripts/emulator/`.

| # | Scenario | Result | Evidence / limits |
|---|---|---|---|
| E1 | First launch on each emulator: only a local display name requested | PASS | onboarding observed |
| E2 | Pairing by one-time link (relay over Tor), second device shows "Incognito" profile and a generated random name by default | PASS | confirms the incognito default in the UI (VEIL-009) |
| E3 | Message B to A and A to B delivered, both directions | PASS | texts visible on both devices; relays only see ciphertext by protocol design, not independently inspected |
| E4 | Sockets of both app uids during a session | PASS | only `127.0.0.1:<port> -> 127.0.0.1:9050` (3 samples each, `/proc/net`) |
| E5 | Store-and-forward: receiver app stopped, B sends, B stopped, then A started | PASS | A received `offline_msg_1` after both parties were never online together |
| E6 | Tor killed, B sends, Tor restarted | PASS | message stayed queued on B; A received it after Tor returned (about 2 min); no direct path was available |
| E7 | History persists across process restart and APK update | PASS | chats and messages present after force-stop and `adb install -r` |
| E8 | Local database not plaintext | PASS | `files_chat.db` header is not `SQLite format 3`, message strings absent from the file bytes (database key handling is upstream's, not audited) |
| E9 | Tor stopped, app started: chat not started, alert shown | PASS | see `TOR_ONLY.md` |
| E10 | Group created on device A (relay over Tor) | PASS | group `veilgrp` created |
| E11 | Group join through a group link: A creates group, shares the link, B opens it, joins incognito; messages both ways | PASS | link opened via VIEW intent on B; both sides show "connected"; each app uid had only `127.0.0.1:9050` sockets. 3-member and 10-participant scenarios NOT TESTED (no third emulator, memory) |
| E11b | Member role limits: a plain member sees group preferences read-only ("Only group owners can change group preferences") and no invite action; owner can change roles | PASS (observed) | invite revocation, group-link deletion effects and anyone-can-join abuse controls NOT TESTED |
| E12 | Security code: both sides show the same code, "Mark verified" and "Clear verification" change the state on that side | PASS (group member level) | wrong-contact (differing code) and key change NOT TESTED; 1:1 contact verification not exercised separately |
| E13 | Delivery state semantics (sent vs delivered) | NOT TESTED | message Info lists per-member delivery rows, but the state icon is not exposed to the accessibility tree and screenshots are black (secure window); semantics unverified |
| E14 | Background/doze behavior | NOT TESTED | emulator cannot represent vendor battery policy |
| E15 | Relay availability over Tor | FLAKY | several connection attempts timed out on the first runs; later runs worked |
| E16 | Settings > Help & support > "Tor and privacy status" shows proxy reachability, policy enforcement, incognito/link-preview/call defaults | PASS | Tor up: "reachable: yes"; Tor killed, "Check again": "reachable: no". Shows a SOCKS listener only, not circuit health |
| E17 | Empty chat list no longer shows the upstream "Invest" banner (gated by `VeilBranding.UPSTREAM_PROMOTIONS`) | PASS | uiautomator text dump after APK update |
| E18 | In-app open-source licenses section (AGPL-3.0, upstream copyright, link to notices) | PASS | present in the status screen; notices text itself is on GitHub, not bundled |

| E19 | `scripts/emulator/scenario_group.py` end to end on a fresh group (create, link, join, messages both ways, socket check) | PASS | 9/9 steps, run on the same two emulators; repeatable by script, still emulator + host Tor only |
| E20 | Chat-list line "SOCKS proxy answers (Tor circuits not measured)" / "SOCKS proxy not answering: nothing is sent or received", polled every 15 s | PASS | Tor killed: text flipped within 22 s; Tor restarted: flipped back. Measures a local SOCKS listener only, not circuits or relays |
| E21 | "Ask SimpleX Team" card (created by the prebuilt core) hidden from the New chat contact list | PASS | text dump of New chat sheet after APK update; the card still exists in the core database |
| E22 | Group link deleted before use (`scenario_abuse.py` P1) | PASS | B opening the link gets "Connection link removed"; no membership within the window. Absence-of-event over Tor, window 240 s |
| E23 | Group link with initial role observer (P2) | PASS | B joined, cannot send ("You can't send messages!"), A's message reaches B |
| E24 | Owner removes a member | PASS (observed, manual) | B sees "removed you" and cannot send. Finding: removal is not a ban. With the link still active B rejoined as a new incognito identity; delete the link to stop that |
| E25 | Restored old database backup on B (ratchet desync), then "Fix connection" | PASS (manual) | B showed "Decryption error / Fix connection"; no silent loss. After Fix: "encryption agreed / encryption ok", A to B delivered. B to A not tested (B was observer). This is desync recovery, not a verified key-change warning |
| E26 | Wrong security code / identity key change warning | NOT TESTED | cannot force a differing code through the UI |
| E27 | Background and Doze delivery (`scenario_background.py`) | INCONCLUSIVE | both cases failed, but B did not receive even a foreground probe afterwards: the group connection on B was broken after E25. Baseline invalid; VEIL-013 stays NOT TESTED |
