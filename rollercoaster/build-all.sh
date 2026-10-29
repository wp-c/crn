#!/bin/bash
# =============================================================================
#  build-all.sh — bangun SEMUA profil dengan pemakaian CPU maksimum
#
#  Pakai:
#    bash rollercoaster/build-all.sh <tree> [profil...]
#      contoh: bash rollercoaster/build-all.sh tree chronicle critical roll
#              JOBS=4 bash rollercoaster/build-all.sh tree        (semua profil)
#
#  Strategi "max CPU":
#    MODE=serial  (default)  : satu profil sekali jalan, tiap make -j$(nproc).
#                              Aman untuk RAM kecil (build 4.14 arm64 butuh
#                              ±2 GB per 4 job; rumus kasar: 0,5 GB/job).
#    MODE=parallel           : semua profil sekaligus, core dibagi rata.
#                              Lebih cepat untuk CPU banyak + RAM besar
#                              (dipakai kalau core >= 8 dan RAM >= 16 GB).
#                              Tiap profil punya O= sendiri, jadi aman.
#                              ccache dipakai bersama (CCACHE_DIR sama).
#
#  Variabel: JOBS, MODE, MEM_MIN_MB (default 1500/profil untuk parallel)
# =============================================================================
set -uo pipefail

TREE="${1:?usage: build-all.sh <tree> [profile...]}"
shift || true
SELF="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROFILES=("$@"); [ ${#PROFILES[@]} -eq 0 ] && PROFILES=(chronicle critical roll lite)

CORES="$(nproc 2>/dev/null || echo 2)"
JOBS="${JOBS:-auto}"
MODE="${MODE:-serial}"
[ "$JOBS" = "auto" ] && JOBS="$CORES"

echo "==============================================================="
echo " build-all: ${PROFILES[*]}"
echo " core=$CORES  jobs=$JOBS  mode=$MODE"
echo "==============================================================="

rc_total=0
run_one() { # run_one <profile> <jobs>
  local p="$1" j="$2"
  echo
  echo ">>> [$p] mulai $(date -u '+%H:%M:%S') (jobs=$j)"
  if JOBS="$j" bash "$SELF/build-rollercoaster.sh" "$TREE" "$p" "$j"; then
    echo "<<< [$p] SELESAI $(date -u '+%H:%M:%S')"
    return 0
  fi
  echo "<<< [$p] GAGAL $(date -u '+%H:%M:%S')"
  return 1
}

if [ "$MODE" = "parallel" ]; then
  per=$(( JOBS / ${#PROFILES[@]} )); [ "$per" -lt 1 ] && per=1
  pids=()
  for p in "${PROFILES[@]}"; do
    run_one "$p" "$per" &
    pids+=($!)
  done
  for pid in "${pids[@]}"; do wait "$pid" || rc_total=1; done
else
  for p in "${PROFILES[@]}"; do
    run_one "$p" "$JOBS" || rc_total=1
  done
fi

echo
if [ "$rc_total" = 0 ]; then
  echo "SEMUA PROFIL OK — hasil di $(cd "$SELF/.." && pwd)/output"
else
  echo "ADA PROFIL YANG GAGAL — lihat log di atas"
fi
exit "$rc_total"
