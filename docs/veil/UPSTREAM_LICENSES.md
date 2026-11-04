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
| 6 | Third-party dependency licenses, Android Kotlin/Gradle runtime classpath (273 artifacts) | Inventoried 2026-11-04 with `scripts/licenses/inventory.py`: all 273 declare a license; Apache-2.0 (about 266), MIT (6), BSD-3-Clause (1), **LGPL-2.1 (1: `net.sf.trove4j:trove4j`, transitive of BoofCV)**. No GPL-incompatible license found. LGPL-2.1 allows conversion to GPL terms (its section 3), so combining with AGPL-3.0 is acceptable; keep it a separate replaceable library and keep its notice | `THIRD_PARTY_NOTICES.md` | 2026-11-04 |
| 6b | Haskell core dependencies and native libraries (`libsimplex.so`, `libsupport.so`) | **Inventoried by approximation, 2026-11-04** with `scripts/licenses/haskell_inventory.py` (no build, no solver, no binary inspection): 160 packages (45 direct) walked from `simplex-chat.cabal`, `cabal.project` and upstream's `flake.nix` flags. Declared licenses: BSD-3-Clause 126, MIT 17, BSD-2-Clause 12, AGPL-3.0 2 (`simplex-chat` itself and `simplexmq` at the pinned tag, license text verified), ISC 1, public domain 1 (`monad-loops`), **no license 1: `android-support`** (the glue code of `libsupport.so`; no `license:` field and no license file at the pinned tag, so no license grant could be verified). No GPL-2.0-only, BSD-4-Clause or proprietary license found. Native code outside Haskell, from the build files only: OpenSSL (static plus shipped `.so`), SQLCipher/SQLite (BSD-style / public domain), GMP (LGPL-3.0-or-later OR GPL-2.0-or-later, static), libffi (MIT), libiconv (LGPL-2.1-or-later, probably static), vendored `blst` and `libbbs` (Apache-2.0 per their license files). Versions are the newest satisfying the cabal constraints, not a build plan; GHC boot package versions and the OpenSSL/GMP/iconv versions are not known. `direct-sqlcipher` declares BSD-3-Clause but its license file reads as MIT (both compatible) | `THIRD_PARTY_NOTICES.md` (last section) | 2026-11-04 |
| 7 | Third-party notices file | `docs/THIRD_PARTY_NOTICES.md` generated (Gradle part, plus an approximate Haskell/native section); in-app licenses section done (Settings > Help & support > Tor and privacy status; links to the notices file, which is not bundled in the APK) | - | - |

## Rules for Veil

1. Fork/import upstream with history (VEIL-003); do not squash away authorship.
2. Keep upstream license text and file headers. Do not relicense.
3. Do not copy brand assets (logos, icons, wordmarks, store art). Veil uses its own name and assets. Replace upstream-branded resources before any build is distributed.
4. Make corresponding source available from within the app (link to this repository and the pinned upstream tag).
5. Generate third-party notices from the real dependency graph once VEIL-004 resolves it, then re-check each license for AGPL compatibility.
6. State "an app for using the SimpleX network" in the app and README (see `RELAY_USAGE.md`).

## Conflicts

No license found so far is incompatible with AGPL-3.0, by the methods used. Three things keep the review **incomplete** and any distributed build **NO-GO**: (1) `android-support` (linked into `libsupport.so`) has no license declared and no license file at the pinned tag; until its author or the upstream project confirms a license, it cannot be treated as AGPL-compatible. (2) The Haskell inventory (item 6b) is derived from cabal constraints, not from a build plan or from the shipped binaries; the real dependency set and versions can differ, `primitive` shows a constraint conflict the method cannot resolve, and the C libraries (OpenSSL, GMP, libiconv: versions and exact license variants) were taken from upstream's build files, not verified in the `.so` files. (3) LGPL parts (GMP, libiconv, trove4j) are compatible with AGPL-3.0 but carry notice and relink/replace duties that are not yet implemented for the native libraries. The in-app summary (`veil_licenses_text`) names Apache-2.0, MIT, BSD and LGPL-2.1; the Haskell part adds ISC, public domain and LGPL-3.0/GPL-2.0-or-later (GMP), which are not named there. They are all permissive or AGPL-compatible, and the string was not changed.
