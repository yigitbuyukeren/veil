#!/usr/bin/env bash
# Scans tracked files for secret-like content. Allow-list: scripts/ci/secret-allowlist.txt (one path prefix per line).
set -u
cd "$(git rev-parse --show-toplevel)"
allow=scripts/ci/secret-allowlist.txt
fail=0
report() { echo "SECRET-SCAN FAIL: $1"; fail=1; }
allowed() { local f="$1"; [ -f "$allow" ] || return 1; while read -r p; do case "$p" in ''|'#'*) continue;; esac; case "$f" in "$p"*) return 0;; esac; done < "$allow"; return 1; }

# 1. forbidden file names
while IFS= read -r f; do
  case "$f" in
    *.jks|*.keystore|*.p12|*.pfx|*.pem|*.key|*/.env|.env|*/.env.*|.env.*|keystore.properties|*/local.properties|local.properties)
      case "$f" in *.env.example|*.example) continue;; esac
      allowed "$f" || report "forbidden file tracked: $f";;
  esac
done < <(git ls-files)

# 2. forbidden content patterns
pat='-----BEGIN ([A-Z]+ )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{50,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|xox[baprs]-[0-9A-Za-z-]{10,}|sk-[A-Za-z0-9]{32,}'
while IFS= read -r f; do
  allowed "$f" || report "secret-like content in: $f"
done < <(git grep -lIE -e "$pat" -- . 2>/dev/null)

[ $fail -eq 0 ] && echo "secret scan: PASS ($(git ls-files | wc -l | tr -d ' ') files)"
exit $fail
