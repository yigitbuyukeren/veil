# Upstream licenses and attribution map

Checked: 2026-11-04. Upstream: https://github.com/simplex-chat/simplex-chat (release `v7.0.3`, published 2026-09-26, branch `stable`).

## Findings

| # | Item | Finding | Source | Checked |
|---|---|---|---|---|
| 1 | Upstream code license | AGPL-3.0 (GitHub reports `AGPL-3.0`; `LICENSE` at repo root) | https://github.com/simplex-chat/simplex-chat/blob/v7.0.3/LICENSE | 2026-11-04 |
| 2 | Veil repository license | AGPL-3.0 (`LICENSE` in this repo), compatible with deriving from upstream | this repo | 2026-11-04 |
| 3 | Copyright notices | Upstream copyright and per-file headers must stay intact in any imported file; Veil additions carry their own notice | upstream sources | 2026-11-04 |
| 4 | Network-use clause | AGPL §13: users interacting with a modified version over a network must be offered the source. Veil is a client; the in-app link to source code (also required by relay terms) satisfies this and will be built in VEIL-016 | https://www.gnu.org/licenses/agpl-3.0.html | 2026-11-04 |
| 5 | Brand and logos | Upstream repo ships `media-logos/`, `images/`, `website/`. Brand/logos are not assumed to be covered by the code license; **Veil copies none of them** and must not use SimpleX Chat Ltd branding without permission | repo tree at `v7.0.3`; https://simplex.chat/privacy/ (third-party client rules) | 2026-11-04 |
| 6 | Third-party dependency licenses (Kotlin/Gradle, Haskell/Hackage, native libs) | **NOT YET INVENTORIED.** Needs a resolved dependency graph; requires the build from VEIL-004 | — | — |
| 7 | Third-party notices screen/file | Planned: `THIRD_PARTY_NOTICES` generated from the resolved dependency graph, plus an in-app licenses entry (VEIL-016) | — | — |

## Rules for Veil

1. Fork/import upstream with history (VEIL-003); do not squash away authorship.
2. Keep upstream license text and file headers. Do not relicense.
3. Do not copy brand assets (logos, icons, wordmarks, store art). Veil uses its own name and assets. Replace upstream-branded resources before any build is distributed.
4. Make corresponding source available from within the app (link to this repository and the pinned upstream tag).
5. Generate third-party notices from the real dependency graph once VEIL-004 resolves it, then re-check each license for AGPL compatibility.
6. State "an app for using the SimpleX network" in the app and README (see `RELAY_USAGE.md`).

## Conflicts

No conflicting license has been found so far. Item 6 is an open gap, not a pass: until the dependency inventory exists, the license review is **incomplete**, and any distributed build is **NO-GO** until it is done.
