# Veil plans (remaining work only)

Updated 2026-11-04. This lists work that is **not done**; finished items live in `DEVELOPMENT_STATUS.md` and `EMULATOR_TESTS.md`. Related items are merged into one workstream. Each workstream has one acceptance gate. Nothing here is a claim that a feature works. Real-device and Orbot results are never replaced by emulator results.

Order rule: finish M1 gates first; later milestones only start after the M1 GO decision. "Needs devices" means blocked until two real Android phones and Orbot are available.

## M1: closed alpha gates

| # | Workstream | Remaining work | Acceptance gate | Needs devices |
|---|---|---|---|---|
| 1 | Real-device pairing and Orbot | Orbot integration check; two-phone pairing, messaging both ways, restart recovery | Recorded run on 2 phones (model, Android version, patch level) | yes |
| 2 | Tor network resilience | Wi-Fi/mobile switch, Tor drop and automatic reconnect, circuit/relay reachability check beyond the SOCKS probe, user-readable network states | Scripted outage cases pass on devices; indicator never claims more than it measures | yes |
| 3 | Leak test matrix | DNS, IPv4, IPv6, UDP, HTTP tests, and "Tor off means no app egress" proof, with packet capture | Capture shows only loopback to the proxy in every case; matrix in `NETWORK_EGRESS.md` all PASS or documented | yes |
| 4 | Delivery and background | Sent/delivered/read semantics, delay and failed-delivery handling, Doze and battery optimization, background delivery, crash and restart recovery | Per-state evidence; clean-pair rerun of the background scenario, then devices | yes (final) |
| 5 | Identity and key safety | Wrong security code, identity-change warning, key change and connection recovery | Each case provoked and the warning observed | partly |
| 6 | Groups | 3-member and 10-member private groups with load; invite link expiry and use limits; admin and member permission matrix; anyone-can-join and invite-spam controls; rejoin policy for removed members | Test report per case; removed-member rejoin either blocked or documented with the link-deletion workaround in the UI | partly |
| 7 | Branding and language | Remaining SimpleX strings, full Turkish translation and language consistency, core-created texts | String audit script passes in CI; Turkish and English complete | no |
| 8 | Native core provenance | Source, version, hash and signature verification of the libraries; optional separate Linux build with hash comparison; license inventory of native and Haskell dependencies | Verified checksums and signature; inventory merged into `THIRD_PARTY_NOTICES.md` (see `NATIVE_BUILD_PLAN.md`) | no |
| 9 | Acceptance and decision | End-to-end acceptance on two phones; independent security review; GO or NO-GO | Signed-off report in `M1_RELEASE_REPORT.md` | yes |

## M2: messaging features (beta)

Audit upstream first and enable or restyle what exists instead of rebuilding it.

1. **Media over Tor.** Photo, video, documents, voice messages through the Tor path only; block direct connections; strip metadata; resumable large transfers; progress indicators; previews and a local gallery. Gate: leak matrix (workstream 3) repeated with media.
2. **Message actions.** Disappearing and view-once messages, edit/delete, reply/quote, reactions, forward, pin; chat pin and archive; in-chat and global local search.
3. **Group and contact management.** Group description and image, advanced admin tools, membership approval, blocking and spam controls, per-chat privacy and notification settings.

## M3: privacy and data safety

1. **App lock.** Biometric and PIN lock, auto-lock timer, hidden preview in the app switcher, secure screenshot policy.
2. **Notification privacy.** Hide content on lock screen, hide sensitive notifications, per-chat setting.
3. **Data lifecycle.** Secure local wipe, retention period, encrypted backup and restore, move to a new phone, backup integrity and recovery tests.
4. **Identity and invites.** Invite expiry and usage limit, revoke and renew, unwanted-request blocking, guard against mixing anonymous identities.
5. **Hardening review.** Minimal permissions, no sensitive data in logs, local key storage audit, traffic metadata review, recurring threat-model tests.

## M4: interface

1. Original Veil design system, light/dark themes, color settings, modern chat list and chat screen.
2. First-run flow including an easy Orbot setup screen and short in-app security explanations.
3. QR flows for adding contacts and joining groups; simplified privacy settings; clear connection and message states; error and recovery screens; friendly group creation.
4. Accessibility: screen reader, dynamic font size; animation and performance pass.

## M5: reliability and performance

1. Retry strategy by error type, timeouts, secure session continuity on network change, correct offline behavior.
2. Message queue tests: no duplicates, order preserved, long-offline recovery.
3. Android background service compatibility across versions.
4. Measurements and optimization: battery, memory, large histories, many chats and groups, low-RAM phones, crash data integrity.

## M6: platforms and advanced

1. Multi-device and sync without increasing account linkage (research first).
2. Own relay support: add a relay, setup guide, relay selection and connection management.
3. Calls: Tor compatibility research for 1:1 voice and video, anonymity and media routing, separate security tests. Calls stay disabled until then.
4. Other platforms (iOS, macOS, Windows, Linux) only after M1 to M3 are stable.

## M7: quality and release (continuous)

Run these from now on, tighten per milestone:

- Acceptance criteria per feature; commit, CI and issue evidence per change.
- Automation: Kotlin unit tests, Android integration tests, two-emulator scenarios, group and invite security scenarios, Orbot and Tor leak automation, security regression suite.
- CI gates: dependency vulnerability scan, SBOM and license checks, APK integrity and signature verification, tracking of official native library releases.
- Compatibility: ARM64, several Android versions, APK size, crash/ANR/performance, error reporting without sensitive data.
- Release: AGPL-3.0 source distribution obligations, changelog per release, closed alpha (M1) and beta (M2) acceptance, final security and quality check, independent security audit.
