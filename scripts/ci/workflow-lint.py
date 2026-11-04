#!/usr/bin/env python3
"""Static policy check for .github/workflows: least privilege, no privileged triggers, pinned actions."""
import glob, re, sys
bad = 0
files = sorted(glob.glob(".github/workflows/*.yml") + glob.glob(".github/workflows/*.yaml"))
for f in files:
    s = open(f).read()
    def fail(m):
        global bad
        print(f"WORKFLOW FAIL: {f}: {m}"); bad = 1
    if re.search(r"^permissions:\s*\n\s+contents:\s*read\s*$", s, re.M) is None:
        fail("top-level `permissions: contents: read` required")
    if "pull_request_target" in s: fail("pull_request_target is not allowed")
    if re.search(r"\bsecrets\.", s): fail("secrets must not be used")
    if re.search(r"^\s*permissions:\s*write-all", s, re.M): fail("write-all permissions")
    for m in re.finditer(r"uses:\s*([^\s#]+)", s):
        ref = m.group(1)
        if ref.startswith("./"): continue
        if not re.search(r"@[0-9a-f]{40}$", ref): fail(f"action not pinned to a commit SHA: {ref}")
if not files:
    print("WORKFLOW FAIL: no workflows"); bad = 1
print("workflow lint: " + ("FAIL" if bad else f"PASS ({len(files)} workflows)"))
sys.exit(bad)
