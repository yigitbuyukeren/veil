# Veil design

How the Veil interface is built and how it should look and behave. Written for the whole-app redesign called "Tül" (Turkish for tulle, the sheer fabric of a veil). The redesign is tracked in [ROADMAP.md](ROADMAP.md); progress is in [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md). The ideas behind it are in [VISION.md](VISION.md). All screen groups of the screen map are implemented on Android and build; **none of the interface has been run on a device or emulator**, so every visual and gesture statement below describes the code, not an observed result. Tokens may still be refined after the first device run.

## Architecture rule: logic stays upstream, the interface lives in Veil

The whole interface is being rewritten, but taking updates from upstream must stay possible. Therefore:

- All new interface code lives in its own package, `chat.simplex.common.veil`:
  - `veil/design/` for design tokens, theme, fonts and components;
  - `veil/screens/<group>/` for the new screens;
  - `veil/data/` for small adapters the screens need (only `ChatController` / `ChatModel` calls).
- **New screens call existing logic; they do not copy it.** If behavior must change, it is done in a small adapter in `veil/data/`.
- The switch point is `App.kt` (`AppScreen`, `MainScreen` and the onboarding stages). A `VeilUi` flag controls the routing. Screens are replaced one by one and the app builds and runs after every step.
- Upstream screen files are edited only at small points marked `// Veil:` (routing, hooks into the Veil screens, guards that close paths to hidden features). Nothing is deleted. Upstream screens that Veil no longer opens still compile; some are opened on purpose, see "Reachable upstream screens" below. When an upstream update is merged, conflicts should be limited to the routing in `App.kt` and those marked points.
- Upstream's theme system (`ThemeManager`, wallpapers) is not used. Veil has its own `VeilTheme`. The Material colors are derived from `VeilTheme` so that any remaining upstream screens stay readable.
- Policy code (`VeilNetworkPolicy`, the single choke point `apiSetNetworkConfig`) is not part of the interface layer and is not replaced by the redesign.

## Concept: "Tül"

The name comes from the idea of a veil, and it matches the layers of Tor.

### Signature elements

| Element | What it does | Why |
|---|---|---|
| **Veil reveal** | Previews in the chat list and notifications sit behind a frosted veil. Long-press or opening the chat lifts it (280 ms). | Makes privacy a gesture and gives real protection against shoulder surfing. |
| **Mask avatars** | Instead of photos, deterministic geometric patterns generated per identity. Incognito profiles get their own pattern. | Makes "a separate identity for every chat" visible and tells chats apart without photos. |
| **Tor ring** | A ring in the top bar that breathes, with three states: *proxy answering*, *connecting*, *no Tor: nothing is being sent*. | Shows the connection state honestly. It cannot measure circuits, so it never says "Tor is healthy". |
| **Folded-corner bubbles** | One corner of each message bubble is cut like folded cloth. | A memorable shape that fits the concept. |
| **Layered surfaces** | Screens are built from overlapping translucent layers with a light grain texture (about 3%). | Gives depth and carries the layer idea through. |

### Tokens (initial proposal)

- **Dark theme first.** Background "Night" `#0B0F14`. Veil surfaces are white at 6 to 12 percent.
- **Flow gradient** (accent): `#7CF5D3` to `#8A7CFF` to `#FF8FB1`. Used sparingly: the Tor ring, the primary button, the selected state.
- **Status colors:** warning `#FFB547`, error `#FF5D5D`. There is no "success green".
- **Light theme "Sis"** (mist): page `#ECEFF7`, raised surfaces `#F5F6FB` / `#FFFFFF`, ink `#0D1121` with the same muted levels (72 % and 66 %), layers are dark-iris tints (`#1B2140` at 4 to 10 %) instead of white ones, grain at half strength. Fills keep the bright mint, iris, rose gradient with dark labels; thin strokes, rings, links and switch tracks use deeper stops (`#0A9275`, `#5A4BE6`, `#D63E78`) because the bright ones do not reach 3:1 on a light page. Warning `#8A4B00`, error `#C42B3A` (text `#B3202F`). There is no success green here either.
- **Theme setting:** Appearance offers Gece (default), Sis and Sistem (follows the OS). It is stored in the Veil preference `veilTheme` (`gece`, `sis`, `sistem`), separate from upstream's theme preferences, which are ignored.
- **Contrast:** all text and surface pairs of both themes are asserted in `VeilContrastTest` (4.5:1 for text, 3:1 for strokes and UI parts). Measured examples, Gece / Sis: ink on page 17.1 / 16.3, ink2 9.1 / 8.8, ink3 6.3 / 5.7, warning 10.9 / 5.9, danger text 8.5 / 5.8, accent 5.9 / 5.1, label on gradient 6.0 / 5.8, weakest stroke stop 5.9 / 3.4.
- **Accessibility:** icon-only buttons carry Turkish labels and are at least 48 dp; decorative art (grain, mask tiles, rings, onboarding art) is hidden from TalkBack; the Tor ring state is announced politely on the chat list header only; a veiled preview stays hidden from accessibility services until revealed.
- **Type:** a display face with character for headings (candidate: Bricolage Grotesque, OFL license) and Inter for body text, which the app already bundles. Acceptance condition: the letters ğ, ı, İ, ş, ç, ö, ü must render correctly. The font license is added to `THIRD_PARTY_NOTICES.md` when a font is added.
- **Shapes:** sheet top corners 28 dp, cards 20 dp, bubbles 18 dp with one cut corner.
- **Motion:** veil reveal 280 ms ease-out; Tor ring breathing cycle 2.4 s. The Android "reduce animations" setting is respected.

