#!/usr/bin/env python3
"""Third-party license inventory for the Haskell core (libsimplex.so, libsupport.so).

Usage:
  python3 -I scripts/licenses/haskell_inventory.py                 # section to stdout
  python3 -I scripts/licenses/haskell_inventory.py --write         # replace/append the section in docs/THIRD_PARTY_NOTICES.md
  python3 -I scripts/licenses/haskell_inventory.py --json          # raw data as JSON (debugging)

Veil does not build the core. This script therefore does NOT use a cabal build plan. It approximates one:
  1. Reads the pinned sources in the repo root: simplex-chat.cabal (main library build-depends) and cabal.project
     (source-repository-package entries with git tag, constraints, flag settings).
  2. Fetches .cabal files from Hackage (or from GitHub at the pinned tag for source-repository-package entries)
     and walks build-depends of the main library recursively, evaluating `if` conditions for an Android/aarch64
     GHC 9.6.3 target with the flag overrides taken from flake.nix and cabal.project.
  3. Resolves each package to the highest non-deprecated Hackage version that satisfies every version range found
     in the walk (fixpoint, no real solver). `index-state` from cabal.project is NOT applied.
  4. Reads the license of every package from its .cabal `license:` field (source-repository packages: also the
     license file text at the pinned tag) and classifies it against AGPL-3.0 compatibility.
Python 3 standard library only. Needs network access (hackage.haskell.org, github.com, raw.githubusercontent.com).
Nothing is cached on disk. Set GITHUB_TOKEN to raise the GitHub API rate limit (only used as a fallback)."""
import sys, os, re, io, json, time, tarfile, collections, urllib.request, urllib.error
import concurrent.futures as cf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
NOTICES = os.path.join(ROOT, "docs", "THIRD_PARTY_NOTICES.md")
BEGIN, END = "<!-- haskell-core:begin -->", "<!-- haskell-core:end -->"
GHC_VERSION = (9, 6, 3)  # flake.nix: compiler-nix-name = "ghc963"
TARGET_OS = {"linux", "android"}
TARGET_ARCH = {"aarch64"}
# GHC 9.6.3 boot/global packages: shipped with the compiler, not resolved from Hackage by cabal.
# Their exact versions come from the compiler; license is read from the highest Hackage version matching the ranges.
BOOT = {"base", "ghc-prim", "ghc-bignum", "integer-gmp", "array", "binary", "bytestring", "containers", "deepseq",
        "directory", "exceptions", "filepath", "mtl", "parsec", "pretty", "process", "stm", "template-haskell",
        "text", "time", "transformers", "unix", "Cabal", "Cabal-syntax", "ghc-boot", "ghc-boot-th", "ghc-heap",
        "ghc", "ghci", "hpc", "haskeline", "terminfo", "xhtml", "Win32", "rts", "libiserv", "ghc-compact"}
# Flag overrides applied by upstream's Android build (flake.nix, aarch64-android:lib:simplex-chat) and cabal.project.
FLAG_OVERRIDES = {
    "simplex-chat": {"client_library": True},   # flake.nix: packages.simplex-chat.flags.client_library = true
    "simplexmq": {"client_library": True},      # flake.nix: packages.simplexmq.flags.client_library = true
    "direct-sqlcipher": {"openssl": True},      # flake.nix: packages.direct-sqlcipher.flags.openssl = true
    "text": {"simdutf": False},                 # flake.nix: packages.text.flags.simdutf = false
}
SRC_FILES = {  # repo name -> .cabal file name, when it differs from the repo name (otherwise discovered via GitHub API)
    "hs-socks": "socks.cabal", "haskell-terminal": "terminal.cabal",
}
UA = {"User-Agent": "veil-license-inventory/1.0"}

# ---------------------------------------------------------------- http
def http(url, accept=None, tries=4, binary=False):
    hdr = dict(UA)
    if accept: hdr["Accept"] = accept
    if "api.github.com" in url and os.environ.get("GITHUB_TOKEN"):
        hdr["Authorization"] = "Bearer " + os.environ["GITHUB_TOKEN"]
    last = None
    for i in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=hdr), timeout=40) as r:
                d = r.read()
                return d if binary else d.decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            if e.code in (404, 410): return None
            last = e
        except Exception as e:
            last = e
        time.sleep(1.5 * (i + 1))
    raise RuntimeError(f"fetch failed: {url}: {last}")

_cache = {}
def cached(key, fn):
    if key not in _cache: _cache[key] = fn()
    return _cache[key]

# ---------------------------------------------------------------- versions and ranges
def vparse(s):
    return tuple(int(x) for x in re.findall(r"\d+", s.split("-")[0]))
def vcmp(a, b):
    n = max(len(a), len(b)); a += (0,) * (n - len(a)); b += (0,) * (n - len(b))
    return (a > b) - (a < b)

