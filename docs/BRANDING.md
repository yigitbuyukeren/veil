# Branding and identity status

| Item | State |
|---|---|
| Android `applicationId` | `io.github.yigitbuyukeren.veil` (upstream: `chat.simplex.app`); installs next to SimpleX |
| FileProvider authority | `io.github.yigitbuyukeren.veil[.suffix].provider` |
| Kotlin namespace / class package | still `chat.simplex.app` (internal, keeps upstream merges small) |
| App label | "Veil" (`MR/base/strings.xml`); all 21 locale files set `app_name` to Veil (guarded by `scripts/ci/policy-check.sh`) |
| Launcher icon | original vector mark (`drawable/veil_icon_foreground.xml`); upstream launcher PNGs removed |
| Attribution | Settings > Help: "About Veil" and source-code link (AGPL-3.0, "app for using the SimpleX network") |

## Done (2026-11-04)

- In-app logos, tray/notification glyphs and the Android drawable icon replaced with generated original artwork (`scripts/branding/generate_assets.py`).
- Upstream promotion surfaces hidden behind `VeilBranding.UPSTREAM_PROMOTIONS=false`: investment banner, "Why SimpleX is built", news/founder/e-mail contact items, "support the project" section (rate app, star on GitHub, contribute), the empty-chat-list investment banner and the settings/what's-new crowdfunding entries (`crowdfundingAvailable()`).
- Settings > Help & support > "Tor and privacy status": proxy reachability, policy enforcement, privacy defaults and an open-source licenses section (AGPL-3.0, upstream copyright, link to `THIRD_PARTY_NOTICES.md`).

- The core's "Ask SimpleX Team" contact card is hidden in the New chat contact list (`VeilBranding.isUpstreamTeamCard`); it still exists in the core database until the core is rebuilt from source.

## Still upstream-branded (to resolve in VEIL-016, before any distribution)

- Onboarding screens and in-app logo (`icon_foreground_android_common`, `logo*.png`, "Why SimpleX is built").
- Other strings in non-English locales still say SimpleX; "About SimpleX Chat", help, news, contact-the-team and rating items in Settings.
- Deep-link intent filters for `simplex:` and `simplex.chat`/relay hosts (`AndroidManifest.xml`).
- Desktop module (not part of Veil).

Until these are replaced the app must not be distributed (see `UPSTREAM_LICENSES.md`).
