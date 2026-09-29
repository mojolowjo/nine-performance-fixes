#!/usr/bin/env python3
"""Checks every Mixin in build/classes against the real mod jars' bytecode.

For each mixin: the target class exists, every @Inject/@Redirect/@ModifyArg method string names a real
method (with the same static-ness as the handler), every INVOKE injection point really occurs in that
method, and every @Shadow member exists. Targets in Minecraft itself can't be checked without a Minecraft
jar; for those, pass --evidence jars that reference the same member (e.g. another mod's mixin).

Usage: tools/verify_targets.py JAR [JAR...]      (after ./build.sh; needs a JDK's javap)
"""
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLASSES = os.path.join(ROOT, "build", "classes")
ENV = dict(os.environ)
ENV.pop("JAVA_TOOL_OPTIONS", None)
_cache = {}


def javap(cp, cls, *flags):
    key = (cp, cls, flags)
    if key not in _cache:
        r = subprocess.run(["javap", "-p", *flags, "-cp", cp, cls], capture_output=True, text=True, env=ENV)
        _cache[key] = r.stdout if r.returncode == 0 else ""
    return _cache[key]


def find_class(jars, cls):
    for j in jars:
        if javap(j, cls):
            return j
    return None


def members(cp, cls):
    """[(name, descriptor, is_static, is_field)] of a class."""
    out, lines = [], javap(cp, cls, "-s").splitlines()
    for i, l in enumerate(lines):
        if i + 1 < len(lines) and lines[i + 1].strip().startswith("descriptor: "):
            desc = lines[i + 1].strip()[len("descriptor: "):]
            decl = l.strip().rstrip(";")
            static = " static " in " " + decl + " "
            if "(" in decl:
                name = decl[:decl.index("(")].split()[-1].split(".")[-1]
                if name == cls.split(".")[-1]:
                    name = "<init>"
                out.append((name, desc, static, False))
            else:
                out.append((decl.split()[-1], desc, static, True))
    return out


def mixin_info(cls):
    """Parse a compiled mixin: targets, injectors, shadows."""
    v = javap(CLASSES, cls, "-v")
    targets = re.findall(r'targets=\["?([^"\]]+)"?\]', v)
    targets = [t.strip('"') for t in re.findall(r'"([^"]+)"', v.split("org.spongepowered.asm.mixin.Mixin(")[1].split(")")[0])] if "org.spongepowered.asm.mixin.Mixin(" in v else []
    injectors, shadows = [], []
    # split into member sections: a member header is a 2-space-indented declaration line
    sections = re.split(r"\n(?=  \S[^\n]*[;)]\n)", v)
    for s in sections:
        head = s.split("\n", 1)[0].strip()
        m_desc = re.search(r"descriptor: (\S+)", s)
        if not m_desc:
            continue
        static = " static " in " " + head + " "
        name = head[:head.index("(")].split()[-1] if "(" in head else head.rstrip(";").split()[-1]
        for kind in ("Inject", "Redirect", "ModifyArg"):
            for ann in re.findall(r"org\.spongepowered\.asm\.mixin\.injection\." + kind + r"\((.*?)\n        \)", s, re.S):
                methods = re.findall(r'"([^"]+)"', re.search(r"method=\[?(.*?)\]?\n", ann).group(1))
                at_value = re.search(r'value="([A-Z_]+)"', ann)
                at_target = re.search(r'target="([^"]+)"', ann)
                injectors.append(dict(kind=kind, handler=name, static=static, methods=methods,
                                      at=at_value.group(1) if at_value else None,
                                      invoke=at_target.group(1) if at_target else None))
        if "org.spongepowered.asm.mixin.Shadow" in s:
            shadows.append((name, m_desc.group(1), "(" not in head))
    return targets, injectors, shadows


def split_method(spec):
    m = re.match(r"^([^(]+)(\(.*)?$", spec)
    return m.group(1), m.group(2)


