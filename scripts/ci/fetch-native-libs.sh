#!/usr/bin/env bash
# Interim: fetch upstream's official native libs for arm64-v8a from the pinned release APK, verify checksum.
# See docs/ANDROID_BUILD.md. Not a reproducible build of the core.
set -euo pipefail
TAG=v7.0.3
APK_SHA=d535e5a8986d4e11b9abd5656a3c0e1be5a08e06b00a7b20d6b7587ed0c3457f
tmp=$(mktemp -d)
gh release download "$TAG" -R simplex-chat/simplex-chat -p simplex-aarch64.apk -D "$tmp"
echo "$APK_SHA  $tmp/simplex-aarch64.apk" | shasum -a 256 -c -
dest=apps/multiplatform/common/src/commonMain/cpp/android/libs/arm64-v8a
mkdir -p "$dest"
unzip -joq "$tmp/simplex-aarch64.apk" lib/arm64-v8a/libsimplex.so lib/arm64-v8a/libsupport.so -d "$dest"
rm -rf "$tmp"
