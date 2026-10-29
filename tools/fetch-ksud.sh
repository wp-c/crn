#!/bin/bash
# =============================================================================
#  tools/fetch-ksud.sh — ambil binary `ksud` (userspace KernelSU-Next)
#
#  Kenapa tidak disimpan di repo: ukurannya 5,5 MB dan itu binary pihak ketiga.
#  Yang disimpan hanya skrip ini; binary diambil saat build.
#
#  Pakai:  bash tools/fetch-ksud.sh [tujuan]     (default rollercoaster/anykernel/ksud)
#
#  Variabel:
#    KSUD_URL   URL langsung (kalau diisi, dipakai apa adanya)
#    KSUD_TAG   tag rilis, default: rilis terbaru
#
#  Verifikasi: berkas harus ELF aarch64 dan ukurannya > 1 MB.
# =============================================================================
set -euo pipefail

OUT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/rollercoaster/anykernel/ksud}"
REPO="${KSUD_REPO:-sidex15/KernelSU-Next}"
TAG="${KSUD_TAG:-}"

if [ -s "$OUT" ]; then
  echo "[ksud] sudah ada: $OUT ($(stat -c%s "$OUT") byte)"
  exit 0
fi

mkdir -p "$(dirname "$OUT")"

if [ -n "${KSUD_URL:-}" ]; then
  url="$KSUD_URL"
else
  api="https://api.github.com/repos/$REPO/releases"
  [ -n "$TAG" ] && api="$api/tags/$TAG"
  url="$(curl -fsSL "$api" \
    | grep -o 'https://[^"]*\/ksud[^"]*' | head -1)" || true
  [ -n "$url" ] || { echo "[ksud] GAGAL: tidak menemukan aset bernama 'ksud' di rilis $REPO"; exit 1; }
fi

echo "[ksud] unduh: $url"
curl -fsSL -o "$OUT.tmp" "$url"

size=$(stat -c%s "$OUT.tmp")
if [ "$size" -lt 1000000 ]; then
  echo "[ksud] GAGAL: ukuran $size byte (terlalu kecil / bukan binary)"; rm -f "$OUT.tmp"; exit 1
fi
if ! head -c4 "$OUT.tmp" | grep -q $'\x7fELF'; then
  echo "[ksud] GAGAL: bukan berkas ELF"; rm -f "$OUT.tmp"; exit 1
fi
if command -v file >/dev/null 2>&1; then
  file "$OUT.tmp" | grep -q 'ARM aarch64' || { echo "[ksud] GAGAL: bukan ELF aarch64"; rm -f "$OUT.tmp"; exit 1; }
fi
mv "$OUT.tmp" "$OUT"; chmod 0755 "$OUT"
echo "[ksud] OK: $OUT ($size byte)"
