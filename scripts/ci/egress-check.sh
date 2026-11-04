#!/usr/bin/env bash
# Inventory guard for VEIL-008: Android-relevant sources may only use network APIs in files listed in docs/NETWORK_EGRESS.md.
# A new use of an HTTP/socket/WebView API outside the allow-list fails CI until it is reviewed and documented.
set -u
cd "$(git rev-parse --show-toplevel)"
allow=scripts/ci/egress-allowlist.txt
pat='HttpURLConnection|\.openConnection\(|Jsoup\.connect|okhttp3|io\.ktor|HttpClient|java\.net\.Socket|DatagramSocket|InetAddress\.getBy|WebView\(|\.loadUrl\(|loadDataWithBaseURL|MediaPlayer\.setDataSource|ExoPlayer|ACTION_VIEW|openUriCatching|openExternalLink|PeerConnection|RTCIceServer|ImageRequest\.Builder|AsyncImage'
fail=0
while IFS= read -r f; do
  grep -qxF "$f" "$allow" || { echo "EGRESS FAIL: network API in unlisted file $f"; grep -nE "$pat" "$f" | head -3; fail=1; }
done < <(git grep -lE "$pat" -- 'apps/multiplatform/common/src/commonMain/*.kt' 'apps/multiplatform/common/src/androidMain/*.kt' 'apps/multiplatform/android/src/main/*.kt' 'apps/multiplatform/android/src/main/*.java' 'apps/multiplatform/android/src/main/AndroidManifest.xml' | sort)
[ $fail -eq 0 ] && echo "egress inventory check: PASS ($(wc -l < $allow | tr -d ' ') listed files)"
exit $fail
