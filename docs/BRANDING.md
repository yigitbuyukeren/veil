# Branding and identity status

| Item | State |
|---|---|
| Android `applicationId` | `io.github.yigitbuyukeren.veil` (upstream: `chat.simplex.app`); installs next to SimpleX |
| FileProvider authority | `io.github.yigitbuyukeren.veil[.suffix].provider` |
| Kotlin namespace / class package | still `chat.simplex.app` (internal, keeps upstream merges small) |
| App label | "Veil" (`MR/base/strings.xml`); other locales still carry upstream translations of `app_name` |
| Launcher icon | original vector mark (`drawable/veil_icon_foreground.xml`); upstream launcher PNGs removed |
| Attribution | Settings > Help: "About Veil" and source-code link (AGPL-3.0, "app for using the SimpleX network") |

## Still upstream-branded (to resolve in VEIL-016, before any distribution)

- Onboarding screens and in-app logo (`icon_foreground_android_common`, `logo*.png`, "Why SimpleX is built").
- Strings in non-English locales; "About SimpleX Chat", help, news, contact-the-team and rating items in Settings.
- Deep-link intent filters for `simplex:` and `simplex.chat`/relay hosts (`AndroidManifest.xml`).
- Desktop module (not part of Veil).

Until these are replaced the app must not be distributed (see `UPSTREAM_LICENSES.md`).
