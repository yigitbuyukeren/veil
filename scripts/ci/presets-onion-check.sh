#!/usr/bin/env bash
# Tor-only policy guard: every preset SMP/XFTP server in the pinned core source must carry an .onion address.
set -u
cd "$(git rev-parse --show-toplevel)"
f=src/Simplex/Chat/Operators/Presets.hs
total=$(grep -oE '"(smp|xftp)://[^"]+"' $f | wc -l | tr -d ' ')
bad=$(grep -oE '"(smp|xftp)://[^"]+"' $f | grep -vc '\.onion"')
if [ "$bad" -ne 0 ] || [ "$total" -eq 0 ]; then
  echo "PRESETS FAIL: $bad of $total preset servers have no .onion address"; grep -oE '"(smp|xftp)://[^"]+"' $f | grep -v '\.onion"'; exit 1
fi
echo "presets onion check: PASS ($total preset servers, all with .onion)"
