# Native core: decision

**Decision (2026-11-04): Veil does not build the native core from source.** The app ships the official prebuilt `libsimplex.so` (Haskell core) and `libsupport.so` from the SimpleX Chat `v7.0.3` release. A from-source build is not planned.

This replaces the earlier plan to build the core with Nix on Linux and compare hashes. That plan, its option table and its Linux preparation checklist are no longer maintained; they remain in Git history.

## Why

- **Cost.** Upstream states the Nix cross build takes 12 to 24 hours and needs a Linux x86_64 machine with tens of gigabytes of disk and 16 GB or more of RAM. Free hosted runners are not expected to finish it, and Veil has no such hardware or budget.
- **Little to gain.** Veil does not change the core. The cryptography, the protocol and the Haskell code stay with upstream by design ([VISION.md](VISION.md)). A self-built core would be the same source built by a less practiced pipeline.
- **Authenticity is checkable without a build.** The release files are signed by the upstream release key, and Veil pins the expected hash.
- **Scope discipline.** Any feature that would need a core change is not done. It is solved in the app layer or left out.

## How the prebuilt libraries are verified

`scripts/ci/fetch-native-libs.sh` (run by CI) downloads the `v7.0.3` release asset `simplex-aarch64.apk` and stops before using it unless:

1. the pinned public key (`scripts/ci/keys/simplex-release.asc`) has the expected primary fingerprint `BBDF 7BDA D154 8B16 836A F5B9 D53B DFD1 53C3 66BA`;
2. the detached signatures of `_sha256sums` and of the APK are good and were made by that key;
3. the APK SHA-256 matches the pinned value.

The two libraries are then extracted unmodified for `arm64-v8a`. Details, hashes and what was and was not checked by hand are in the "Native core" section of [ANDROID_BUILD.md](ANDROID_BUILD.md).

## Consequences

- **Reproducible builds are no longer a release gate.** Nobody can show that the shipped binary was built from the pinned source; trust rests on upstream's signature.
- **The core has no Tor-only policy of its own.** Veil enforces it in the app layer at the single choke point `apiSetNetworkConfig` ([TOR_ONLY.md](TOR_ONLY.md)). Behavior of the core for any network call not controlled by `NetCfg` is unknown ([NETWORK_EGRESS.md](NETWORK_EGRESS.md), "Not covered").
- **Core-created content stays in the core.** For example, the "Ask SimpleX Team" contact exists in the core database; Veil hides it in the interface ([BRANDING.md](BRANDING.md)).
- **License gaps remain release blockers.** Not building the core does not settle its licenses: `android-support` (linked into `libsupport.so`) has no license, the Haskell and native inventory is approximate, and LGPL notice duties are not implemented. They are tracked in [UPSTREAM_LICENSES.md](UPSTREAM_LICENSES.md) and keep the release decision at NO-GO ([M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md)).
- **Upgrading the core** means bumping the tag and hash in the fetch script after reading upstream's release notes and advisories, following [UPSTREAM.md](UPSTREAM.md).

## What would reopen this decision

A core change that cannot be done in the app layer and is judged necessary for the privacy claims, a discovered problem with upstream's release signing, or access to suitable Linux hardware at no extra cost. Any of these would need a new, dated decision here.
