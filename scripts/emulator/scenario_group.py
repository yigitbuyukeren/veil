#!/usr/bin/env python3
"""Two-emulator group scenario: A creates a group and shares its link, B joins via the link, both exchange a message,
then each app uid's sockets are checked (only 127.0.0.1:9050 allowed).
Usage: scenario_group.py <serialA> <serialB> [groupname]. Both apps must be past onboarding and on the chat list,
Tor must be up with `adb reverse tcp:9050 tcp:9050`. Exit code 0 only if every step passed. Emulator evidence only."""
import os, re, shutil, subprocess, sys, time, xml.etree.ElementTree as ET
ADB = shutil.which("adb") or os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb")
PKG = "io.github.yigitbuyukeren.veil"
A, B = sys.argv[1], sys.argv[2]
NAME = sys.argv[3] if len(sys.argv) > 3 else "scn" + str(int(time.time()) % 100000)

def sh(d, *a): return subprocess.run([ADB, "-s", d, *a], capture_output=True, text=True).stdout
def nodes(d):
    sh(d, "shell", "uiautomator", "dump", "/sdcard/u.xml")
    try: return list(ET.fromstring(sh(d, "shell", "cat", "/sdcard/u.xml")).iter("node"))
    except ET.ParseError: return []
def texts(d): return [(n.get("text") or n.get("content-desc") or "") for n in nodes(d)]
def center(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds"))); return (x1 + x2) // 2, (y1 + y2) // 2
def tap(d, want, exact=False, timeout=20):
    end = time.time() + timeout
    while time.time() < end:
        for n in nodes(d):
            t = (n.get("text") or n.get("content-desc") or "").lower()
            if (t == want.lower()) if exact else (want.lower() in t):
                x, y = center(n); sh(d, "shell", "input", "tap", str(x), str(y)); return True
        time.sleep(2)
    return False
def wait_text(d, want, timeout=90):
    end = time.time() + timeout
    while time.time() < end:
        if any(want in t for t in texts(d)): return True
        time.sleep(3)
    return False
def send(d, msg):
    sh(d, "shell", "input", "tap", "600", "2120"); sh(d, "shell", "input", "text", msg); time.sleep(1)
    return tap(d, "Send Message", timeout=10)
def sockets_ok(d):
    ps = sh(d, "shell", "ps -A -o USER,NAME").splitlines()
    user = next((l.split()[0] for l in ps if PKG in l), None)
    if not user: return False, "app not running"
    uid = 10000 + int(user.replace("u0_a", ""))
    bad = []
    for l in sh(d, "shell", "cat /proc/net/tcp /proc/net/tcp6").splitlines()[1:]:
        f = l.split()
        if len(f) > 7 and f[7] == str(uid) and f[3] == "01" and f[2] not in ("0100007F:235A",): bad.append(f[2])
    return not bad, bad
results = []
def step(name, ok, info=""):
    results.append(ok); print(("PASS " if ok else "FAIL ") + name + (" " + str(info) if info else ""), flush=True)
    return ok

for d in (A, B): sh(d, "shell", "am", "start", "-n", PKG + "/chat.simplex.app.MainActivity")
time.sleep(5)
ok = tap(A, "Start new chat") and tap(A, "Create group", exact=True)
step("A opens create-group", ok)
if ok:
    time.sleep(2); sh(A, "shell", "input", "tap", "400", "790"); sh(A, "shell", "input", "text", NAME); time.sleep(1)
    tap(A, "Create group", exact=True); time.sleep(2)
    # the app asks for incognito choice via a toggle; group link screen follows
    sh(A, "shell", "input", "tap", "290", "1040")
    step("A reaches group link screen", wait_text(A, "Share link"))
    tap(A, "Share link")
    link = None; end = time.time() + 30
    while time.time() < end and not link:
        for t in texts(A):
            m = re.search(r"https://\S+/g#\S+", t)
            if m: link = m.group(0)
        time.sleep(2)
    step("group link read from share sheet", bool(link), link and re.sub(r"#.*", "#...", link))
    if link:
        sh(B, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", link, PKG)
        step("B shows open-group prompt", tap(B, "Open new group", timeout=60))
        time.sleep(3); sh(B, "shell", "input", "tap", "580", "2115")
        step("B joined (chat composer visible)", wait_text(B, "Group", 120) and any("Message" == t for t in texts(B)))
        sh(A, "shell", "input", "keyevent", "4"); time.sleep(1); tap(A, "Continue", timeout=5)
        step("B -> A group message", send(B, "scn_b_" + NAME) and wait_text(A, "scn_b_" + NAME, 120))
        step("A -> B group message", send(A, "scn_a_" + NAME) and wait_text(B, "scn_a_" + NAME, 120))
for d in (A, B):
    ok_, info = sockets_ok(d); step("sockets only to 127.0.0.1:9050 on " + d, ok_, info if not ok_ else "")
sys.exit(0 if results and all(results) else 1)