def parse_range(text):
    """Cabal version range -> predicate over version tuples."""
    toks = re.findall(r"\|\||&&|\(|\)|\^>=|>=|<=|==|<|>|=|-any|-none|[0-9][0-9A-Za-z.\-]*\*?|any|none", text)
    pos = [0]
    def peek(): return toks[pos[0]] if pos[0] < len(toks) else None
    def eat(): pos[0] += 1; return toks[pos[0] - 1]
    def expr():
        l = term()
        while peek() == "||":
            eat(); r = term(); l = (lambda a, b: lambda v: a(v) or b(v))(l, r)
        return l
    def term():
        l = factor()
        while peek() == "&&":
            eat(); r = factor(); l = (lambda a, b: lambda v: a(v) and b(v))(l, r)
        return l
    def factor():
        t = peek()
        if t is None: return lambda v: True
        if t == "(":
            eat(); e = expr()
            if peek() == ")": eat()
            return e
        if t in ("-any", "any"): eat(); return lambda v: True
        if t in ("-none", "none"): eat(); return lambda v: False
        if t in ("==", ">=", "<=", "<", ">", "=", "^>="):
            op = eat(); ver = eat() if peek() else "0"
            star = ver.endswith("*"); vv = vparse(ver)
            if op in ("==", "=") and star:
                return lambda v: (v + (0,) * len(vv))[:len(vv)] == vv
            if op in ("==", "="): return lambda v: vcmp(v, vv) == 0
            if op == ">=": return lambda v: vcmp(v, vv) >= 0
            if op == "<=": return lambda v: vcmp(v, vv) <= 0
            if op == "<": return lambda v: vcmp(v, vv) < 0
            if op == ">": return lambda v: vcmp(v, vv) > 0
            up = (vv[0], (vv[1] if len(vv) > 1 else 0) + 1)  # ^>=
            return lambda v: vcmp(v, vv) >= 0 and vcmp(v, up) < 0
        eat(); return lambda v: True
    return expr()

# ---------------------------------------------------------------- cabal file parsing
FIELD = re.compile(r"^([A-Za-z0-9_\-.]+)\s*:\s*(.*)$")
IF = re.compile(r"^(if|elif)\b\s*(.*)$", re.I)
def _lines(text):
    out = []
    for raw in text.expandtabs(8).splitlines():
        s = raw.strip()
        if not s or s.startswith("--"): continue
        out.append((len(raw) - len(raw.lstrip()), s))
    return out

def _block(lines, i, indent):
    nodes = []
    while i < len(lines) and lines[i][0] >= indent:
        ind, s = lines[i]
        if ind > indent:  # stray over-indented line; skip
            i += 1; continue
        m = IF.match(s)
        if m and not FIELD.match(s):
            cond = m.group(2); i += 1
            j = i
            while j < len(lines) and lines[j][0] > ind: j += 1
            then = _block(lines[i:j], 0, lines[i][0]) if j > i else []
            i = j; els = []
            if i < len(lines) and lines[i][0] == ind and re.match(r"^else\b", lines[i][1], re.I):
                rest = lines[i][1][4:].strip()
                i += 1; j = i
                while j < len(lines) and lines[j][0] > ind: j += 1
                els = _block(lines[i:j], 0, lines[i][0]) if j > i else []
                i = j
            nodes.append(("if", cond, then, els)); continue
        m = FIELD.match(s)
        if m:
            val = [m.group(2)]; i += 1
            while i < len(lines) and lines[i][0] > ind:
                val.append(lines[i][1]); i += 1
            nodes.append(("field", m.group(1).lower(), "\n".join(val)))
            continue
        i += 1
    return nodes

def parse_cabal(text):
    lines = _lines(text)
    top, stanzas, i = {}, [], 0
    while i < len(lines):
        ind, s = lines[i]
        if ind != 0: i += 1; continue
        m = FIELD.match(s)
        if m:
            val = [m.group(2)]; i += 1
            while i < len(lines) and lines[i][0] > 0:
                val.append(lines[i][1]); i += 1
            top[m.group(1).lower()] = "\n".join(val)
        else:
            parts = s.split(None, 1); i += 1; j = i
            while j < len(lines) and lines[j][0] > 0: j += 1
            body = _block(lines[i:j], 0, lines[i][0]) if j > i else []
            stanzas.append((parts[0].lower(), parts[1].strip() if len(parts) > 1 else "", body)); i = j
    return top, stanzas

def eval_cond(cond, flags):
    toks = re.findall(r"&&|\|\||!|\(|\)|[A-Za-z_]+\s*\([^)]*\)|[A-Za-z_]+", cond)
    pos = [0]
    def peek(): return toks[pos[0]] if pos[0] < len(toks) else None
    def eat(): pos[0] += 1; return toks[pos[0] - 1]
    def orx():
        v = andx()
        while peek() == "||": eat(); r = andx(); v = v or r
        return v
    def andx():
        v = notx()
        while peek() == "&&": eat(); r = notx(); v = v and r
        return v
    def notx():
        if peek() == "!": eat(); return not notx()
        return atom()
    def atom():
        t = eat()
        if t == "(": v = orx(); eat() if peek() == ")" else None; return v
        if t.lower() == "true": return True
        if t.lower() == "false": return False
        m = re.match(r"([A-Za-z_]+)\s*\((.*)\)$", t)
        if not m: return True
        k, a = m.group(1).lower(), m.group(2).strip()
        if k == "os": return a.lower() in TARGET_OS
        if k == "arch": return a.lower() in TARGET_ARCH
        if k == "flag": return flags.get(a, False)
        if k == "impl":
            mm = re.match(r"ghc(?![\w\-])\s*(.*)$", a, re.I)
            if not mm: return False
            return parse_range(mm.group(1) or "-any")(GHC_VERSION)
        return True
    try: return orx()
    except Exception: return True