### Honesty rules for the interface

- No shield, lock or "you are safe" badge. Text on screen is chosen from the allowed list in [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md).
- No green indicator for anything that is not measured.
- Every action that leaves the app (such as opening a web link) warns that it opens outside Tor and reveals the IP address.
- Network settings are shown read-only (Tor-only is locked), see [TOR_ONLY.md](TOR_ONLY.md).

## Screen map

| Group | Screens |
|---|---|
| **A. Onboarding** | Welcome; three-step story ("No account", "Tor only", "A separate identity for every chat"); **Orbot setup** with live checks; local display name; **acceptance of the SimpleX conditions of use** (a relay requirement: no network connection before acceptance, see [RELAY_USAGE.md](RELAY_USAGE.md)); notification mode; database passphrase |
| **B. Main** | Chat list (veil reveal, Tor ring, filters, pinned chats); search |
| **C. Chat** | One-to-one chat; bubbles; composer; message actions (reply, edit, delete, react, forward); chat info; security code verification |
| **D. Connect** | New chat sheet; create invitation and QR; paste link; scan QR; contacts |
| **E. Groups** | Create group; group info; members and roles; group link (observer role, deleting the link); add members |
| **F. Settings** | Simplified: Profiles, Privacy, Notifications, Appearance, Database and passphrase, **Tor and status**, About and licenses. Network settings are **read-only** |
| **G. System** | App lock; empty and error states; dialogs; database error screens |
| **Hidden** | Calls, desktop, remote connection ("link a mobile", "use from desktop"), channel creation, upstream promotions, the core's "Ask SimpleX Team" card, terminal (debug builds only). How each path is closed is in "Reachable upstream screens" |

## Decisions taken during implementation

These were decided while the screens were built; the code is the reference.

- **Chat list gestures.** Previews sit behind the veil. Pressing and holding a row (after 150 ms) reveals the preview until release, and the release does not open the chat. The eye button on the row toggles the reveal, and the toggles are dropped when the app leaves the foreground. Tapping the row lifts the veil and opens the chat. Tapping the avatar opens the chat actions sheet (mark read or unread, favorite, mute, clear, delete or leave; pending invitations and requests have their own actions). Search matches names and preview text.
- **Hybrid chat rendering.** The chat keeps upstream's list, loading, selection and composer logic. Plain text messages (not forwarded, live, timed, deleted, with a file or in the large-emoji layout) are drawn by Veil as folded-corner bubbles. Media, files, forwarded and timed items, live messages, events and everything else stay upstream-rendered, inside the Veil frame (night background, mask avatars, date dividers, Veil top bar and composer frame). Holding a message opens the Veil actions sheet, which runs the same dialogs upstream uses.
- **Delivery words.** Outgoing messages show Turkish state words: Gönderiliyor, Gönderildi, Teslim edildi, partial variants, Yeniden deneniyor, Gönderilemedi. **"Okundu" (read) is never shown for outgoing messages**: the SimpleX protocol has no read state for them, only delivery receipts. What the states mean on a real network is NOT TESTED ([DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md)).
- **Theme modes.** Appearance offers Gece (default, dark), Sis (light) and Sistem (follows the OS). Stored in the Veil preference `veilTheme`; upstream's theme preferences and wallpapers are ignored.
- **"Gelişmiş" rows open full upstream screens.** The Veil contact info, group info and settings home end with a "Gelişmiş" row that opens upstream's complete screen (contact info with connection resync and address switch, group info with every member action, version and developer tools). They keep options the Veil screens do not repeat, including safety-critical ones. They are not redesigned yet.
- **Settings open some upstream screens on purpose** (profiles, notifications, database and passphrase, privacy "more", network and servers read-only, contact and group preferences). Reason: they carry safety-critical options (database export and import, passphrase, profile hiding and deletion) whose logic must not be copied.
- **Network settings** are shown read-only from the Tor and status screen; Tor-only is enforced at `apiSetNetworkConfig`, not in the screens.
- **Honest status wording.** The Tor ring and the status screen say only whether the local proxy answers; they never say that circuits work. Wording that goes beyond what has been measured is tracked in the "In-app statements pending evidence" section of [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md).
- **What is hidden:** voice and video calls (call buttons, the calls preference and the WebRTC server settings are not offered, incoming calls are rejected without ringing, a call attempt shows an explanation), moving the app to another device, linking a mobile or desktop, channel creation, upstream promotions (investment banner, news, founder contact, crowdfunding, rate and star items), the core-created "Ask SimpleX Team" contact in lists, link previews (they would leave Tor), and the terminal (debug builds only).

