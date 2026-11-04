# Privacy defaults (VEIL-009)

| Item | State |
|---|---|
| Account, phone, email, real name | Not requested. Onboarding asks only for a local display name (observed on emulator, 2026-11-04: "Get started" -> profile name -> create). PASS (observation) |
| Global user identifier | None created by the app; upstream design uses per-connection queues (upstream claim, not re-verified) |
| Incognito (per-connection random profile) | Default changed to **on** (`appPrefs.incognito`, `SimpleXAPI.kt`). Fresh installs use incognito for new links and connections; the user can switch it off per chat. Compiled and installed; the effective flag on a created connection was **not** observed in the UI (the invitation screen exposed no readable indicator through the accessibility dump; screenshots are blocked by the app's secure window) |
| Invitation creation | One-time link created through a relay over Tor on the emulator (PASS, once; relay reachability over Tor was intermittent, see `TOR_ONLY.md`) |
| Two invitations must not share a pseudonym | NOT TESTED |
| Invitation cancel / unwanted-invite blocking | NOT TESTED |
| Contact verification (security code) and identity-difference explanations | NOT TESTED / not changed |

Acceptance status: "no registration required" observed; the remaining acceptance items are not met. Issue stays open.
