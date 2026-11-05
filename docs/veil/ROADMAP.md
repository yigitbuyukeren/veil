# Veil roadmap

This is the single plan for work that is **not done**. What is done, and with what evidence, is in [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) (status), [EMULATOR_TESTS.md](EMULATOR_TESTS.md) and the other evidence documents. Nothing here is a claim that a feature works. Real-device and Orbot results are never replaced by emulator results.

Direction and design: [VISION.md](VISION.md), [DESIGN.md](DESIGN.md).

Order rule: the M1 gates decide whether a closed alpha can be handed to anyone. The "Tül" redesign is interface work and runs alongside them. M2 and later milestones start only after the M1 GO decision. "Needs devices" means blocked until two real Android phones and Orbot are available.

## M1: closed alpha

Goal: a Tor-only Android closed alpha, not a finished or audited product. Issue groups: F0 foundation (VEIL-001 to 006), F1 private 1:1 messaging (VEIL-007 to 013), F2 small private groups and the release gate (VEIL-014 to 017).

**Required M1 proof:** two actual Android devices; `.onion`/Tor-only message flow; sender off and receiver later on; persisted chat history; a 3-member group and a 10-participant load test; network leak tests. Untested behavior must not be called secure.

| # | Workstream | Remaining work | Acceptance gate | Needs devices |
|---|---|---|---|---|
| 1 | Real-device pairing and Orbot | Orbot integration check; two-phone pairing, messaging both ways, restart recovery | Recorded run on 2 phones (model, Android version, patch level) | yes |
| 2 | Tor network resilience | Wi-Fi/mobile switch, Tor drop and automatic reconnect, circuit/relay reachability check beyond the SOCKS probe, user-readable network states | Scripted outage cases pass on devices; indicator never claims more than it measures | yes |
| 3 | Leak test matrix | DNS, IPv4, IPv6, UDP, HTTP tests, and "Tor off means no app egress" proof, with packet capture | Capture shows only loopback to the proxy in every case; matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) all PASS or documented | yes |
| 4 | Delivery and background | Sent/delivered/read semantics, delay and failed-delivery handling, Doze and battery optimization, background delivery, crash and restart recovery | Per-state evidence; clean-pair rerun of the background scenario, then devices | yes (final) |
| 5 | Identity and key safety | Wrong security code, identity-change warning, key change and connection recovery | Each case provoked and the warning observed | partly |
| 6 | Groups | 3-member and 10-member private groups with load; invite link expiry and use limits; admin and member permission matrix; anyone-can-join and invite-spam controls; rejoin policy for removed members | Test report per case; removed-member rejoin either blocked or documented with the link-deletion workaround in the UI | partly |
| 7 | Branding and language | Remaining SimpleX strings, full Turkish translation and language consistency, core-created texts (solved in the app layer, see [NATIVE_CORE.md](NATIVE_CORE.md)) | String audit script passes in CI; Turkish and English complete. Largely delivered through the redesign below | no |
| 8 | Native core verification | Checksum and signature verification of the prebuilt libraries is done (enforced in CI). Open: the license gaps in [UPSTREAM_LICENSES.md](UPSTREAM_LICENSES.md) (`android-support` has no license, the Haskell inventory is approximate, LGPL notice duties are not implemented) | License gaps closed or resolved with upstream; inventory merged into `THIRD_PARTY_NOTICES.md`. A from-source build is not planned | no |
| 9 | Acceptance and decision | End-to-end acceptance on two phones; independent security review; GO or NO-GO | Signed-off report in [M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md) | yes |

## Redesign "Tül" (implemented, NOT TESTED)

