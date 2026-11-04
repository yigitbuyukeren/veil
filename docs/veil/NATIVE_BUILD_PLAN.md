# Native core from source: options and decision

State: **BLOCKED, not built.** The app currently links `libsimplex.so`/`libsupport.so` taken from the official v7.0.3 release APK (checksum-verified, GPG not verified; see `ANDROID_BUILD.md`). Nothing in this document is a build result.

## What the pinned source offers

- `flake.nix` defines `aarch64-android:lib:simplex-chat` and `aarch64-android:lib:support` (haskell.nix cross build to Android, API 26). Upstream's own reproduction script (`scripts/simplex-chat-reproduce-builds-android.sh`) states the core build with Nix takes **12 to 24 hours**.
- The upstream release libraries come from their own CI (`scripts/android/download-libs.sh` fetches from a job repository), not from GitHub Actions workflows in this tree.

## Options

| Option | Cost | Assessment |
|---|---|---|
| Local Mac | none | Not possible: Nix cross build needs Linux x86_64; no Nix/Docker here and about 4 GB free disk. |
| GitHub-hosted runner (`workflow_dispatch`, free for public repos) | none | Job limit 6 h, 14 GB disk (about 60 GB after clearing preinstalled tools), 10 GB cache limit. Expected to hit the time limit on a cold build. Not added because an unrunnable workflow would only produce noise; a trial would need one full 6 h run to confirm. |
| Self-hosted runner on a Linux x86_64 machine the owner controls | owner's hardware | Works within upstream's stated 12-24 h; needs about 60 GB disk and 16 GB RAM. Recommended path if no paid service is wanted. |
| Rented Linux VM for one build | paid | Excluded by project rules without explicit approval. |

## Acceptance for replacing the prebuilt libraries

1. Build both Android libs from the pinned commit on Linux x86_64.
2. Compare against the official libs (same source, same flake lock): identical hash means reproducible, differing hash must be explained before shipping either.
3. Only then switch `scripts/ci/fetch-native-libs.sh` and mark VEIL-004's core criterion done; until then CI keeps stating "core not built from source".
4. Use the build's dependency closure to finish the Haskell/native license inventory (`UPSTREAM_LICENSES.md` row 6b).

Decision: wait for a Linux x86_64 machine. No workflow is added.

## Linux x86_64 preparation checklist (nothing here is run on the Mac)

These figures are planning estimates from upstream's statement and typical Nix/haskell.nix cross builds; they are not measurements from this project.

- Machine: Linux x86_64, 4+ cores (8 preferred), 16 GB RAM (32 GB safer for the final GHC link steps), 80 GB free disk for the Nix store, build outputs and logs, stable network. Time budget 12 to 24 h cold.
- Software: Nix with flakes enabled, `git`, `curl`, `gawk`, `jq`; optional Docker for upstream's reproduce script. No Android SDK is needed for the core libraries.
- Source: clone this repository at the commit being shipped; the flake pins upstream's inputs. Do not update `flake.lock` for the first reproduction.
- Build targets: `aarch64-android:lib:simplex-chat` and `aarch64-android:lib:support` from `flake.nix`.
- Record: commit, `flake.lock` hash, wall time, output sha256 of `libsimplex.so` and `libsupport.so`, and the sha256 of the official release libraries for comparison.
- Never run this on the Mac: it would exhaust the roughly 4-5 GB of free disk and the 16 GB of RAM shared with the emulators.