def collect(nodes, flags, commons, out):
    for n in nodes:
        if n[0] == "field":
            k, v = n[1], n[2]
            if k == "import":
                for c in re.split(r"[,\s]+", v):
                    if c in commons: collect(commons[c], flags, commons, out)
            else: out[k].append(v)
        else:
            collect(n[2] if eval_cond(n[1], flags) else n[3], flags, commons, out)

def library_info(cabal_text, pkg_overrides):
    """Return dict(license, version, deps[(name, rangetext)], native{...}) for the main library."""
    top, stanzas = parse_cabal(cabal_text)
    flags = {}
    for kw, arg, body in stanzas:
        if kw == "flag":
            d = next((n[2] for n in body if n[0] == "field" and n[1] == "default"), "true")
            flags[arg] = d.strip().lower() == "true"
    flags.update(pkg_overrides)
    commons = {a: b for kw, a, b in stanzas if kw == "common"}
    out = collections.defaultdict(list)
    sub = {arg: body for kw, arg, body in stanzas if kw == "library" and arg}
    for kw, arg, body in stanzas:
        if kw == "library" and arg == "": collect(body, flags, commons, out)
    def parse_deps(o):
        res = []
        for blk in o["build-depends"]:
            blk = re.sub(r":\{[^}]*\}", "", blk)
            blk = re.sub(r"(?<=[A-Za-z0-9]):[A-Za-z0-9_\-]+", "", blk)
            for item in blk.replace("\n", " ").split(","):
                m = re.match(r"^\s*([A-Za-z0-9][A-Za-z0-9_\-]*)\s*(.*)$", item)
                if m: res.append((m.group(1), m.group(2).strip()))
        return res
    deps, done = parse_deps(out), set()
    while True:  # unqualified references to internal sub-libraries: replace them by the sub-library's own dependencies
        inner = [d for d in deps if d[0] in sub and d[0] not in done]
        if not inner: break
        for n, _ in inner:
            done.add(n); so = collections.defaultdict(list)
            collect(sub[n], flags, commons, so)
            deps += parse_deps(so)
            for k in ("extra-libraries", "pkgconfig-depends", "frameworks", "c-sources"): out[k] += so[k]
    deps = [d for d in deps if d[0] not in sub]
    native = {}
    for key in ("extra-libraries", "pkgconfig-depends", "frameworks", "c-sources"):
        vals = [x for v in out[key] for x in re.split(r"[,\s]+", v.strip()) if x]
        if vals: native[key] = sorted(set(vals))
    return {"license": top.get("license", "").strip(), "license-file": top.get("license-file", "").strip(),
            "version": top.get("version", "").strip(), "name": top.get("name", "").strip(),
            "deps": deps, "native": native}

# ---------------------------------------------------------------- cabal.project
def parse_project(text):
    srcs, cons_flags, i = [], collections.defaultdict(dict), 0
    lines = [l for l in text.splitlines() if not l.strip().startswith("--")]
    blocks, cur = [], None
    for l in lines:
        if l and not l[0].isspace():
            cur = [l]; blocks.append(cur)
        elif cur is not None and l.strip(): cur.append(l)
    for b in blocks:
        head = b[0].strip()
        body = {}
        for l in b[1:]:
            m = re.match(r"\s*([\w\-]+)\s*:\s*(.*)$", l)
            if m: body[m.group(1)] = m.group(2).strip()
        if head.startswith("source-repository-package"):
            srcs.append(body)
        elif head.startswith("constraints"):
            for part in re.split(r",|\n", head.split(":", 1)[1] + " " + " ".join(b[1:])):
                m = re.match(r"\s*([A-Za-z0-9\-]+)((?:\s+[+-][\w\-]+)+)\s*$", part)
                if m:
                    for f in m.group(2).split(): cons_flags[m.group(1)][f[1:]] = f[0] == "+"
        elif head.startswith("package "):
            pkg = head.split(None, 1)[1].strip()
            for f in body.get("flags", "").split():
                if f[0] in "+-": cons_flags[pkg][f[1:]] = f[0] == "+"
    return srcs, cons_flags

# ---------------------------------------------------------------- license handling
NORM = {"BSD3": "BSD-3-Clause", "BSD2": "BSD-2-Clause", "BSD4": "BSD-4-Clause", "GPL-2": "GPL-2.0-only",
        "GPL-3": "GPL-3.0-only", "LGPL-2.1": "LGPL-2.1-only", "LGPL-3": "LGPL-3.0-only", "AGPL-3": "AGPL-3.0-only",
        "PublicDomain": "Public domain", "OtherLicense": "OtherLicense", "AllRightsReserved": "AllRightsReserved",
        "UnspecifiedLicense": "Unspecified", "": "Unspecified", "NONE": "Unspecified", "MIT": "MIT", "ISC": "ISC",
        "Apache-2.0": "Apache-2.0", "MPL-2.0": "MPL-2.0", "BSD-3-Clause": "BSD-3-Clause", "BSD-2-Clause": "BSD-2-Clause",
        "GPL": "GPL (version unspecified)", "LGPL": "LGPL (version unspecified)", "AGPL": "AGPL (version unspecified)",
        "Apache": "Apache (version unspecified)", "BSD": "BSD (unspecified)"}
