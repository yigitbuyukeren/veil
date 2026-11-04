"""Shared adb/uiautomator helpers for the emulator scenarios (emulator evidence only)."""
import os, re, shutil, subprocess, sys, time, xml.etree.ElementTree as ET
ADB = shutil.which("adb") or os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb")
PKG = "io.github.yigitbuyukeren.veil"

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

def new_group_with_link(a, name, role=None):
    """On the chat list of `a`: create a group and open its group-link screen. Returns the link or None."""
    if not (tap(a, "Start new chat") and tap(a, "Create group", exact=True)): return None
    time.sleep(2); sh(a, "shell", "input", "tap", "400", "790"); sh(a, "shell", "input", "text", name); time.sleep(1)
    tap(a, "Create group", exact=True); time.sleep(2)
    sh(a, "shell", "input", "tap", "290", "1040")
    if not wait_text(a, "Share link"): return None
    if role:
        sh(a, "shell", "input", "tap", "846", "698"); time.sleep(1)
        if not tap(a, role, exact=True, timeout=8): return None
        time.sleep(1)
    tap(a, "Share link")
    end = time.time() + 30
    while time.time() < end:
        for t in texts(a):
            m = re.search(r"https://\S+/g#\S+", t)
            if m: return m.group(0)
        time.sleep(2)
    return None

class Report:
    def __init__(self): self.results = []
    def step(self, name, ok, info=""):
        self.results.append(ok); print(("PASS " if ok else "FAIL ") + name + (" " + str(info) if info else ""), flush=True); return ok
    def code(self): return 0 if self.results and all(self.results) else 1

def delete_group_link(a):
    """From the group-link creation screen: Continue -> group info -> Group link -> Delete link -> confirm."""
    sh(a, "shell", "input", "keyevent", "4"); time.sleep(2)
    if not tap(a, "Continue", timeout=10): return False
    time.sleep(3); sh(a, "shell", "input", "keyevent", "111"); sh(a, "shell", "input", "tap", "640", "2263"); time.sleep(3)
    if not tap(a, "Group link", exact=True, timeout=6):
        sh(a, "shell", "input", "swipe", "540", "1900", "540", "700")
        if not tap(a, "Group link", exact=True, timeout=6): return False
    time.sleep(2)
    return tap(a, "Delete link", timeout=10) and tap(a, "Delete", exact=True, timeout=10) and wait_text(a, "Create link", 20)
