# Use of SimpleX public relay infrastructure

Checked: 2026-11-04. Source: https://simplex.chat/privacy/ (section "Conditions of Use of Software and Infrastructure", third-party client applications). Terms can change; re-check before every release.

## Conditions for a third-party client (summary, not legal advice)

| Condition | Veil approach | Status |
|---|---|---|
| Protocol specs not older than 1 year | Track upstream stable; manual update procedure in `docs/veil/UPSTREAM.md` (VEIL-003) | Planned |
| User-to-user messaging only (or clearly labeled bots) | M1 is private 1:1 and small group messaging only; no bots | Compliant by scope |
| Same limits, rules and restrictions as official software | Do not alter client limits or relay rate/size handling | Must verify at VEIL-017 |
| Users accept the same Conditions of Use before access | First-run screen with link to the conditions, acceptance recorded locally; relays unreachable before acceptance | Planned (VEIL-009/016) |
| App states that it is an app for using the SimpleX network | Disclosure in README and in-app about screen | Planned (VEIL-016) |
| No SimpleX Chat Ltd branding without permission; no impersonation of official software | Own name and assets; no upstream logos | Planned |
| Source available in-app under an open-source license; AGPLv3 when using SimpleX code | AGPL-3.0; in-app source link | Planned (VEIL-016) |
| Ongoing compliance | Review on each upstream bump | Process |

## User-facing conditions to surface

- Minimum age: 13 (higher where local law requires).
- Legal use only; no spam or rights violations; no disrupting or scraping infrastructure.
- Preset servers may change or be discontinued at any time; updated conditions must be accepted within the stated window.

## Veil-specific rules

- Veil does not sell, resell or charge for access to the preset relays, and runs no paid server of its own in M1.
- Veil does not bypass relay quotas or rate limits.
- Veil uses only relays reachable via `.onion` addresses (Tor-only policy). If a preset relay set lacks onion endpoints, or the terms forbid the intended usage, that is a **NO-GO** until resolved by choosing different relays, such as user-supplied ones.
- No telemetry about relay use is collected by Veil.

## Open items

- Confirm exactly which preset relays publish `.onion` addresses in the pinned upstream version (VEIL-007).
- Wording of the conditions-acceptance screen must be reviewed against the then-current text.

No contradiction between the relay terms and the M1 scope has been found at the date above. This is a document review only; no relay was contacted.
