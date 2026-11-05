# Privacy defaults (VEIL-009)

| Item | State |
|---|---|
| Account, phone, email, real name | Not requested. Onboarding asks only for a local display name (observed on emulator, 2026-11-04: "Get started" -> profile name -> create). PASS (observation) |
| Global user identifier | None created by the app; upstream design uses per-connection queues (upstream claim, not re-verified) |
| Incognito (per-connection random profile) | Default changed to **on** (`appPrefs.incognito`, `SimpleXAPI.kt`). Fresh installs use incognito for new links and connections; the user can switch it off per chat. Compiled and installed; the effective flag was observed on the joining side: the second emulator appeared as "Incognito" with a generated random name (`EMULATOR_TESTS.md` E2) and joined a group incognito (E11). Not observed on the inviting side's invitation screen (secure window, no readable indicator) |
| Notification preview | New installs: **hidden** (`notificationPreviewMode` = HIDDEN, notification shows only "new message"). It is stored when the Veil onboarding notification step is passed and the step explains it (`veil_onb_ntf_preview_note`); the upstream pref default (message) is unchanged, so installs that already exist keep their mode. The user can change it in Privacy. Per-chat "hide content in this chat's notifications" (device-local list `VeilNtfPrivacy`, never shows more than the contact name) and lock-screen visibility PRIVATE with a generic public version for message notifications. NOT TESTED on a device |
| Invitation creation | One-time link created through a relay over Tor on the emulator (PASS, once; relay reachability over Tor was intermittent, see `TOR_ONLY.md`) |
| Two invitations must not share a pseudonym | NOT TESTED |
| Invitation cancel / unwanted-invite blocking | The chat action sheet of a pending invitation you created shows "Daveti iptal et" (upstream's delete of the pending connection, which invalidates the link). NOT TESTED |
| Contact verification (security code) | Group-member level on two emulators: same code on both sides, mark/clear verified works (E12). Wrong-contact and key-change cases NOT TESTED; identity-difference explanations not changed |
| App lock (M3-2) | Off by default (upstream `performLA` = false). When turned on: system lock (Android) or Veil passcode, lock time default 30 s (upstream `laLockDelay`), choices right away, 30 s, 1 min, 5 min. NOT TESTED on a device |
| Self-destruct passcode (M3-2) | Off by default (upstream `selfDestruct` = false). Set only in Settings > Gelişmiş, behind a warning, and only with the Veil passcode lock. NOT TESTED |
| Screen protection (M3-2) | On by default (upstream `privacyProtectScreen`); turning it off asks first. NOT TESTED |
| Local file encryption (M3-4) | On by default (upstream `privacyEncryptLocalFiles`); the Veil switch cannot be turned off while on (it can be turned on if an imported setting left it off). Applies to files saved after it was on. NOT TESTED |
| Delete messages after (M3-4) | Off ("never") by default (upstream `chatItemTTL`); the choice is on Settings > "Veritabanı ve parola" (N6f). NOT TESTED |
| Backup (M3-3) | Not automatic and never sent anywhere by Veil; taken only by the owner, only with a passphrase the owner set. NOT TESTED, see [BACKUP_TESTS.md](BACKUP_TESTS.md) |

Acceptance status: "no registration required" observed; the remaining acceptance items are not met. Issue stays open.