OK_IDS = {"MIT", "ISC", "BSD-2-Clause", "BSD-3-Clause", "Apache-2.0", "MPL-2.0", "Public domain", "0BSD", "Unlicense",
          "CC0-1.0", "Zlib", "BSL-1.0", "X11", "AGPL-3.0-only", "AGPL-3.0-or-later", "GPL-3.0-only", "GPL-3.0-or-later",
          "GPL-2.0-or-later", "LGPL-2.1-only", "LGPL-2.1-or-later", "LGPL-3.0-only", "LGPL-3.0-or-later", "HPND", "PSF-2.0"}
BAD_IDS = {"GPL-2.0-only": "GPL-2.0-only is not compatible with AGPL-3.0",
           "BSD-4-Clause": "BSD-4-Clause advertising clause is not compatible with (A)GPL-3.0",
           "AllRightsReserved": "AllRightsReserved: no license granted"}

def norm_license(raw):
    """Return (display string, status, note). status: ok | review | incompatible."""
    raw = (raw or "").strip()
    def one(tok):
        tok = tok.strip().strip("()")
        t = NORM.get(tok, tok)
        if t.endswith("+") and t[:-1] + "-only" in OK_IDS | set(BAD_IDS): t = t[:-1] + "-or-later"
        return t
    if re.search(r"\bOR\b", raw):
        parts = [one(p) for p in re.split(r"\s+OR\s+", raw)]
        sts = [classify(p)[0] for p in parts]
        st = "ok" if "ok" in sts else ("review" if "review" in sts else "incompatible")
        return " OR ".join(parts), st, "SPDX expression; at least one option is compatible" if st == "ok" else ""
    if re.search(r"\bAND\b", raw):
        parts = [one(p) for p in re.split(r"\s+AND\s+", raw)]
        sts = [classify(p)[0] for p in parts]
        st = "incompatible" if "incompatible" in sts else ("review" if "review" in sts else "ok")
        return " AND ".join(parts), st, ""
    t = one(raw)
    st, note = classify(t)
    return t, st, note

def classify(t):
    if t in OK_IDS: return "ok", ""
    if t in BAD_IDS: return "incompatible", BAD_IDS[t]
    if t.startswith("Apache-2.0"): return "ok", ""
    return "review", "license not recognised/unspecified in the cabal file; read the package's license file"

def identify_text(txt):
    t = re.sub(r"\s+", " ", txt or "")
    if "GNU AFFERO GENERAL PUBLIC LICENSE" in t.upper() and "Version 3" in t: return "AGPL-3.0 (text)"
    if "GNU LESSER GENERAL PUBLIC LICENSE" in t.upper(): return "LGPL (text)"
    if "GNU GENERAL PUBLIC LICENSE" in t.upper(): return "GPL-" + ("3.0" if "Version 3" in t else "2.0") + " (text)"
    if "Apache License" in t and "Version 2.0" in t: return "Apache-2.0 (text)"
    if "Mozilla Public License" in t: return "MPL (text)"
    if "Permission is hereby granted, free of charge" in t: return "MIT (text)"
    if "Redistribution and use in source and binary forms" in t:
        if "advertising materials" in t.lower(): return "BSD-4-Clause (text)"
        return "BSD-3-Clause (text)" if "Neither the name" in t or "endorse or promote" in t else "BSD-2-Clause (text)"
    return "unrecognised text"

# ---------------------------------------------------------------- sources
class Src:
    def __init__(self, entry):
        loc = entry["location"].rstrip("/"); self.tag = entry["tag"]; self.sub = entry.get("subdir", "").strip("/")
        self.slug = re.sub(r"^https://github.com/|\.git$", "", loc)
        self.repo = self.slug.split("/")[1]
        self.url = f"https://github.com/{self.slug}"
    def raw(self, path):
        p = (self.sub + "/" if self.sub else "") + path
        return f"https://raw.githubusercontent.com/{self.slug}/{self.tag}/{p}"
    def web(self):
        return f"{self.url}/tree/{self.tag}" + (f"/{self.sub}" if self.sub else "")
    def cabal(self):
        guesses = [SRC_FILES.get(self.repo)] if self.repo in SRC_FILES else []
        guesses += [(self.sub or self.repo) + ".cabal", self.repo + ".cabal"]
        for g in guesses:
            t = http(self.raw(g))
            if t: return t
        api = f"https://api.github.com/repos/{self.slug}/git/trees/{self.tag}?recursive=1"
        tree = json.loads(http(api))["tree"]
        pre = self.sub + "/" if self.sub else ""
        for e in tree:
            if e["path"].startswith(pre) and e["path"].endswith(".cabal") and "/" not in e["path"][len(pre):]:
                return http(self.raw(e["path"][len(pre):]))
        raise RuntimeError("no cabal file for " + self.url)

