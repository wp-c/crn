#!/usr/bin/env python3
# =============================================================================
#  tools/audit-config.py — audit defconfig fragment terhadap Kconfig TREE
#
#  Kenapa ini ada:
#    `make olddefconfig` MEMBUANG simbol yang tidak ada di tree dan simbol yang
#    dependensinya tidak terpenuhi -- diam-diam, tanpa peringatan. Akibatnya
#    profil bisa "berisi 100 baris" tetapi hanya 80 yang berefek. Skrip ini
#    membandingkan niat (berkas profil) dengan hasil (model Kconfig) SEBELUM
#    kernel dibangun, dan gagal (exit 1) kalau ada:
#       . simbol STALE   : tidak ada di tree (salah tulis / fitur sudah hilang)
#       . simbol INERT   : ada, tapi dependensi tidak terpenuhi -> nilai diabaikan
#
#  Dipakai:
#    SRC=/path/ke/tree python3 tools/audit-config.py 00-common.config chronicle.config
#    SRC=... python3 tools/audit-config.py --base arch/arm64/configs/vendor/xiaomi/miatoll_defconfig ...
#
#  Butuh: pip install kconfiglib   (tidak butuh toolchain / cross compiler)
# =============================================================================
import argparse
import os
import re
import sys
import tempfile

VAL_RE = re.compile(r"^(CONFIG_[A-Za-z0-9_]+)=(.*)$")
UNSET_RE = re.compile(r"^# (CONFIG_[A-Za-z0-9_]+) is not set$")
DEFAULT_BASE = "arch/arm64/configs/vendor/xiaomi/miatoll_defconfig"


def parse_fragments(paths):
    """Niat akhir per simbol: yang terakhir menang (sama seperti concat defconfig)."""
    wanted = {}
    for path in paths:
        with open(path, encoding="utf-8", errors="replace") as fh:
            for lineno, raw in enumerate(fh, 1):
                line = raw.strip()
                m = VAL_RE.match(line)
                if m:
                    wanted[m.group(1)[7:]] = (m.group(2).strip('"'), path, lineno)
                    continue
                m = UNSET_RE.match(line)
                if m:
                    wanted[m.group(1)[7:]] = ("n", path, lineno)
    return wanted


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("fragments", nargs="+")
    ap.add_argument("--base", default=DEFAULT_BASE)
    ap.add_argument("--src", default=os.environ.get("SRC", "."))
    ap.add_argument("--quiet", action="store_true")
    args = ap.parse_args()

    src = os.path.abspath(args.src)
    os.environ.update(srctree=src, ARCH="arm64", SRCARCH="arm64", CC=os.environ.get("CC", "clang"))
    if "KERNELVERSION" not in os.environ:
        mk = os.path.join(src, "Makefile")
        if os.path.exists(mk):
            v = {}
            for line in open(mk, errors="replace"):
                m = re.match(r"^(VERSION|PATCHLEVEL|SUBLEVEL)\s*=\s*(\S+)", line)
                if m:
                    v[m.group(1)] = m.group(2)
            os.environ["KERNELVERSION"] = ".".join(v.get(k, "0") for k in ("VERSION", "PATCHLEVEL", "SUBLEVEL"))

    import kconfiglib as K

    base = os.path.join(src, args.base)
    if not os.path.exists(base):
        print(f"FATAL: defconfig basis tidak ada: {base}", file=sys.stderr)
        return 2

    merged = tempfile.NamedTemporaryFile("w", suffix=".config", delete=False)
    with open(base, errors="replace") as fh:
        merged.write(fh.read())
    for frag in args.fragments:
        with open(frag, errors="replace") as fh:
            merged.write("\n" + fh.read())
    merged.close()

    kconf = K.Kconfig(os.path.join(src, "Kconfig"), warn_to_stderr=False)
    kconf.load_config(merged.name)
    os.unlink(merged.name)

    stale, inert = [], []
    for sym, (want, path, lineno) in sorted(parse_fragments(args.fragments).items()):
        node = kconf.syms.get(sym)
        if node is None:
            stale.append((sym, want, path, lineno))
            continue
        got = node.str_value
        if want == got:
            continue
        inert.append((sym, want, got, path, lineno, node))

    total = len(parse_fragments(args.fragments))
    label = ", ".join(os.path.basename(p) for p in args.fragments)
    print(f"== {label}")
    print(f"   {total} simbol diminta · {total - len(stale) - len(inert)} efektif · "
          f"{len(stale)} stale · {len(inert)} inert")
    for sym, want, path, lineno in stale:
        print(f"   STALE  {sym}={want}  ({os.path.basename(path)}:{lineno})")
        print(f"          simbol ini tidak ada di tree -> olddefconfig membuangnya diam-diam")
    for sym, want, got, path, lineno, node in inert:
        print(f"   INERT  {sym}: minta={want} hasil={got}  ({os.path.basename(path)}:{lineno})")
        dep = K.expr_str(node.direct_dep) if node.direct_dep is not None else ""
        if dep and dep != "y":
            print(f"          depends on: {dep}")
        if node.visibility == 0 and dep in ("", "y"):
            print(f"          simbol tidak terlihat (tidak punya prompt / di-select simbol lain)")
    if stale or inert:
        print("   HASIL: GAGAL")
        return 1
    print("   HASIL: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
