#!/usr/bin/env bash
# Veil-owned documents: relative links resolve, prohibited privacy wording is absent.
set -u
cd "$(git rev-parse --show-toplevel)"
fail=0
docs="README.md SECURITY.md SOURCES.md docs/THIRD_PARTY_NOTICES.md docs/veil/THREAT_MODEL.md docs/veil/PRIVACY_CLAIMS.md docs/veil/UPSTREAM.md docs/veil/UPSTREAM_LICENSES.md docs/veil/RELAY_USAGE.md docs/veil/ANDROID_BUILD.md docs/veil/NATIVE_CORE.md docs/veil/BRANDING.md docs/veil/TOR_ONLY.md docs/veil/NETWORK_EGRESS.md docs/veil/PRIVACY_DEFAULTS.md docs/veil/DEVELOPMENT_STATUS.md docs/veil/M1_RELEASE_REPORT.md docs/veil/EMULATOR_TESTS.md docs/veil/ROADMAP.md docs/veil/VISION.md docs/veil/DESIGN.md"
for f in $docs; do
  [ -f "$f" ] || { echo "DOCS FAIL: missing $f"; fail=1; continue; }
  dir=$(dirname "$f")
  for l in $(grep -oE '\]\([^)#]+(#[^)]*)?\)' "$f" | sed -E 's/^\]\(//; s/\)$//; s/#.*//' | grep -vE '^(https?:|mailto:)' ); do
    [ -e "$dir/$l" ] || { echo "DOCS FAIL: $f links to missing $l"; fail=1; }
  done
done
# prohibited claims (the policy documents quote them on purpose)
banned='100% anonymous|completely anonymous|untraceable|unhackable|military-grade|guaranteed (private|secure|anonym)'
for f in README.md SECURITY.md docs/veil/ROADMAP.md docs/veil/VISION.md docs/veil/DESIGN.md docs/veil/UPSTREAM.md docs/veil/RELAY_USAGE.md docs/veil/ANDROID_BUILD.md; do
  if grep -niE "$banned" "$f" | grep -viE 'not (a guarantee of )?"?100%|never say|prohibited|must not|not "?100%' ; then
    echo "DOCS FAIL: prohibited claim in $f"; fail=1
  fi
done
grep -q 'GNU AFFERO GENERAL PUBLIC LICENSE' LICENSE || { echo "LICENSE FAIL: AGPL text not found"; fail=1; }
[ $fail -eq 0 ] && echo "docs check: PASS"
exit $fail
