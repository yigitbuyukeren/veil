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
| E11 | Group with 2+ members, 3-member and 10-participant scenarios | NOT TESTED | adding a member through the UI could not be driven (invite action showed no screen); third emulator not run (memory) |
| E12 | Contact verification code, wrong-contact case, key change | NOT TESTED | |
| E13 | Delivery state semantics (sent vs delivered) | NOT TESTED | only presence of messages checked |
| E14 | Background/doze behavior | NOT TESTED | emulator cannot represent vendor battery policy |
| E15 | Relay availability over Tor | FLAKY | several connection attempts timed out on the first runs; later runs worked |
| E16 | Settings > Help & support > "Tor and privacy status" shows proxy reachability, policy enforcement, incognito/link-preview/call defaults | PASS | Tor up: "reachable: yes"; Tor killed, "Check again": "reachable: no". Shows a SOCKS listener only, not circuit health |
| E17 | Empty chat list no longer shows the upstream "Invest" banner (gated by `VeilBranding.UPSTREAM_PROMOTIONS`) | PASS | uiautomator text dump after APK update |
| E18 | In-app open-source licenses section (AGPL-3.0, upstream copyright, link to notices) | PASS | present in the status screen; notices text itself is on GitHub, not bundled |
