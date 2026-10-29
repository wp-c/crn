#!/bin/bash
# =============================================================================
#  tools/tune-device.sh — tuning runtime Rollercoaster (userspace, aman, bisa balik)
#
#  Ini BUKAN pengganti config kernel: ini knob sysfs/procfs yang dibaca driver
#  di tree ini (WALT, msm_performance, cpu_boost, cfq/deadline, fq_codel).
#  Semua penulisan dicek dulu: kalau node tidak ada, dilaporkan "skip" — tidak
#  ada error dan tidak ada yang dipaksa.
#
#  Pakai (di HP, butuh root):
#    su -c 'sh tools/tune-device.sh apply chronicle'
#    su -c 'sh tools/tune-device.sh apply critical'
#    su -c 'sh tools/tune-device.sh apply roll'
#    su -c 'sh tools/tune-device.sh revert'
#    su -c 'sh tools/tune-device.sh status'
#
#  Nilai asli disimpan di /data/local/tmp/rc-tune-backup.txt SEBELUM ditimpa,
#  jadi `revert` mengembalikan persis seperti sebelumnya.
# =============================================================================
set -u

PROFILE="${2:-}"
BACKUP=/data/local/tmp/rc-tune-backup.txt

say() { echo "[rc-tune] $*"; }

need_root() {
  [ "$(id -u)" = "0" ] || { say "butuh root: jalankan lewat su -c"; exit 1; }
}

save() { # save <path>
  local p="$1"
  [ -f "$p" ] || return 0
  grep -q "^$p=" "$BACKUP" 2>/dev/null && return 0
  echo "$p=$(cat "$p" 2>/dev/null)" >> "$BACKUP"
}

w() { # w <path> <value> [label]
  local p="$1" v="$2" label="${3:-$(basename "$1")}"
  if [ -e "$p" ]; then
    save "$p"
    if echo "$v" > "$p" 2>/dev/null; then
      say "  set   $label = $v"
    else
      say "  GAGAL $label = $v (read-only / ditolak driver)"
    fi
  else
    say "  skip  $label (node tidak ada)"
  fi
}

cmd_status() {
  say "profil sekarang: $(cat /proc/sys/kernel/sched_boost 2>/dev/null | sed 's/^/sched_boost=/')"
  for z in /sys/class/thermal/thermal_zone0/temp; do :; done
  for f in /proc/sys/vm/swappiness /proc/sys/kernel/sched_boost \
           /sys/module/cpu_boost/parameters/input_boost_ms \
           /proc/sys/net/ipv4/tcp_congestion_control; do
    [ -e "$f" ] && say "  $(basename "$f") = $(cat "$f" 2>/dev/null)"
  done
  say "  uname: $(uname -r)"
}

cmd_revert() {
  need_root
  [ -f "$BACKUP" ] || { say "tidak ada backup ($BACKUP)"; exit 1; }
  while IFS='=' read -r p v; do
    [ -n "$p" ] || continue
    if [ -e "$p" ]; then
      echo "$v" > "$p" 2>/dev/null && say "  pulih $p = $v" || say "  GAGAL pulih $p"
    fi
  done < "$BACKUP"
  : > "$BACKUP"
  say "selesai — nilai dikembalikan seperti sebelum tuning"
}

cmd_apply() {
  need_root
  [ -n "$PROFILE" ] || { say "pakai: apply <chronicle|critical|roll|lite>"; exit 1; }
  touch "$BACKUP" 2>/dev/null || { say "tidak bisa tulis $BACKUP"; exit 1; }

  say "== jaringan (semua profil): BBR + fq_codel =="
  w /proc/sys/net/ipv4/tcp_congestion_control bbr tcp_congestion_control
  w /proc/sys/net/core/default_qdisc fq_codel default_qdisc

  say "== blok I/O: iostats mati, rq_affinity=2, readahead =="
  for q in /sys/block/sd*/queue /sys/block/mmcblk*/queue /sys/block/dm-*/queue; do
    [ -d "$q" ] || continue
    w "$q/iostats" 0 "$(dirname "$q")/iostats"
    w "$q/rq_affinity" 2 "$(dirname "$q")/rq_affinity"
    w "$q/read_ahead_kb" 512 "$(dirname "$q")/read_ahead_kb"
  done

  case "$PROFILE" in
    critical)
      say "== CRITICAL: gaming =="
      for c in /sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_governor; do
        w "$c" performance "$c"
      done
      # WALT: boost penuh (frame drop saat sentuh / beban naik)
      w /proc/sys/kernel/sched_boost 1 sched_boost
      # cpu_boost: dorong big core saat layar disentuh
      for f in /sys/module/cpu_boost/parameters/input_boost_ms \
               /sys/module/cpu_boost/parameters/input_boost_freq; do
        [ -e "$f" ] && case "$f" in
          *ms)   w "$f" 1500 input_boost_ms ;;
          *freq) w "$f" "0:1804800 1:1804800 2:1804800 3:1804800 4:2200000 5:2200000 6:2200000 7:2200000" input_boost_freq ;;
        esac
      done
      # I/O: deadline untuk loading aset game
      for s in /sys/block/sd*/queue/scheduler /sys/block/mmcblk*/queue/scheduler /sys/block/dm-*/queue/scheduler; do
        [ -e "$s" ] && w "$s" deadline "$s"
      done
      w /proc/sys/vm/swappiness 100 vm.swappiness
      w /proc/sys/kernel/sched_nr_migrate 16 sched_nr_migrate
      # GPU: governor performa-keseimbangan
      w /sys/class/kgsl/kgsl-3d0/devfreq/governor msm-adreno-tz kgsl_governor
      ;;
    chronicle|roll)
      say "== $PROFILE: harian seimbang =="
      for c in /sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_governor; do
        w "$c" schedutil "$c"
      done
      w /proc/sys/kernel/sched_boost 0 sched_boost
      for s in /sys/block/sd*/queue/scheduler /sys/block/mmcblk*/queue/scheduler /sys/block/dm-*/queue/scheduler; do
        [ -e "$s" ] && w "$s" cfq "$s"
      done
      w /proc/sys/vm/swappiness 100 vm.swappiness
      w /proc/sys/vm/vfs_cache_pressure 100 vm.vfs_cache_pressure
      w /proc/sys/vm/dirty_background_ratio 5 vm.dirty_background_ratio
      w /proc/sys/vm/dirty_ratio 10 vm.dirty_ratio
      ;;
    lite)
      say "== LITE: hemat daya (tanpa root stack) =="
      for c in /sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_governor; do
        w "$c" schedutil "$c"
      done
      w /proc/sys/kernel/sched_boost 0 sched_boost
      w /proc/sys/vm/swappiness 60 vm.swappiness
      w /proc/sys/vm/dirty_writeback_centisecs 1500 vm.dirty_writeback_centisecs
      ;;
    *) say "profil tidak dikenal: $PROFILE"; exit 1 ;;
  esac

  say "selesai. balikkan dengan: su -c 'sh $0 revert'"
  cmd_status
}

case "${1:-}" in
  apply)  cmd_apply ;;
  revert) cmd_revert ;;
  status) cmd_status ;;
  *) sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//' ;;
esac
