#!/usr/bin/env python3
"""Two-emulator group scenario: A creates a group and shares its link, B joins via the link, both exchange a message,
then each app uid's sockets are checked (only 127.0.0.1:9050 allowed).
Usage: scenario_group.py <serialA> <serialB> [groupname]. Both apps must be past onboarding and on the chat list,
Tor must be up with `adb reverse tcp:9050 tcp:9050`. Exit code 0 only if every step passed. Emulator evidence only."""
import os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from emu import *
A, B = sys.argv[1], sys.argv[2]
NAME = sys.argv[3] if len(sys.argv) > 3 else "scn" + str(int(time.time()) % 100000)
r = Report()
for d in (A, B):
    sh(d, "shell", "am", "force-stop", PKG); sh(d, "shell", "am", "start", "-n", PKG + "/chat.simplex.app.MainActivity")
time.sleep(15)
link = new_group_with_link(A, NAME)
r.step("A creates group and reads link from share sheet", bool(link), link and re.sub(r"#.*", "#...", link))
if link:
    sh(B, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", link, PKG)
    r.step("B shows open-group prompt", tap(B, "Open new group", timeout=60))
    time.sleep(3); sh(B, "shell", "input", "tap", "580", "2115")
    r.step("B joined (a member shows connected)", wait_text(B, " connected", 240))
    sh(A, "shell", "input", "keyevent", "4"); time.sleep(1); tap(A, "Continue", timeout=5)
    r.step("B -> A group message", send(B, "scn_b_" + NAME) and wait_text(A, "scn_b_" + NAME, 120))
    r.step("A -> B group message", send(A, "scn_a_" + NAME) and wait_text(B, "scn_a_" + NAME, 120))
for d in (A, B):
    ok_, info = sockets_ok(d); r.step("sockets only to 127.0.0.1:9050 on " + d, ok_, info if not ok_ else "")
sys.exit(r.code())
