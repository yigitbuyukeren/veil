#!/usr/bin/env bash
# String-resource audit: veil_ keys exist in base and tr, none empty, SimpleX wording count may not grow (ratchet), no prohibited claims in veil_ strings.
set -u
cd "$(git rev-parse --show-toplevel)"
fail=0
res=apps/multiplatform/common/src/commonMain/resources/MR
baseline=scripts/ci/strings-simplex-baseline.txt
# "name<TAB>value" for every single-line <string> entry
dump() { sed -nE 's/^[[:space:]]*<string name="([^"]+)"[^>]*>(.*)<\/string>[[:space:]]*$/\1\t\2/p' "$1"; }
dump $res/base/strings.xml > /tmp/.strings-base.$$
dump $res/tr/strings.xml > /tmp/.strings-tr.$$
trap 'rm -f /tmp/.strings-base.$$ /tmp/.strings-tr.$$' EXIT
bf=/tmp/.strings-base.$$
tf=/tmp/.strings-tr.$$
# 1. veil_ keys: same set in base and tr
bk=$(grep -E '^veil_' $bf | cut -f1 | sort)
tk=$(grep -E '^veil_' $tf | cut -f1 | sort)
for k in $(comm -23 <(echo "$bk") <(echo "$tk")); do echo "STRINGS FAIL: $k is in base but missing in tr"; fail=1; done
for k in $(comm -13 <(echo "$bk") <(echo "$tk")); do echo "STRINGS FAIL: $k is in tr but missing in base"; fail=1; done
[ -n "$bk" ] || { echo "STRINGS FAIL: no veil_ keys found in base"; fail=1; }
# 2. no empty veil_ values
for l in base tr; do
  f=/tmp/.strings-$l.$$
  while IFS=$'\t' read -r k v; do
    [ -n "${v//[[:space:]]/}" ] || { echo "STRINGS FAIL: $k is empty in $l"; fail=1; }
  done < <(grep -E '^veil_' "$f")
done
# 3. SimpleX occurrences in user-visible values may not exceed the baseline
# (raw file with attributes and comment lines removed, so multi-line values are counted too)
count() { sed -E 's/name="[^"]*"//g' "$1" | grep -vE '^[[:space:]]*<!--' | grep -o 'SimpleX' | wc -l | tr -d ' '; }
for l in base tr; do
  n=$(count $res/$l/strings.xml)
  max=$(sed -nE "s/^$l=([0-9]+)\$/\1/p" $baseline 2>/dev/null)
  if [ -z "$max" ]; then echo "STRINGS FAIL: no $l= entry in $baseline"; fail=1; continue; fi
  echo "SimpleX in $l: $n (baseline $max)"
  if [ "$n" -gt "$max" ]; then echo "STRINGS FAIL: SimpleX count in $l grew ($n > $max)"; fail=1
  elif [ "$n" -lt "$max" ]; then echo "hint: lower $l= to $n in $baseline"; fi
done
# 4. prohibited claims in veil_ strings
banned='100% anonymous|completely anonymous|untraceable|unhackable|military-grade|guaranteed (private|secure|anonym)|100% anonim|tamamen anonim|izlenemez|hacklenemez|tamamen güvenli|güvendesin'
for l in base tr; do
  if grep -E '^veil_' /tmp/.strings-$l.$$ | grep -iE "$banned"; then
    echo "STRINGS FAIL: prohibited claim in veil_ strings of $l"; fail=1
  fi
done
[ $fail -eq 0 ] && echo "strings check: PASS"
exit $fail
