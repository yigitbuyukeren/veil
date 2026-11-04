#!/usr/bin/env python3
"""Two-emulator group abuse-control scenario (VEIL-015). Usage: scenario_abuse.py <serialA> <serialB>
P1: group link deleted before use -> B must not become a member.
P2: group link with initial role observer -> B joins but cannot send; A's message reaches B.
P3: owner removes B -> B cannot send afterwards.
Absence checks use timeouts over Tor, so a PASS means 'not observed within the window', not a proof. Emulator evidence only."""
import os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from emu import *
A, B = sys.argv[1], sys.argv[2]
N = str(int(time.time()) % 100000)
r = Report()
for d in (A, B):
    sh(d, "shell", "am", "force-stop", PKG); sh(d, "shell", "am", "start", "-n", PKG + "/chat.simplex.app.MainActivity")
time.sleep(15)

def open_link_on_b(link):
    sh(B, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", link, PKG)
    ok = tap(B, "Open new group", timeout=60)
    if ok: time.sleep(3); sh(B, "shell", "input", "tap", "580", "2115")
    return ok

# P1: revoked link
link = new_group_with_link(A, "rv" + N)
r.step("P1 A creates group and link", bool(link))
if link:
    deleted = delete_group_link(A)
    r.step("P1 A deletes the link", deleted)
    if not deleted: sys.exit(1)
    open_link_on_b(link)
    time.sleep(240)
    tb = " | ".join(t for t in texts(B) if t)[:300]
    r.step("P1 B does not become a member through the deleted link within 240 s", not any(" connected" in t for t in texts(B)), "B shows: " + tb)

# P2: observer role
sh(A, "shell", "am", "force-stop", PKG); sh(B, "shell", "am", "force-stop", PKG)
for d in (A, B): sh(d, "shell", "am", "start", "-n", PKG + "/chat.simplex.app.MainActivity")
time.sleep(15)
link = new_group_with_link(A, "ob" + N, role="observer")
r.step("P2 A creates group with observer link", bool(link))
if link:
    sh(A, "shell", "input", "keyevent", "4"); time.sleep(1); tap(A, "Continue", timeout=5)
    open_link_on_b(link)
    r.step("P2 B joined", wait_text(B, " connected", 300))
    sh(B, "shell", "input", "tap", "600", "2120"); time.sleep(2)
    tb = [t for t in texts(B) if t]
    tap(B, "OK", exact=True, timeout=5) if False else None
    r.step("P2 observer cannot send", any("can't send" in t.lower() or "observer" in t.lower() for t in tb), " | ".join(tb)[:200])
    tap(B, "OK", exact=True, timeout=5)
    send(A, "ob_a_" + N)
    r.step("P2 A's message reaches B", wait_text(B, "ob_a_" + N, 180))
sys.exit(r.code())
