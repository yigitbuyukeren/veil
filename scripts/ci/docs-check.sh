#!/usr/bin/env bash
# Veil-owned documents: relative links resolve, prohibited privacy wording is absent.
set -u
cd "$(git rev-parse --show-toplevel)"
fail=0
docs="README.md ROADMAP.md SECURITY.md SOURCES.md docs/THREAT_MODEL.md docs/PRIVACY_CLAIMS.md docs/UPSTREAM.md docs/UPSTREAM_LICENSES.md docs/RELAY_USAGE.md docs/ANDROID_BUILD.md docs/BRANDING.md docs/TOR_ONLY.md"
for f in $docs; do
  [ -f "$f" ] || { echo "DOCS FAIL: missing $f"; fail=1; continue; }
  dir=$(dirname "$f")
  for l in $(grep -oE '\]\([^)#]+(#[^)]*)?\)' "$f" | sed -E 's/^\]\(//; s/\)$//; s/#.*//' | grep -vE '^(https?:|mailto:)' ); do
    [ -e "$dir/$l" ] || { echo "DOCS FAIL: $f links to missing $l"; fail=1; }
  done
done
# prohibited claims (the policy documents quote them on purpose)
banned='100% anonymous|completely anonymous|untraceable|unhackable|military-grade|guaranteed (private|secure|anonym)'
for f in README.md ROADMAP.md SECURITY.md docs/UPSTREAM.md docs/RELAY_USAGE.md docs/ANDROID_BUILD.md; do
  if grep -niE "$banned" "$f" | grep -viE 'not (a guarantee of )?"?100%|never say|prohibited|must not|not "?100%' ; then
    echo "DOCS FAIL: prohibited claim in $f"; fail=1
  fi
done
grep -q 'GNU AFFERO GENERAL PUBLIC LICENSE' LICENSE || { echo "LICENSE FAIL: AGPL text not found"; fail=1; }
[ $fail -eq 0 ] && echo "docs check: PASS"
exit $fail