def vendored_licenses(url, sub=""):
    """License-like files below the package top level inside a source tarball (vendored C code etc.)."""
    try: data = http(url, binary=True)
    except Exception as e: return [("(tarball not fetched: %s)" % e, "")]
    if data is None: return [("(tarball not found)", "")]
    res = []
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as tf:
        for m in tf.getmembers():
            if not m.isfile() or m.size > 300000: continue
            rel = m.name.split("/", 1)[1] if "/" in m.name else m.name
            if sub:
                if not rel.startswith(sub + "/"): continue
                rel = rel[len(sub) + 1:]
            base = os.path.basename(rel)
            if "/" in rel and re.match(r"(?i)^(licen[sc]e|copying|notice|unlicense|copyright)([._\-].*)?$", base):
                res.append((rel, identify_text(tf.extractfile(m).read().decode("utf-8", "replace"))))
    return sorted(res)[:25]

def submodule_licenses(sp):
    """Vendored code that lives in repository submodules is not in the archive; read its license from the pinned commit."""
    res = []
    try:
        tree = json.loads(http(f"https://api.github.com/repos/{sp.slug}/git/trees/{sp.tag}?recursive=1"))["tree"]
        gm = http(sp.raw(".gitmodules")) or ""
        urls = dict(re.findall(r"path\s*=\s*(\S+)\s*\n\s*url\s*=\s*(\S+)", gm))
        pre = sp.sub + "/" if sp.sub else ""
        for e in tree:
            if e["type"] != "commit" or not e["path"].startswith(pre): continue
            rel = e["path"][len(pre):]; url = urls.get(rel, "")
            m = re.match(r"https://github.com/([^/]+/[^/.]+)", url)
            lic = "?"
            if m:
                for name in ("LICENSE", "LICENSE.md", "LICENSE.txt", "COPYING"):
                    t = http(f"https://raw.githubusercontent.com/{m.group(1)}/{e['sha']}/{name}")
                    if t: lic = f"{name}: " + identify_text(t); break
                else: lic = "no license file at the pinned commit"
            res.append((f"{rel} (git submodule {url or 'url unknown'} @ {e['sha'][:12]})", lic))
    except Exception as ex:
        res.append(("(submodule check failed: %s)" % ex, ""))
    return res

# ---------------------------------------------------------------- resolver
def hackage_versions(name):
    def f():
        t = http(f"https://hackage.haskell.org/package/{name}/preferred", accept="application/json")
        if t is None: return None
        d = json.loads(t)
        vs = d.get("normal-version") or []
        if not vs:
            t2 = http(f"https://hackage.haskell.org/package/{name}", accept="application/json")
            vs = list(json.loads(t2).keys()) if t2 else []
        return sorted((vparse(v) for v in vs), key=lambda x: x)
    return cached(("vers", name), f)

def vstr(v): return ".".join(map(str, v))

def hackage_cabal(name, ver):
    return cached(("cabal", name, ver), lambda: http(f"https://hackage.haskell.org/package/{name}-{vstr(ver)}/{name}.cabal"))