Status 2026-11-04: all nine groups below exist in code and build (arm64 FOSS debug) with unit tests for the logic, the contrast of both themes and the design system. **No screen has been run on a device or an emulator**, so by the acceptance rule below no group is "done" yet. Commits per group are in [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md). Remaining: the emulator run (waits for the owner's go), the upstream screens that are opened on purpose and still have the upstream look ([DESIGN.md](DESIGN.md), "Reachable upstream screens"), English translation, and the remaining upstream wording ([BRANDING.md](BRANDING.md)).

A whole-app redesign on a Veil design system, Turkish-first. It replaces the upstream screens one by one without touching upstream logic (architecture rule in [DESIGN.md](DESIGN.md)). The app builds and runs after every screen group. Desktop and remote connection stay hidden, device migration is under Gelişmiş; calls are an opt-in, off by default and outside Tor (N7).

Screen groups (details in the screen map of [DESIGN.md](DESIGN.md)):

1. **Design system:** tokens, `VeilTheme`, fonts, shared components, routing flag in `App.kt`.
2. **Onboarding:** welcome and story, Orbot setup with live checks, local name, acceptance of the SimpleX conditions of use (no network before acceptance), notification mode, database passphrase.
3. **Chat list:** veil reveal, Tor ring, filters, pinned chats, search.
4. **Chat:** bubbles, composer, message actions, chat info, security code verification.
5. **New chat and QR:** invitation, QR display and scan, paste link, contacts.
6. **Groups:** create, info, members and roles, group link, add members.
7. **Settings:** simplified groups, read-only network settings, Tor and status, about and licenses.
8. **System screens:** app lock, empty and error states, dialogs, database errors.
9. **Language:** Turkish-first strings (done, identical in base and tr), CI audit of leftover upstream strings (done as a ratchet), English translation (not started).

Acceptance: each screen group is shown to work on an emulator before it is called done; device behavior stays NOT TESTED until the M1 device runs. The redesign does not by itself change the release decision.

## M2: messaging features (beta)

Audit upstream first and enable or restyle what exists instead of rebuilding it.

1. **Media over Tor.** Photo, video, documents, voice messages through the Tor path only; block direct connections; strip metadata; resumable large transfers; progress indicators; previews and a local gallery. Gate: leak matrix (M1 workstream 3) repeated with media.
2. **Message actions.** Disappearing and view-once messages, edit/delete, reply/quote, reactions, forward, pin; chat pin and archive; in-chat and global local search.
3. **Group and contact management.** Group description and image, advanced admin tools, membership approval, blocking and spam controls, per-chat privacy and notification settings.

## M3: privacy and data safety

1. **App lock.** Biometric and PIN lock, auto-lock timer, hidden preview in the app switcher, secure screenshot policy.
2. **Notification privacy.** Hide content on lock screen, hide sensitive notifications, per-chat setting.
3. **Data lifecycle.** Secure local wipe, retention period, encrypted backup and restore, move to a new phone, backup integrity and recovery tests.
4. **Identity and invites.** Invite expiry and usage limit, revoke and renew, unwanted-request blocking, guard against mixing anonymous identities.
5. **Hardening review.** Minimal permissions, no sensitive data in logs, local key storage audit, traffic metadata review, recurring threat-model tests.

## M4: interface follow-up

What the redesign does not cover.

1. Color settings. (The light theme "Sis" and the theme setting are implemented, NOT TESTED.)
2. Accessibility: a first pass is implemented (labels, 48 dp targets, contrast tests), NOT TESTED with a screen reader; dynamic font size remains.
3. Animation and performance pass; clear connection and message states; error and recovery screens refined from real use.
4. Localization beyond Turkish and English.

## M5: reliability and performance

1. Retry strategy by error type, timeouts, secure session continuity on network change, correct offline behavior.
2. Message queue tests: no duplicates, order preserved, long-offline recovery.
3. Android background service compatibility across versions.
4. Measurements and optimization: battery, memory, large histories, many chats and groups, low-RAM phones, crash data integrity.

## M6: platforms and advanced

1. Multi-device and sync without increasing account linkage (research first).
2. Own relay support: add a relay, setup guide, relay selection and connection management; self-hosted relay discovery and fallback for operator independence.
3. Calls: 1:1 voice and video exist as an opt-in outside Tor (N7: off by default, warned at every step, relay-only). Still open: Tor-compatible media routing research, a separate threat model for call metadata and real-device tests.
4. Other platforms (iOS, macOS, Windows, Linux) only after M1 to M3 are stable.

Anything here that would need a change inside the prebuilt SimpleX core is not done (see [NATIVE_CORE.md](NATIVE_CORE.md)); it must be solved in the app layer or stay out of scope.

## M7: quality and release (continuous)

Run these from now on, tighten per milestone:

- Acceptance criteria per feature; commit, CI and issue evidence per change.
- Automation: Kotlin unit tests, Android integration tests, two-emulator scenarios, group and invite security scenarios, Orbot and Tor leak automation, security regression suite.
- CI gates: dependency vulnerability scan, SBOM and license checks, APK integrity and signature verification, tracking of official native library releases.
- Compatibility: ARM64, several Android versions, APK size, crash/ANR/performance, error reporting without sensitive data.
- Release: AGPL-3.0 source distribution obligations, changelog per release, closed alpha (M1) and beta (M2) acceptance, final security and quality check, independent security audit.

## Excluded for now

Group voice/video calls, public group discovery, paid features, custom crypto, unbounded public relay usage, changes to the SimpleX core, building the core from source.
