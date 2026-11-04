#!/usr/bin/env python3
"""Background/doze delivery scenario (VEIL-013), emulator only. Usage: scenario_background.py <serialA> <serialB> <chat title on both>
A sends while B's app is in the background (HOME), then while B is in forced Doze. For each case the script records whether the
message text shows up in B's notifications while backgrounded, and whether it is present once B is brought back.
It cannot represent vendor battery policies, so a PASS here says nothing about real phones."""
import os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from emu import *
A, B, CHAT = sys.argv[1], sys.argv[2], sys.argv[3]
N = str(int(time.time()) % 100000)
r = Report()
def notif(d): return sh(d, "shell", "dumpsys notification --noredact")
for d in (A, B): sh(d, "shell", "am", "force-stop", PKG); sh(d, "shell", "am", "start", "-n", PKG + "/chat.simplex.app.MainActivity")
time.sleep(20); tap(A, CHAT, timeout=10); tap(B, CHAT, timeout=10); time.sleep(2)
def case(label, pre, post):
    msg = f"bg_{label}_{N}"
    pre(); time.sleep(5)
    ok = send(A, msg)
    seen = False; end = time.time() + 180
    while ok and time.time() < end and not seen:
        seen = msg in notif(B); time.sleep(10)
    post(); time.sleep(3)
    sh(B, "shell", "am", "start", "-n", PKG + "/chat.simplex.app.MainActivity"); time.sleep(5); tap(B, CHAT, timeout=10)
    got = wait_text(B, msg, 180)
    r.step(f"{label}: message present after returning", bool(ok and got))
    print(f"INFO {label}: shown as notification while backgrounded: {seen}", flush=True)
case("background", lambda: sh(B, "shell", "input", "keyevent", "3"), lambda: None)
case("doze", lambda: (sh(B, "shell", "input", "keyevent", "3"), sh(B, "shell", "dumpsys", "deviceidle", "force-idle")),
     lambda: sh(B, "shell", "dumpsys", "deviceidle", "unforce"))
sys.exit(r.code())
