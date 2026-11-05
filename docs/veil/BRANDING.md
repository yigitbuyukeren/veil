# Branding and identity status

Overall project status: [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md). This file lists the branding items only.

| Item | State |
|---|---|
| Android `applicationId` | `io.github.yigitbuyukeren.veil` (upstream: `chat.simplex.app`); installs next to SimpleX |
| FileProvider authority | `io.github.yigitbuyukeren.veil[.suffix].provider` |
| Kotlin namespace / class package | still `chat.simplex.app` (internal, keeps upstream merges small) |
| App label | "Veil" (`MR/base/strings.xml`); all 21 locale files set `app_name` to Veil (guarded by `scripts/ci/policy-check.sh`) |
| Launcher icon | original vector mark (`drawable/veil_icon_foreground.xml`); upstream launcher PNGs removed |
| Attribution | Settings > "Hakkında ve lisanslar": about Veil and source-code link (AGPL-3.0, "app for using the SimpleX network") |

## Done (2026-11-04)

- In-app logos, tray/notification glyphs and the Android drawable icon replaced with generated original artwork (`scripts/branding/generate_assets.py`).
- Upstream promotion surfaces hidden behind `VeilBranding.UPSTREAM_PROMOTIONS=false`: investment banner, "Why SimpleX is built", news/founder/e-mail contact items, "support the project" section (rate app, star on GitHub, contribute), the empty-chat-list investment banner and the settings/what's-new crowdfunding entries (`crowdfundingAvailable()`).
- Settings > "Tor ve durum" (proxy reachability, policy enforcement, privacy defaults) and "Hakkında ve lisanslar" (open-source licenses section with AGPL-3.0, upstream copyright and a link to `THIRD_PARTY_NOTICES.md`).

- The core's "Ask SimpleX Team" contact card is hidden in the New chat contact list (`VeilBranding.isUpstreamTeamCard`); it still exists in the core database because Veil does not modify the prebuilt core ([NATIVE_CORE.md](NATIVE_CORE.md)); it is hidden in the interface only.

## Redesign status (2026-11-04)

The Veil onboarding, chat list, chat frame, new chat, group, settings and system screens carry no upstream branding. The old onboarding screens, the upstream settings and help pages and the "About SimpleX Chat" page are not reachable on Android any more (checked by tracing navigation, see "Reachable upstream screens" in [DESIGN.md](DESIGN.md)). The remaining spots are the upstream screens that are opened on purpose and the system surfaces below. None of this has been seen on a device.

## Still upstream-branded (to resolve before any distribution)

Found by the reachability audit. After the wording pass of 2026-11-05 (N4b), "SimpleX" occurs 72 times in `MR/base` and 61 times in `MR/tr` (ratchet baseline, `scripts/ci/strings-simplex-baseline.txt`, counted by `scripts/ci/strings-check.sh`; the older 116 and 105 were out of date).

- **Upstream screens opened on purpose** (version and developer tools; group "Gelişmiş", add members, member details, preferences, profile and welcome message are Veil pages since N6e): the strings these screens use were reduced on 2026-11-05 (N4b): links say "invite links", names say "public name", the background texts say "Veil". Still open: the upstream look, and any "SimpleX" left in their strings (counted in the ratchet). Kept on purpose: relay and operator terms, the About pages, supporter badges.
- **Network and servers:** the Veil relay list names no operators; upstream's operator pages remain only in debug builds.
- **Other locales:** non-English files other than Turkish still say SimpleX in many strings (only `app_name` is set to Veil everywhere).
- **Deep link:** the manifest keeps only the `simplex:` scheme (no web host filters, no auto-verify), because invitations on the network use it.
- **Inside upstream, not reachable on Android:** the old onboarding screens and logos, `SimpleXInfo`, "Why SimpleX is built", news and contact items, the upstream chat list and user picker. They stay in the source (upstream code is not deleted).
- Desktop module (not part of Veil).

Strings that the Veil screens use for the network name are intended: the conditions screen, the about page and the relay terms must name SimpleX ([RELAY_USAGE.md](RELAY_USAGE.md)).

Until these are replaced the app must not be distributed (see `UPSTREAM_LICENSES.md`).
