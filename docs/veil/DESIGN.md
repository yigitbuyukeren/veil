# Veil design

How the Veil interface is built and how it should look and behave. Written for the whole-app redesign called "Tül" (Turkish for tulle, the sheer fabric of a veil). The redesign is tracked in [ROADMAP.md](ROADMAP.md); progress is in [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md). The ideas behind it are in [VISION.md](VISION.md). Tokens and signature elements below are the current proposal and may be refined as screens are built; nothing here claims a screen exists.

## Architecture rule: logic stays upstream, the interface lives in Veil

The whole interface is being rewritten, but taking updates from upstream must stay possible. Therefore:

- All new interface code lives in its own package, `chat.simplex.common.veil`:
  - `veil/design/` for design tokens, theme, fonts and components;
  - `veil/screens/<group>/` for the new screens;
  - `veil/data/` for small adapters the screens need (only `ChatController` / `ChatModel` calls).
- **New screens call existing logic; they do not copy it.** If behavior must change, it is done in a small adapter in `veil/data/`.
- The switch point is `App.kt` (`AppScreen`, `MainScreen` and the onboarding stages). A `VeilUi` flag controls the routing. Screens are replaced one by one and the app builds and runs after every step.
- Upstream screen files are not edited. They still compile but can no longer be reached. When an upstream update is merged, conflicts should be limited almost entirely to the routing in `App.kt`.
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
| **Hidden** | Calls, desktop, remote connection, device migration, terminal (debug builds only) |

## Language policy

- **Turkish first.** New Veil strings are written in Turkish. Until English is added, the base resource file (`MR/base`) also holds the Turkish text so that a missing key never breaks the build.
- The English translation comes later in the redesign; it is a small job.
- A CI check is planned for upstream strings that still say "SimpleX": CI turns red if untreated text remains in the Turkish and base files. This is tracked as the branding and language gate in [ROADMAP.md](ROADMAP.md).
- Documentation stays in English.