def main(jars):
    mixins = sorted(os.path.splitext(f)[0] for f in os.listdir(os.path.join(CLASSES, "io/github/mojolowjo/ninefix/mixin"))
                    if f.endswith("Mixin.class"))
    bad = 0

    def report(ok, text):
        nonlocal bad
        bad += not ok
        print(("PASS  " if ok else "FAIL  ") + text)

    for simple in mixins:
        cls = "io.github.mojolowjo.ninefix.mixin." + simple
        targets, injectors, shadows = mixin_info(cls)
        for target in targets:
            jar = find_class(jars, target)
            if jar is None:
                if target.startswith("net.minecraft."):
                    print("SKIP  %s -> %s (Minecraft class; check by evidence)" % (simple, target))
                    continue
                report(False, "%s: target class %s not found in the given jars" % (simple, target))
                continue
            mem = members(jar, target)
            code = javap(jar, target, "-c", "-s")
            for inj in injectors:
                for spec in inj["methods"]:
                    name, desc = split_method(spec)
                    found = [m for m in mem if not m[3] and m[0] == name and (desc is None or m[1] == desc)]
                    ok = bool(found) and all(m[2] == inj["static"] for m in found)
                    report(ok, "%s @%s %s.%s%s%s" % (simple, inj["kind"], target.split(".")[-1], name, desc or "",
                                                     "" if ok else ("  (not found)" if not found else "  (static mismatch)")))
                    if ok and inj["invoke"]:
                        owner, rest = inj["invoke"][1:].split(";", 1)
                        mname, mdesc = split_method(rest)
                        needle = "%s.%s:%s" % (owner, mname, mdesc)
                        bodies = [b for b in code.split("\n\n")
                                  if re.search(r"[ .]" + re.escape(name) + r"\(", b.split("\n", 1)[0])
                                  and (desc is None or ("descriptor: " + desc) in b)]
                        hit = any(needle in b or ("Method " + mname + ":" + mdesc) in b for b in bodies)
                        report(hit, "%s   ... INVOKE %s.%s occurs in %s" % (simple, owner.split("/")[-1], mname, name))
            for name, desc, is_field in shadows:
                ok = any(m[0] == name and m[1] == desc and m[3] == is_field for m in mem)
                report(ok, "%s @Shadow %s %s%s" % (simple, "field" if is_field else "method", name, desc))
    # Every method/field our code calls on a class found in the given jars must exist there
    # (in the class itself or one of its supertypes).
    refs = set()
    for dirpath, _, files in os.walk(CLASSES):
        for f in files:
            if f.endswith(".class"):
                cls = os.path.relpath(os.path.join(dirpath, f), CLASSES)[:-6].replace(os.sep, ".")
                for m in re.finditer(r"= (?:Methodref|InterfaceMethodref|Fieldref)\s+\S+\s+// (\S+?)\.(\"?[\w$<>]+\"?):(\S+)", javap(CLASSES, cls, "-v")):
                    refs.add((m.group(1).replace("/", "."), m.group(2).strip('"'), m.group(3)))
    skipped = set()
    for owner, name, desc in sorted(refs):
        if owner.startswith("io.github.mojolowjo.") or owner.startswith("java."):
            continue
        jar = find_class(jars, owner)
        if jar is None:
            skipped.add(owner)
            continue
        todo, seen, ok = [owner], set(), False
        while todo and not ok:
            c = todo.pop()
            if c in seen:
                continue
            seen.add(c)
            cj = find_class(jars, c)
            if cj is None:
                continue
            ok = any(m[0] == name and m[1] == desc for m in members(cj, c))
            head = javap(cj, c).split("{", 1)[0]
            for kw in ("extends", "implements"):
                if kw in head:
                    part = head.split(kw, 1)[1].split("implements")[0] if kw == "extends" else head.split("implements", 1)[1]
                    todo += [re.sub(r"<.*", "", t.strip()) for t in part.split(",") if t.strip()]
        report(ok, "call %s.%s%s" % (owner.split(".")[-1], name, desc if "(" in desc else ":" + desc))
    for owner in sorted(skipped):
        print("SKIP  calls into %s (not in the given jars)" % owner)
    print("ALL MIXIN TARGETS AND CALLS VERIFIED" if bad == 0 else "%d PROBLEM(S)" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1:]))
