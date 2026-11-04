# Android FOSS debug build

Verified 2026-11-04 on macOS (Apple silicon), upstream base `v7.0.3` (see `UPSTREAM.md`).

## Pinned toolchain

| Component | Version |
|---|---|
| JDK | 17 (OpenJDK 17.0.19) |
| Gradle | 8.12 (wrapper) |
| Android SDK platform | 35 (compileSdk 35, target 35, min 26) |
| Build tools | 35.0.0 |
| NDK | 23.1.7779620 (required by `android/build.gradle.kts`) |
| ABIs | `arm64-v8a`, `armeabi-v7a` (default); `-Pveil.abis=arm64-v8a` limits the build |
| Flavor | `foss` (no Play Billing) |

## Steps

```bash
export ANDROID_HOME=/path/to/android-sdk
sdkmanager "platforms;android-35" "ndk;23.1.7779620" "build-tools;35.0.0"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > apps/multiplatform/local.properties   # ignored by git

# native libraries, see "Native core" below
cd apps/multiplatform
JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US" ./gradlew assembleFossDebug -Pveil.abis=arm64-v8a
```

Output: `apps/multiplatform/android/build/outputs/apk/foss/debug/android-foss-arm64-v8a-debug.apk` (not committed).

## Native core (Haskell, JNI)

The Android app links `libsimplex.so` (Haskell core) and `libsupport.so`. Upstream builds them with Nix on Linux (`scripts/android/build-android.sh`) or downloads CI builds (`scripts/android/download-libs.sh`, needs a job repo URL). Neither Nix, GHC nor Docker is present on this machine, and the CI job URL is not documented in the repository.

**Interim, used for this verification:** the two libraries were taken unmodified from the official upstream release `v7.0.3` asset `simplex-aarch64.apk` and placed in `apps/multiplatform/common/src/commonMain/cpp/android/libs/arm64-v8a/` (git-ignored via `*.so`).

- APK SHA-256 `d535e5a8986d4e11b9abd5656a3c0e1be5a08e06b00a7b20d6b7587ed0c3457f` matches the file in the official `v7.0.3` release. The release `_sha256sums` does not list the APK (only desktop and CLI artifacts), so the APK is covered by its own detached signature `_simplex-aarch64.apk.asc`.
- Signatures verified 2026-11-04: `_sha256sums.asc` and `_simplex-aarch64.apk.asc` are both **Good signature** from `SimpleX Build (Key for signing releases) <build@simplex.chat>`, primary key fingerprint `BBDF 7BDA D154 8B16 836A F5B9 D53B DFD1 53C3 66BA`. The fingerprint is documented in `docs/REPRODUCE.md`, where the key is also published on keys.openpgp.org; the public key is stored at `scripts/ci/keys/simplex-release.asc` and its fingerprint is checked on import. The key's user ID is not certified by a trusted signature on this machine, and the cross-check against the other sources listed in `docs/REPRODUCE.md` (ENS, Mastodon, Reddit) was not done.
- CI enforces this: `scripts/ci/fetch-native-libs.sh` verifies both signatures against the pinned fingerprint in a throwaway keyring and stops on a bad or missing signature before the APK is used.
- These binaries contain upstream's network stack, which has no Tor-only policy of its own. Veil's Tor-only behavior is enforced through app settings and tests (VEIL-007/008), and any change that needs core changes requires building the core from source. Reproducing the core from source on Linux/Nix is **not done**; release builds must not ship with these prebuilt libraries without that.

## Known pitfall: Turkish (tr) locale

Resource generation lowercases names with the default locale; with a Turkish locale `Inter-Italic.ttf` becomes `ıtalic` (dotless i) and `:common:compileDebugKotlinAndroid` fails with `Unresolved reference 'italic'`. Build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` and delete `apps/multiplatform/common/build/generated/moko` if it was generated under another locale.

## Verification results (2026-11-04)

| Check | Result |
|---|---|
| `./gradlew assembleFossDebug -Pveil.abis=arm64-v8a` | PASS (BUILD SUCCESSFUL, about 2 min after dependency download) |
| APK: package `io.github.yigitbuyukeren.veil` (see `BRANDING.md`), versionName 7.0.3, native-code `arm64-v8a`, targetSdk 35 | PASS (`aapt2 dump badging`) |
| FOSS flavor, no Play Billing | PASS: `billingclient` string occurrences in all dex files: 0 |
| `:android:testFossDebugUnitTest` | PASS (only the template `ExampleUnitTest` exists) |
| Lint | NOT RUN |
| `armeabi-v7a` build | NOT RUN (no armv7a native libs fetched) |
| Install/run on an emulator | PASS (see `EMULATOR_TESTS.md`); real device NOT TESTED |
| Reproducible build of the native core | NOT DONE, **BLOCKED**: upstream builds it with Nix (haskell.nix cross-compilation to Android) on x86_64 Linux through its Hydra cache; this Mac has no Nix, GHC or Docker and about 4 GB free disk, and the CI job URL is not in the repository. A GitHub-hosted Nix build was not attempted (multi-hour cross-compile without a known cache). Until it is done, release builds are NO-GO |

APK size 260 MB (debug, one ABI). The signing key and all outputs stay out of Git.

## Open points (not solved by this task)

- Branding is partly done; remaining items in `docs/BRANDING.md`.
- The manifest requests camera/microphone/foreground-service permissions and others that M1 may not need: review in VEIL-006/008.

Options and decision for building the native core from source: `NATIVE_BUILD_PLAN.md`.