def main():
    cabal_text = open(os.path.join(ROOT, "simplex-chat.cabal")).read()
    proj_text = open(os.path.join(ROOT, "cabal.project")).read()
    srcs_raw, proj_flags = parse_project(proj_text)
    srcs = [Src(e) for e in srcs_raw]
    overrides = collections.defaultdict(dict)
    for k, v in FLAG_OVERRIDES.items(): overrides[k].update(v)
    for k, v in proj_flags.items(): overrides[k].update(v)

    # source-repository-package entries: name -> record
    srcpk = {}
    def load_src(s):
        txt = s.cabal(); top, _ = parse_cabal(txt)
        name = top.get("name", s.repo).strip()
        info = library_info(txt, overrides.get(name, {}))
        lic_text = None
        lf = info["license-file"].split()[0] if info["license-file"] else ""
        if lf:
            lic_text = http(s.raw(lf)) or http(s.raw(lf.split("/")[-1]))
        return name, s, info, lic_text
    with cf.ThreadPoolExecutor(8) as ex:
        for name, s, info, lt in ex.map(load_src, srcs):
            srcpk[name] = {"src": s, "info": info, "lic_text": lt}

    root = library_info(cabal_text, overrides["simplex-chat"])
    root_name = "simplex-chat"
    roots = [(root_name, root)]
    if "android-support" in srcpk:  # libsupport.so
        roots.append(("android-support", srcpk["android-support"]["info"]))

    def candidates(name):
        if name in srcpk: return [vparse(srcpk[name]["info"]["version"])]
        return hackage_versions(name)

    def best(name, ranges):
        vs = candidates(name)
        if not vs: return None, False
        preds = [parse_range(r) for r in ranges]
        ok = [v for v in vs if all(p(v) for p in preds)]
        if ok: return max(ok), False
        scored = sorted(vs, key=lambda v: (sum(1 for p in preds if p(v)), v))  # conflict: satisfy as many as possible
        return scored[-1], True

    chosen, conflicts, unresolved = {}, {}, set()
    rounds = 0
    for rounds in range(1, 11):
        cons = collections.defaultdict(dict)
        infos, seen, level = {}, set(), []
        def push(deps, dependant):
            for n, r in deps:
                if n in ("simplex-chat",) or n == dependant: continue
                cons[n][dependant] = r or "-any"
                if n not in seen: seen.add(n); level.append(n)
        for rn, ri in roots: push(ri["deps"], rn)
        while level:
            cur, level[:] = list(level), []
            walk = [n for n in cur if n not in BOOT]
            with cf.ThreadPoolExecutor(12) as ex:  # prefetch version lists
                list(ex.map(candidates, walk))
            pick = {}
            for n in walk:
                v = chosen.get(n)
                if v is None: v, _ = best(n, list(cons[n].values()))
                if v is None: unresolved.add(n); continue
                pick[n] = v
            def fetch(n):
                if n in srcpk: return n, srcpk[n]["info"]
                t = hackage_cabal(n, pick[n])
                return n, (library_info(t, overrides.get(n, {})) if t else None)
            with cf.ThreadPoolExecutor(12) as ex:
                for n, info in ex.map(fetch, list(pick)):
                    if info is None: unresolved.add(n); continue
                    infos[n] = (pick[n], info); push(info["deps"], n)
        new, conflicts = {}, {}
        for n in seen:
            if n in BOOT:
                v, c = best(n, list(cons[n].values()))
                if v: new[n] = v
                continue
            if n in unresolved: continue
            v, c = best(n, list(cons[n].values()))
            new[n] = v
            if c: conflicts[n] = list(cons[n].items())
        converged = new == {k: chosen.get(k) for k in new} and set(chosen) == set(new)
        chosen = new
        if converged: break
    else:
        print("WARNING: resolution did not converge in 10 rounds", file=sys.stderr)

    # boot packages: need their license, from Hackage (cabal file of the highest matching version)
    rows = []
    for n in sorted(chosen, key=str.lower):
        v = chosen[n]
        if n in srcpk:
            sp = srcpk[n]; info = sp["info"]; s = sp["src"]
            lic, st, note = norm_license(info["license"])
            txt = identify_text(sp["lic_text"]) if sp["lic_text"] else "license file not found"
            fam = lambda x: re.split(r"[-\s(]", x)[0].upper()
            if sp["lic_text"] and fam(txt) != fam(lic) and "unrecognised" not in txt: txt += " (differs from cabal field)"
            rows.append(dict(name=n, version=info["version"] or vstr(v), license=lic, status=st, note=note,
                             kind="source", source=s.web(), text=txt, native=info["native"], cons=cons.get(n, {})))
        else:
            t = hackage_cabal(n, v)
            info = library_info(t, overrides.get(n, {})) if t else None
            if info is None:
                rows.append(dict(name=n, version=vstr(v), license="UNKNOWN", status="review", note="cabal file not fetched",
                                 kind="boot" if n in BOOT else "hackage", source=f"https://hackage.haskell.org/package/{n}",
                                 text="", native={}, cons=cons.get(n, {}))); continue
            lic, st, note = norm_license(info["license"])
            rows.append(dict(name=n, version=("GHC 9.6.3 boot" if n in BOOT else vstr(v)), license=lic, status=st, note=note,
                             kind="boot" if n in BOOT else "hackage",
                             source=f"https://hackage.haskell.org/package/{n}" + ("" if n in BOOT else f"-{vstr(v)}"),
                             text="", native=info["native"], cons=cons.get(n, {})))
    for r in rows:
        if r["kind"] == "source" and r["status"] == "review" and r["text"] == "license file not found":
            r["note"] = "no license declared in the cabal file and no license file at the pinned tag: no license grant could be verified"
    for rn, ri in roots:
        if rn in chosen: continue
        lic, st, note = norm_license(ri["license"])
        if rn in srcpk:
            sp = srcpk[rn]
            rows.append(dict(name=rn, version=ri["version"], license=lic, status=st, note=note, kind="source", source=sp["src"].web(),
                             text=identify_text(sp["lic_text"]) if sp["lic_text"] else "license file not found", native=ri["native"], cons={}))
        else:
            lf = os.path.join(ROOT, "LICENSE")
            rows.append(dict(name=rn, version=ri["version"], license=lic, status=st, note=note, kind="root",
                             source="this repository (`simplex-chat.cabal`, `LICENSE`)",
                             text=identify_text(open(lf).read()) if os.path.exists(lf) else "LICENSE not found", native=ri["native"], cons={}))
    for r in rows:
        if r["kind"] == "source" and r["status"] == "review" and r["text"] == "license file not found":
            r["note"] = "no license declared in the cabal file and no license file at the pinned tag: no license grant could be verified"
    def vend(r):
        if not r["native"].get("c-sources") or r["kind"] in ("boot", "root"): return r["name"], []
        if r["kind"] == "source":
            sp = srcpk[r["name"]]["src"]
            return r["name"], vendored_licenses(f"https://github.com/{sp.slug}/archive/{sp.tag}.tar.gz", sp.sub) + submodule_licenses(sp)
        n, v = r["name"], r["version"]
        return n, vendored_licenses(f"https://hackage.haskell.org/package/{n}-{v}/{n}-{v}.tar.gz")
    with cf.ThreadPoolExecutor(6) as ex:
        vmap = dict(ex.map(vend, rows))
    for r in rows: r["vendored"] = vmap.get(r["name"], [])
    for n in sorted(unresolved):
        rows.append(dict(name=n, version="?", license="UNRESOLVED", status="review", note="not found on Hackage and not a source-repository-package",
                         kind="hackage", source="", text="", native={}, cons=cons.get(n, {})))
    rows.sort(key=lambda r: r["name"].lower())
    unreached = sorted(n for n in srcpk if n not in {r["name"] for r in rows})
    data = dict(unreached=unreached, rows=rows, rounds=rounds, conflicts=conflicts, roots=[r[0] for r in roots],
                direct=sorted({d[0] for _, ri in roots for d in ri["deps"]}), src=srcpk, flags=dict(overrides),
                index_state=(re.search(r"index-state:\s*(\S+)", proj_text) or [None, "?"])[1])
    if "--json" in sys.argv:
        print(json.dumps({k: v for k, v in data.items() if k != "src"}, indent=1, default=str)); return
    md = render(data)
    if "--write" in sys.argv:
        cur = open(NOTICES).read()
        if BEGIN in cur and END in cur:
            cur = cur[:cur.index(BEGIN)].rstrip("\n") + "\n\n" + md + cur[cur.index(END) + len(END):].rstrip("\n") + "\n"
        else:
            cur = cur.rstrip("\n") + "\n\n" + md
        open(NOTICES, "w").write(cur)
    else:
        sys.stdout.write(md)

