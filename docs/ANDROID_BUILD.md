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

- APK SHA-256 `d535e5a8986d4e11b9abd5656a3c0e1be5a08e06b00a7b20d6b7587ed0c3457f` matches upstream `_sha256sums` for `v7.0.3`.
- The detached GPG signature (`_sha256sums.asc`) was **not** verified (no `gpg` installed).
- These binaries contain upstream's network stack, which has no Tor-only policy of its own. Veil's Tor-only behavior is enforced through app settings and tests (VEIL-007/008), and any change that needs core changes requires building the core from source. Reproducing the core from source on Linux/Nix is **not done**; release builds must not ship with these prebuilt libraries without that.

## Known pitfall: Turkish (tr) locale

Resource generation lowercases names with the default locale; with a Turkish locale `Inter-Italic.ttf` becomes `ıtalic` (dotless i) and `:common:compileDebugKotlinAndroid` fails with `Unresolved reference 'italic'`. Build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` and delete `apps/multiplatform/common/build/generated/moko` if it was generated under another locale.

## Verification results (2026-11-04)

| Check | Result |
|---|---|
| `./gradlew assembleFossDebug -Pveil.abis=arm64-v8a` | PASS (BUILD SUCCESSFUL, about 2 min after dependency download) |
| APK: package `chat.simplex.app`, versionName 7.0.3, native-code `arm64-v8a`, targetSdk 35 | PASS (`aapt2 dump badging`) |
| FOSS flavor, no Play Billing | PASS: `billingclient` string occurrences in all dex files: 0 |
| `:android:testFossDebugUnitTest` | PASS (only the template `ExampleUnitTest` exists) |
| Lint | NOT RUN |
| `armeabi-v7a` build | NOT RUN (no armv7a native libs fetched) |
| Install/run on a device or emulator | NOT TESTED |
| Reproducible build of the native core | NOT DONE, **BLOCKED**: upstream builds it with Nix (haskell.nix cross-compilation to Android) on x86_64 Linux through its Hydra cache; this Mac has no Nix, GHC or Docker and about 4 GB free disk, and the CI job URL is not in the repository. A GitHub-hosted Nix build was not attempted (multi-hour cross-compile without a known cache). Until it is done, release builds are NO-GO |

APK size 260 MB (debug, one ABI). The signing key and all outputs stay out of Git.

## Open points (not solved by this task)

- Package id is still upstream's `chat.simplex.app` and the app is still upstream-branded: VEIL-006.
- The manifest requests camera/microphone/foreground-service permissions and others that M1 may not need: review in VEIL-006/008.

Options and decision for building the native core from source: `NATIVE_BUILD_PLAN.md`.
