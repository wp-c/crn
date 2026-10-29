# AnyKernel3 Ramdisk Mod Script
# osm0sis @ xda-developers
# Rollercoaster-Roll.3.3 — Linux 4.14.370 + KernelSU-Next + SuSFS v2.3.0 + NoMount
#
# PENTING (perbaikan 3.3):
#   Paket ini TIDAK lagi memaksa DTB dan cmdline milik satu ROM saja.
#   - DTB hanya diganti kalau DTB di boot image memang DTB ROM 3.0.304.0
#     (sha256 6ae7105b…). ROM lain -> DTB asli perangkat dipertahankan.
#   - cmdline dipertahankan apa adanya; hanya token sisa eksperimen 1.3.0
#     (memmap=…, ram.mem_address=…) yang dibuang kalau ada.
#   - /data/adb/ksud yang sudah ada TIDAK ditimpa.

## AnyKernel setup
properties() { '
kernel.string=Rollercoaster-Roll.3.3 - Linux 4.14.370 + KernelSU-Next + SUSFS + NoMount - miatoll
kernel.compiler=clang 18 (AOSP LLVM)
kernel.made=graziemille
kernel.version=Rollercoaster-Roll.3.3
message.word=Rollercoaster-Roll.3.3
do.devicecheck=1
do.modules=0
do.systemless=1
do.cleanup=1
do.cleanuponabort=0
device.name1=miatoll
device.name2=curtana
device.name3=excalibur
device.name4=gram
device.name5=joyeuse
supported.versions=
supported.patchlevels=
'; } # end properties

# shell variables
block=/dev/block/bootdevice/by-name/boot;
is_slot_device=0;
ramdisk_compression=auto;
patch_vbmeta_flag=auto;

# DTB ROM 3.0.304.0 yang kita punya (330 458 byte)
DTB_3040_SHA256=6ae7105b5ad512e40af1f14e53a8c27010b2d7caf0934d53c58cfcc24cc3f239


## AnyKernel methods (DO NOT CHANGE)
. tools/ak3-core.sh;


## AnyKernel file attributes
set_perm_recursive 0 0 755 644 $ramdisk/*;
set_perm_recursive 0 0 750 750 $ramdisk/init* $ramdisk/sbin;


## AnyKernel boot install
dump_boot;

ui_print " "
ui_print "  Rollercoaster-Roll.3.3"
ui_print "  Linux 4.14.370 + KernelSU-Next + SuSFS + NoMount"
ui_print " "

# --- cadangan senyap --------------------------------------------------------
DIAG=/sdcard/Rollercoaster-diag
mkdir -p $DIAG 2>/dev/null
cp -f $split_img/boot.img $DIAG/boot-sebelum-roll3.3.img 2>/dev/null

# --- cmdline: pertahankan milik ROM, buang sisa 1.3.0 -----------------------
if [ -f "$split_img/cmdline.txt" ]; then
  OLD_CMD=$(cat "$split_img/cmdline.txt" 2>/dev/null)
else
  OLD_CMD=$(grep '^cmdline=' "$split_img/header" 2>/dev/null | cut -d= -f2-)
fi
echo "$OLD_CMD" > $DIAG/cmdline-sebelum-roll3.3.txt 2>/dev/null

case "$OLD_CMD" in
  *memmap=*|*ram.mem_address=*)
    NEW_CMD=$(echo "$OLD_CMD" | sed -e 's/memmap=[^ ]*//g' -e 's/ram\.mem_address=[^ ]*//g' -e 's/  */ /g' -e 's/^ *//' -e 's/ *$//')
    echo "$NEW_CMD" > $split_img/cmdline.txt
    ui_print "  cmdline: token 1.3.0 dibuang (${#NEW_CMD} byte)"
    ;;
  "")
    ui_print "  cmdline: kosong, dibiarkan"
    ;;
  *)
    ui_print "  cmdline: dipakai apa adanya (${#OLD_CMD} byte)"
    ;;
esac

# --- DTB: hanya ganti kalau memang DTB ROM 3.0.304.0 ------------------------
# Dua syarat, keduanya wajib: (1) DTB di boot image = DTB ROM 3.0.304.0,
# (2) berkas ./dtb benar-benar ada di paket. Tanpa syarat (2) zip tetap sah:
# DTB asli perangkat dipertahankan (lebih aman daripada cp gagal di tengah).
if [ -f "$split_img/dtb" ] && [ -s "$split_img/dtb" ]; then
  CUR_SHA=$(sha256sum "$split_img/dtb" 2>/dev/null | cut -d' ' -f1)
  if [ "$CUR_SHA" = "$DTB_3040_SHA256" ]; then
    if [ -s ./dtb ]; then
      cp -f ./dtb $split_img/dtb
      ui_print "  DTB: ROM 3.0.304.0 terdeteksi -> ramoops ditanam"
    else
      ui_print "  DTB: ROM 3.0.304.0 terdeteksi, tapi paket tanpa dtb -> asli dipakai"
    fi
  else
    ui_print "  DTB: ROM lain terdeteksi -> DTB asli dipertahankan"
  fi
else
  ui_print "  DTB: boot.img tanpa DTB -> tidak diubah"
fi

ui_print "  Memasang kernel..."
write_boot;
ui_print "  Selesai. Reboot system."


# --- ksud: jangan timpa yang sudah ada --------------------------------------
if [ -f ./ksud ]; then
  mount -o rw /data 2>/dev/null
  [ -d /data/adb ] || mount -t f2fs -o rw /dev/block/by-name/userdata /data 2>/dev/null
  [ -d /data/adb ] || mount -t f2fs -o rw /dev/block/bootdevice/by-name/userdata /data 2>/dev/null
  [ -d /data/adb ] || mount -t ext4 -o rw /dev/block/by-name/userdata /data 2>/dev/null
  [ -d /data/adb ] || mount -t ext4 -o rw /dev/block/bootdevice/by-name/userdata /data 2>/dev/null
  if [ -d /data/adb ] && [ ! -f /data/adb/ksud ]; then
    cp -f ./ksud /data/adb/ksud
    chown 0:0 /data/adb/ksud 2>/dev/null
    chmod 0755 /data/adb/ksud 2>/dev/null
    mkdir -p /data/adb/ksu/bin /data/adb/ksu/log /data/adb/ksu/cache /data/adb/modules /data/adb/modules_update 2>/dev/null
    ui_print "  KernelSU: ksud dipasang (belum ada sebelumnya)"
  fi
fi
## end boot install