# ---------------------------------------------------------------- native libraries (from upstream build files)
NATIVE_LIBS = [
    ("OpenSSL (libcrypto, libssl)", "flake.nix `aarch64-android:lib:simplex-chat`: `androidPkgs.openssl` static for direct-sqlcipher and simplexmq; `openssl.out/lib/*.so` copied into the lib zip (shipped as separate .so files)",
     "Apache-2.0 for OpenSSL 3.x; OpenSSL license + SSLeay license for 1.1.1 and older", "version comes from the pinned nixpkgs; not determined without inspecting the binaries"),
    ("SQLCipher and SQLite (amalgamation inside `direct-sqlcipher`)", "cabal.project source-repository-package `direct-sqlcipher`; flake.nix sets `direct-sqlcipher.flags.openssl`",
     "SQLCipher: BSD-style license of Zetetic LLC; SQLite: public domain", "verify the license file in the direct-sqlcipher tag"),
    ("GMP (libgmp)", "flake.nix: `gmp6` with static libs (`ghc-bignum` GMP backend on Android)",
     "LGPL-3.0-or-later OR GPL-2.0-or-later", "statically linked into libsimplex.so per the flake; LGPL relinking/notice duties apply; compatible with AGPL-3.0"),
    ("libffi", "flake.nix: `androidFFI` and `-optl-lffi` in the libsimplex.so link line", "MIT", ""),
    ("GNU libiconv", "flake.nix: `androidIconv` (static, patched) defined for the Android build; GHC `base` needs an iconv implementation",
     "LGPL-2.1-or-later (library)", "likely statically linked; the flake lines that copy it are commented out, so not confirmed"),
    ("GHC runtime system (threaded RTS) and boot libraries", "flake.nix: `-threaded`, compiler ghc963; statically linked into libsimplex.so", "BSD-3-Clause", ""),
    ("Android NDK/bionic (libc, libm, libdl, liblog)", "dynamic system libraries; not redistributed in the APK", "n/a (platform)", ""),
    ("`android-support` C code (libsupport.so)", "cabal.project source-repository-package `android-support`", "see table: android-support", "built separately from libsimplex.so"),
]

