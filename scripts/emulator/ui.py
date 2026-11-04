#!/usr/bin/env python3
"""adb UI helper for emulator tests. ui.py <serial> texts | tap "<text>" | type "<text>" | key <keycode> | dump
Needs ANDROID_HOME. Taps the first node whose text/content-desc contains the argument (case-insensitive)."""
import os, re, subprocess, sys, xml.etree.ElementTree as ET
ADB = os.path.join(os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools"), "platform-tools", "adb")
serial, cmd, *args = sys.argv[1:]
def sh(*a): return subprocess.run([ADB, "-s", serial, *a], capture_output=True, text=True).stdout
def nodes():
    sh("shell", "uiautomator", "dump", "/sdcard/u.xml")
    return list(ET.fromstring(sh("shell", "cat", "/sdcard/u.xml")).iter("node"))
if cmd in ("texts", "dump"):
    for n in nodes():
        t = (n.get("text") or "") + "|" + (n.get("content-desc") or "")
        if len(t) > 1: print(t[:140].replace("\n", " "), n.get("bounds"))
elif cmd == "tap":
    want = args[0].lower()
    for n in nodes():
        t = (n.get("text") or n.get("content-desc") or "").lower()
        if want in t:
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
            sh("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)); print("tapped", t[:60]); break
    else:
        print("NOT FOUND", want); sys.exit(1)
elif cmd == "type": sh("shell", "input", "text", args[0].replace(" ", "%s"))
elif cmd == "key": sh("shell", "input", "keyevent", args[0])
