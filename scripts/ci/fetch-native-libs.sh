#!/usr/bin/env bash
# Interim: fetch upstream's official native libs for arm64-v8a from the pinned release APK, verify checksum and GPG signatures.
# See docs/ANDROID_BUILD.md. Not a reproducible build of the core.
set -euo pipefail
TAG=v7.0.3
APK_SHA=d535e5a8986d4e11b9abd5656a3c0e1be5a08e06b00a7b20d6b7587ed0c3457f
# Upstream release signing key build@simplex.chat, documented in docs/REPRODUCE.md. Pinned by fingerprint.
KEY_FPR=BBDF7BDAD1548B16836AF5B9D53BDFD153C366BA
KEY_FILE=scripts/ci/keys/simplex-release.asc
tmp=$(mktemp -d)
gnupg="$tmp/gnupg"
mkdir -m 700 "$gnupg"
gh release download "$TAG" -R simplex-chat/simplex-chat -p simplex-aarch64.apk -p _sha256sums -p _sha256sums.asc -p _simplex-aarch64.apk.asc -D "$tmp"
# throwaway keyring: the pinned key must be the one whose primary fingerprint is KEY_FPR
gpg --homedir "$gnupg" --batch --import "$KEY_FILE"
got_fpr=$(gpg --homedir "$gnupg" --batch --with-colons --list-keys | awk -F: '$1=="fpr" {print $10; exit}')
[ "$got_fpr" = "$KEY_FPR" ] || { echo "unexpected signing key fingerprint: $got_fpr" >&2; exit 1; }
# a detached signature must be good and made by the pinned key, otherwise stop before using the APK
verify_sig() {
  local sig=$1 signed=$2 status
  status=$(gpg --homedir "$gnupg" --batch --status-fd 1 --verify "$sig" "$signed" 2>/dev/null) || { echo "bad or missing signature: $sig" >&2; exit 1; }
  grep -Eq "^\[GNUPG:\] GOODSIG " <<< "$status" || { echo "no good signature: $sig" >&2; exit 1; }
  grep -Eq "^\[GNUPG:\] VALIDSIG .* ${KEY_FPR}\$" <<< "$status" || { echo "signature not made by $KEY_FPR: $sig" >&2; exit 1; }
}
verify_sig "$tmp/_sha256sums.asc" "$tmp/_sha256sums"
# _sha256sums lists only desktop and CLI artifacts, so the APK is covered by its own detached signature
verify_sig "$tmp/_simplex-aarch64.apk.asc" "$tmp/simplex-aarch64.apk"
echo "$APK_SHA  $tmp/simplex-aarch64.apk" | shasum -a 256 -c -
dest=apps/multiplatform/common/src/commonMain/cpp/android/libs/arm64-v8a
mkdir -p "$dest"
unzip -joq "$tmp/simplex-aarch64.apk" lib/arm64-v8a/libsimplex.so lib/arm64-v8a/libsupport.so -d "$dest"
rm -rf "$tmp"
