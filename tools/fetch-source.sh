#!/bin/bash
# =============================================================================
#  tools/fetch-source.sh — ambil tree kernel miatoll (pin versi)
#
#  Pakai:
#    bash tools/fetch-source.sh <tujuan>                # clone penuh (untuk build)
#    bash tools/fetch-source.sh <tujuan> --sparse        # HANYA Kconfig (untuk audit)
#
#  Variabel:
#    SOURCE_REPO  default https://github.com/Saamitk/kernel_xiaomi_sm6250-switchoff
#    SOURCE_REF   default a          (4.14.357-openela, KSU+SUSFS+NoMount in-tree)
#    DEPTH        default 1
#
#  --sparse memakai partial clone (blob:none) + sparse-checkout, jadi hanya
#  mengunduh ±25 MB (bukan ±1,5 GB) dan cukup untuk tools/audit-config.py.
# =============================================================================
set -euo pipefail

DEST="${1:?usage: fetch-source.sh <tujuan> [--sparse]}"
MODE="${2:-full}"
REPO="${SOURCE_REPO:-https://github.com/Saamitk/kernel_xiaomi_sm6250-switchoff.git}"
REF="${SOURCE_REF:-a}"
DEPTH="${DEPTH:-1}"

mkdir -p "$(dirname "$DEST")"
if [ -e "$DEST/.git" ]; then
  echo "[source] sudah ada: $DEST (dipakai apa adanya)"
  exit 0
fi

if [ "$MODE" = "--sparse" ]; then
  echo "[source] partial clone (audit): $REPO @ $REF"
  git clone --depth="$DEPTH" --filter=blob:none --no-checkout --single-branch -b "$REF" "$REPO" "$DEST"
  cd "$DEST"
  git sparse-checkout init --no-cone
  # yang dibutuhkan kconfiglib: seluruh Kconfig + defconfig + Makefile versi
  # + symlink drivers/kernelsu -> KernelSU-Next/kernel (Kconfig KSU/SuSFS)
  git sparse-checkout set --no-cone \
    '/Kconfig*' '**/Kconfig' '**/Kconfig.*' \
    '/Makefile' '/scripts/kconfig/**' '/scripts/Kbuild.include' \
    '/arch/arm64/Kconfig*' '/arch/arm64/Makefile' '/arch/arm64/configs/**' \
    '/drivers/kernelsu' '/KernelSU-Next/**' \
    '/kernel/sysctl.c' '/mm/**' '/security/Kconfig.hardening' '/init/Kconfig'
  git checkout
else
  echo "[source] clone penuh: $REPO @ $REF (depth $DEPTH)"
  git clone --depth="$DEPTH" --single-branch -b "$REF" "$REPO" "$DEST"
fi

echo "[source] selesai: $DEST"
grep -E '^(VERSION|PATCHLEVEL|SUBLEVEL|EXTRAVERSION)' "$DEST/Makefile" | sed 's/^/         /'