## Reachable upstream screens (Android)

Audit of 2026-11-04, tracing navigation from every Veil screen. Status values: **Veil** (Veil screen), **on purpose** (upstream screen opened deliberately), **closed** (hidden by design: not reachable, or closed with a `// Veil:` guard), **gap** (reachable with upstream look or wording; a later pass could redesign or close it).

| Entry point | Screen | Status |
|---|---|---|
| Settings, Profiles | `UserProfilesView` (switch, create, hide, delete) | on purpose; gap: upstream look |
| Settings, Notifications | `NotificationsSettingsView` | on purpose; gap: upstream look, SimpleX wording |
| Settings, Database and passphrase | `DatabaseView` (export, import, passphrase, start and stop chat, delete) | on purpose (safety-critical) |
| Settings, Privacy, "Diğer gizlilik ayarları" | `PrivacySettingsView`, `MorePrivacyView` (link previews toggle is policy-locked) | on purpose; gap: upstream look |
| Settings, Tor and status, "Sunucular ve ağ" | `NetworkAndServersView` (read-only; WebRTC servers section now hidden) | Veil read-only relay list (`VeilServersScreen`: hosts of enabled message and file servers, `.onion` marked); upstream's `NetworkAndServersView` only in debug builds |
| Settings, Gelişmiş | `VersionInfoView`, `DeveloperView` (terminal and terminal-app link now debug builds only) | on purpose; gap: upstream look |
| Settings, Hakkında | Veil about and licenses; design gallery in debug builds | Veil |
| Chat list, avatar sheet, rename connection | `ContactConnectionInfoView` | on purpose; gap |
| Chat info (direct), "Kişi tercihleri" | `ContactPreferencesView` (calls preference now hidden) | on purpose; gap |
| Chat info (direct), "Gelişmiş" | `ChatInfoView` (call buttons now hidden) | on purpose; gap |
| Chat info, security code | Veil verify screen with `ScanCodeView` camera scanner | Veil, upstream scanner |
| Group info, preferences, edit, welcome | `GroupPreferencesView`, `GroupProfileView`, `GroupWelcomeView` | on purpose; gap |
| Group info, "Gelişmiş" | `GroupChatInfoView`, `AddGroupMembersView`, `GroupLinkView` | on purpose; gap |
| Group member, "Ayrıntılar" | `GroupMemberInfoView` (call buttons now hidden) | on purpose; gap |
| Groups that use relays (channels) | opened in upstream group info; creation is not offered | closed (creation); gap (viewing) |
| New chat sheet: invite, paste, scan, group | Veil sheet with `QRCodeScanner` and upstream connect-plan dialogs | Veil, upstream scanner |
| Chat items: media, files, events, call history | upstream renderers inside the Veil frame | on purpose; call items are history only (incoming calls are rejected) |
| Link in a message | Tor warning dialog, then the external app | Veil |
| Lock, passcode, database error, splash, alerts | Veil system screens | Veil |
| User picker ("use from desktop", "link a mobile") | `UserPicker` is composed, but nothing opens it on Android | closed (not reachable) |
| Upstream `SettingsView`, `HelpView`, `WhatsNewView`, `SimpleXInfo` ("About SimpleX Chat") | reachable only from the upstream chat list and user picker | closed (not reachable) |
| Device migration ("Başka cihaza taşı", "Başka bir cihazdan taşı") | `MigrateFromDeviceView` from Settings > Gelişmiş (`VeilAdvancedScreen`, Tor proxy checked first); `MigrateToDeviceView` from the Veil name step of the onboarding; `App.kt` still does not show a leftover migration state on Android | on purpose (Gelişmiş) |
| Upstream chat list, new chat sheet, channel creation, promotions | not routed on Android; promotions stay off behind `VeilBranding.UPSTREAM_PROMOTIONS` | closed |
| Calls: call buttons, calls preference, WebRTC servers, incoming calls | guarded by `VeilNetworkPolicy.CALLS_ENABLED` | closed |

Gaps for a later pass: the upstream screens marked "on purpose" (look and Turkish wording), SimpleX wording in the system notification channels, the foreground service notification and the lock texts, viewing channels in the manifest ([BRANDING.md](BRANDING.md)).

## Language policy

- **Turkish first.** Turkish (`tr`) is the primary language of Veil. New Veil strings (`veil_` keys) are written in Turkish in `MR/tr`, and `MR/base` holds the English fallback, so a missing key never breaks the build. Keys that no Kotlin file uses are removed from both.
- `scripts/ci/strings-check.sh` allows no new "SimpleX" wording in the string files (a ratchet with the counts in `scripts/ci/strings-simplex-baseline.txt`), requires the same `veil_` keys in both files and blocks prohibited claims in them. Reducing the remaining upstream wording is tracked as the branding and language gate in [ROADMAP.md](ROADMAP.md).
- Documentation stays in English.
