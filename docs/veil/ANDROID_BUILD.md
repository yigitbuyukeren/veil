# Android FOSS debug build

Verified 2026-11-04 on macOS (Apple silicon), upstream base `v7.0.3` (see [UPSTREAM.md](UPSTREAM.md)).

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

The Android app links `libsimplex.so` (Haskell core) and `libsupport.so`. Veil does **not** build them from source: it ships the official prebuilt libraries from the upstream `v7.0.3` release, verified by SHA-256 and GPG signatures. The decision and its consequences are in [NATIVE_CORE.md](NATIVE_CORE.md).

**How it works:** the two libraries are taken unmodified from the official upstream release `v7.0.3` asset `simplex-aarch64.apk` and placed in `apps/multiplatform/common/src/commonMain/cpp/android/libs/arm64-v8a/` (git-ignored via `*.so`).

- APK SHA-256 `d535e5a8986d4e11b9abd5656a3c0e1be5a08e06b00a7b20d6b7587ed0c3457f` matches the file in the official `v7.0.3` release. The release `_sha256sums` does not list the APK (only desktop and CLI artifacts), so the APK is covered by its own detached signature `_simplex-aarch64.apk.asc`.
- Signatures verified 2026-11-04: `_sha256sums.asc` and `_simplex-aarch64.apk.asc` are both **Good signature** from `SimpleX Build (Key for signing releases) <build@simplex.chat>`, primary key fingerprint `BBDF 7BDA D154 8B16 836A F5B9 D53B DFD1 53C3 66BA`. The fingerprint is documented in upstream's `docs/REPRODUCE.md`, where the key is also published on keys.openpgp.org; the public key is stored at `scripts/ci/keys/simplex-release.asc` and its fingerprint is checked on import. The key's user ID is not certified by a trusted signature on this machine, and the cross-check against the other sources listed in `docs/REPRODUCE.md` (ENS, Mastodon, Reddit) was not done.
- CI enforces this: `scripts/ci/fetch-native-libs.sh` verifies both signatures against the pinned fingerprint in a throwaway keyring and stops on a bad or missing signature before the APK is used.
- These binaries contain upstream's network stack, which has no Tor-only policy of its own. Veil's Tor-only behavior is enforced in the app layer ([TOR_ONLY.md](TOR_ONLY.md)). Changes that would need core changes are not done. The core is not reproduced from source and that is not a release gate; the open license gaps for these binaries remain release blockers (see "Conflicts" in [UPSTREAM_LICENSES.md](UPSTREAM_LICENSES.md)).

## Known pitfall: Turkish (tr) locale

Resource generation lowercases names with the default locale; with a Turkish locale `Inter-Italic.ttf` becomes `ıtalic` (dotless i) and `:common:compileDebugKotlinAndroid` fails with `Unresolved reference 'italic'`. Build with `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` and delete `apps/multiplatform/common/build/generated/moko` if it was generated under another locale.

## Verification results (2026-11-04)

| Check | Result |
|---|---|
| `./gradlew assembleFossDebug -Pveil.abis=arm64-v8a` | PASS (BUILD SUCCESSFUL, about 2 min after dependency download) |
| APK: package `io.github.yigitbuyukeren.veil` (see [BRANDING.md](BRANDING.md)), versionName 7.0.3, native-code `arm64-v8a`, targetSdk 35 | PASS (`aapt2 dump badging`) |
| FOSS flavor, no Play Billing | PASS: `billingclient` string occurrences in all dex files: 0 |
| `:android:testFossDebugUnitTest` | PASS (only the template `ExampleUnitTest` exists) |
| Lint | NOT RUN |
| `armeabi-v7a` build | NOT RUN (no armv7a native libs fetched) |
| Install/run on an emulator | PASS (see [EMULATOR_TESTS.md](EMULATOR_TESTS.md)); real device NOT TESTED |
| Reproducible build of the native core | NOT PLANNED (decision 2026-11-04, [NATIVE_CORE.md](NATIVE_CORE.md)); not a release gate. Signature and hash verification of the prebuilt libraries: enforced in CI |

APK size 260 MB (debug, one ABI). The signing key and all outputs stay out of Git.

## Open points (not solved by this task)

- Branding is partly done; remaining items in [BRANDING.md](BRANDING.md).
- The manifest requests camera/microphone/foreground-service permissions and others that M1 may not need: review in VEIL-006/008.

Native core decision: [NATIVE_CORE.md](NATIVE_CORE.md). Current status of everything: [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md).