def render(d):
    rows = d["rows"]
    by = collections.Counter(r["license"] for r in rows)
    L = []
    a = L.append
    a(BEGIN)
    a("## Haskell core (libsimplex.so, libsupport.so)")
    a("")
    a("Generated by `scripts/licenses/haskell_inventory.py` (`python3 -I scripts/licenses/haskell_inventory.py --write`). Do not edit by hand.")
    a("")
    a("**Method and limits (read this first).** Veil ships the prebuilt upstream core and does not build it, so there is no cabal build plan. "
      "This list is an approximation derived from the pinned sources: the `build-depends` of the main library in `simplex-chat.cabal` (flags as set in `flake.nix` for the Android build and in `cabal.project`: "
      + ", ".join(f"`{p}` {'+' if v else '-'}{f}" for p, fl in sorted(d["flags"].items()) for f, v in sorted(fl.items()))
      + "), plus `android-support` for libsupport.so, walked recursively through the `.cabal` files of each package. "
      "Versions are **resolved from cabal constraints**: the highest non-deprecated Hackage version satisfying every range found in the walk (a fixpoint, not a solver; "
      f"converged after {d['rounds']} round(s); `index-state` {d['index_state']} from `cabal.project` is **not** applied, so a version can be newer than the one upstream built). "
      "Packages from `source-repository-package` entries use the exact git tag pinned in `cabal.project`. "
      "Packages bundled with GHC 9.6.3 are marked † (their exact version is fixed by the compiler; the license was read from the newest matching Hackage release). "
      "`if` conditions were evaluated for Linux/Android, aarch64, GHC 9.6.3; packages needed only by tests, executables or setup scripts are not included. "
      "Licenses are the `license:` field of each `.cabal` file; they were not checked against the license files inside the packages except for source-repository packages (column 'Text'). "
      "The actual libraries were not inspected: the real set can differ, and this inventory is **not a substitute for scanning the shipped binaries**.")
    a("")
    direct = set(d["direct"])
    a(f"Packages: {len(rows)} (direct dependencies of simplex-chat / android-support: {len(direct & {r['name'] for r in rows})}); "
      f"source-repository-package: {sum(1 for r in rows if r['kind']=='source')}; GHC boot: {sum(1 for r in rows if r['kind']=='boot')}")
    a("")
    a("### License summary (Haskell)")
    a("")
    a("| License (from cabal file, normalised) | Packages | AGPL-3.0 compatibility |")
    a("|---|---|---|")
    stat = {r["license"]: r["status"] for r in rows}
    for lic, n in sorted(by.items(), key=lambda x: (-x[1], x[0])):
        s = {"ok": "compatible", "review": "**needs manual review**", "incompatible": "**NOT compatible**"}[stat[lic]]
        a(f"| {lic} | {n} | {s} |")
    a("")
    flagged = [r for r in rows if r["status"] != "ok"]
    if flagged:
        a("### Packages needing attention")
        a("")
        a("| Package | Version | License | Reason |")
        a("|---|---|---|---|")
        for r in flagged:
            a(f"| {r['name']} | {r['version']} | {r['license']} | {r['note'] or 'see license'} |")
        a("")
    else:
        a("No package with an incompatible, unknown or unspecified license was found by this method.")
        a("")
    if d.get("unreached"):
        a("Pinned in `cabal.project` but not reached from the main library under these flags (server-only or unused): " + ", ".join(f"`{n}`" for n in d["unreached"]) + ".")
        a("")
    if d["conflicts"]:
        a("Version-constraint conflicts: no single version satisfied every range found in the walk, so the version satisfying the most ranges was used (a real solver would also pick older versions of some dependants, which this method does not do): "
          + ", ".join(f"`{n}` (required by " + ", ".join(x[0] for x in d["conflicts"][n][:6]) + ")" for n in sorted(d["conflicts"])) + ". The license of the affected packages is not expected to change between versions, but that is not verified.")
        a("")
    a("### Packages")
    a("")
    a("† = bundled with GHC 9.6.3; * = source-repository-package (git tag pinned in `cabal.project`). 'Text' = what the license file at the tag looks like (source-repository packages only).")
    a("")
    a("| Package | Version | License | Text | Source |")
    a("|---|---|---|---|---|")
    for r in rows:
        mark = " †" if r["kind"] == "boot" else (" *" if r["kind"] == "source" else "")
        a(f"| {r['name']}{mark} | {r['version']} | {r['license']} | {r['text'] or '-'} | {r['source']} |")
    a("")
    nat = [r for r in rows if r["native"]]
    a("### Haskell packages declaring C sources or native libraries")
    a("")
    a("Taken from `c-sources`, `extra-libraries`, `pkgconfig-depends` and `frameworks` in the cabal files (main library, conditions applied). C code vendored inside these packages is covered by the package license field only; it was not audited separately.")
    a("")
    a("| Package | Declares |")
    a("|---|---|")
    for r in nat:
        a(f"| {r['name']} | " + "; ".join(f"{k}: {', '.join(v[:6])}{' ...' if len(v) > 6 else ''}" for k, v in r["native"].items()) + " |")
    a("")
    vend = [r for r in rows if r.get("vendored")]
    a("### Vendored license files inside packages with C sources")
    a("")
    a("License-like files (LICENSE, COPYING, NOTICE ...) found below the top level of each package's source archive (Hackage tarball, or the repository archive at the pinned tag). They show which third-party C code a package bundles under its own terms; identification is by phrase matching and must be confirmed by reading the files. Packages with C sources and no such file are not listed.")
    a("")
    a("| Package | File | Looks like |")
    a("|---|---|---|")
    for r in vend:
        for f, t in r["vendored"]: a(f"| {r['name']} | `{f}` | {t} |")
    a("")
    a("### Native libraries in the upstream build")
    a("")
    a("From upstream's `flake.nix` (Android targets) and `scripts/android/`; **not verified against the shipped `libsimplex.so`/`libsupport.so`**. Licenses listed are the upstream projects' licenses as generally known, not read from the binaries.")
    a("")
    a("| Library | Evidence in upstream build files | License | Note |")
    a("|---|---|---|---|")
    for n, ev, lic, note in NATIVE_LIBS:
        a(f"| {n} | {ev} | {lic} | {note} |")
    a("")
    a(END)
    return "\n".join(L) + "\n"

if __name__ == "__main__":
    main()
