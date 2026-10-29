#!/bin/bash
# =============================================================================
#  tools/fetch-ksud.sh — ambil binary `ksud` (userspace KernelSU-Next)
#
#  Kenapa tidak disimpan di repo: ukurannya ±5,5 MB dan itu binary pihak ketiga.
#  Yang disimpan hanya skrip ini; binary diambil saat build.
#
#  Pakai:  bash tools/fetch-ksud.sh [tujuan]
#          (default: rollercoaster/anykernel/ksud)
#
#  Variabel:
#    KSUD_REPO  default KernelSU-Next/KernelSU-Next
#    KSUD_TAG   tag rilis (mis. v3.4.2). Kosong = rilis terbaru.
#    KSUD_ASSET default ksud-aarch64-linux-android
#    KSUD_URL   URL langsung, kalau ingin memakai berkas lain
#
#  Versi kernel kita melaporkan KSU_VERSION=34000 (= KSU-Next v3.4.2),
#  jadi default terbaru cocok. Pin KSUD_TAG kalau ingin reproducible.
# =============================================================================
set -euo pipefail

OUT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/rollercoaster/anykernel/ksud}"
REPO="${KSUD_REPO:-KernelSU-Next/KernelSU-Next}"
TAG="${KSUD_TAG:-}"
ASSET="${KSUD_ASSET:-ksud-aarch64-linux-android}"

if [ -s "$OUT" ]; then
  echo "[ksud] sudah ada: $OUT ($(stat -c%s "$OUT") byte)"
  exit 0
fi

mkdir -p "$(dirname "$OUT")"

api="https://api.github.com/repos/$REPO/releases"
if [ -n "$TAG" ]; then
  api="$api/tags/$TAG"
  rel="$(curl -fsSL "$api")"
else
  rel="$(curl -fsSL "$api?per_page=1" | sed 's/^\[//; s/\]$//')"
  # ?per_page=1 mengembalikan array; ambil objek pertama apa adanya
  rel="$(curl -fsSL "$api?per_page=1")"
fi

url=""
if command -v jq >/dev/null 2>&1; then
  if [ -n "$TAG" ]; then
    url="$(printf '%s' "$rel" | jq -r --arg a "$ASSET" '.assets[]? | select(.name==$a) | .browser_download_url' | head -1)"
  else
    url="$(printf '%s' "$rel" | jq -r --arg a "$ASSET" '.[0].assets[]? | select(.name==$a) | .browser_download_url' | head -1)"
  fi
fi
if [ -z "$url" ]; then
  # tanpa jq: cari URL yang berakhiran persis /<asset>
  url="$(printf '%s' "$rel" | tr ',' '\n' | grep -o "https://[^\"]*/$ASSET" | head -1)"
fi
[ -n "$url" ] || { echo "[ksud] GAGAL: aset '$ASSET' tidak ditemukan di rilis $REPO"; exit 1; }

echo "[ksud] unduh: $url"
curl -fsSL -o "$OUT.tmp" "$url"

size=$(stat -c%s "$OUT.tmp")
if [ "$size" -lt 1000000 ]; then
  echo "[ksud] GAGAL: ukuran $size byte (terlalu kecil / bukan binary)"; rm -f "$OUT.tmp"; exit 1
fi
if [ "$(head -c4 "$OUT.tmp" | od -An -tx1 | tr -d ' \n')" != "7f454c46" ]; then
  echo "[ksud] GAGAL: bukan berkas ELF"; rm -f "$OUT.tmp"; exit 1
fi
if command -v file >/dev/null 2>&1; then
  file "$OUT.tmp" | grep -q 'ARM aarch64' || { echo "[ksud] GAGAL: bukan ELF aarch64"; rm -f "$OUT.tmp"; exit 1; }
fi
mv "$OUT.tmp" "$OUT"; chmod 0755 "$OUT"
echo "[ksud] OK: $OUT ($size byte)"
