# crn — Rollercoaster kernel (miatoll / sm6250)

Kernel custom **Linux 4.14.357-openela**, arm64, non-GKI untuk
Redmi Note 9 Pro/Pro Max/9S & POCO M2 Pro
(joyeuse · curtana · excalibur · gram · miatoll).

Build 100 % LLVM: **clang 18 + `ld.lld` + `LLVM_IAS=1`**, dikemas dengan
AnyKernel3.

Rilis aktif: **1.3.0** (`rollercoaster/VERSION`).

## Profil build

| Profil | Root stack | Untuk |
|---|---|---|
| `chronicle` | KernelSU-Next + SuSFS lengkap + NoMount | harian AOSP/Lineage, penyamaran maksimal |
| `critical` | KernelSU-Next + SuSFS + NoMount, `HZ=300`, governor `performance` | gaming, FPS tinggi |
| `roll` | KernelSU-Next + SuSFS + NoMount + modul ROM port boleh dimuat | satu paket: AOSP **dan** ROM port (ColorOS/HyperOS) |
| `lite` | tanpa root | harian, baterai, tanpa jejak root |
| `portn` | KernelSU-Next + SuSFS, **tanpa** NoMount | ROM port yang butuh mount nyata (Zygisk Next / metamodule) |

## Build (semua core dipakai otomatis)

```bash
# 0. alat (Debian/Ubuntu)
sudo apt install clang-18 lld-18 llvm-18 ccache bc bison flex \
                 libssl-dev libelf-dev zip unzip cpio python3

# 1. ambil tree kernel (pin versi)
bash tools/fetch-source.sh tree

# 2. ksud (userspace KernelSU-Next, tidak disimpan di repo)
bash tools/fetch-ksud.sh

# 3. build — JOBS=auto berarti -j$(nproc)
bash rollercoaster/build-rollercoaster.sh tree chronicle
bash rollercoaster/build-rollercoaster.sh tree critical
bash rollercoaster/build-rollercoaster.sh tree roll

# atau semua sekaligus
bash rollercoaster/build-all.sh tree chronicle critical roll lite
MODE=parallel bash rollercoaster/build-all.sh tree      # mesin core banyak
```

Hasil: `output/Rollercoaster-<Profil>-1.3.0.zip` + `.sha256`.

Butuh RAM ±0,5 GB per job (4 job ≈ 2 GB). Disk ±8 GB per profil.

## Build di GitHub Actions

```bash
gh workflow run kernel.yml -f profiles="chronicle critical roll" -f jobs=auto
gh run watch
```

* `audit` — jalan di setiap push/PR: memastikan setiap baris profil benar-benar
  berefek di tree (gagal kalau ada simbol stale/inert).
* `build` — matrix per profil, clang-18, ccache, `-j$(nproc)` (runner standar
  = 4 vCPU), artifact 30 hari.
* `release` — otomatis kalau push tag `v*`.

## Status build

Tiga profil utama sudah dibangun di GitHub Actions dan lolos seluruh jalur
(patch → audit → `Image.gz` → AnyKernel3 → artifact):

| Profil | Durasi (4 vCPU) | Artifact |
|---|---|---|
| roll | 9m03s | 21.147.113 B |
| critical | 15m10s | 21.164.987 B |
| chronicle | 20m22s | 21.163.698 B |

Rincian: `docs/HASIL-BUILD-CI.md`. Yang **belum**: uji di perangkat.

## Struktur

```
rollercoaster/
  VERSION                     versi + daftar build
  build-rollercoaster.sh      builder 1 profil  (JOBS=auto = nproc)
  build-all.sh                banyak profil, serial/parallel, RAM-aware
  profiles/
    00-common.config          blok bersama (perf + A17 + zram/BBR/EROFS)
    chronicle|critical|roll|lite|portn|port.config
    10-opt-common.config      optimasi 1.3.0 (semua profil)
    11-opt-aosp-only.config   optimasi khusus AOSP (bukan ROM port)
  anykernel/
    anykernel-chronicle.sh    AK3: tanpa backup otomatis
    anykernel-roll.sh         AK3 Roll 3.3: DTB/cmdline ROM-safe, ksud tidak ditimpa
  patches/                    patch wajib (apk_sign v3, aw8624, vermagic port)
tools/
  audit-config.py             audit profil vs Kconfig tree (kconfiglib)
  fetch-source.sh             clone tree (penuh / --sparse untuk audit)
  fetch-ksud.sh               ambil binary ksud dari rilis KernelSU-Next
  tune-device.sh              tuning runtime di HP (apply/revert)
docs/
  OPTIMASI-1.3.0.md           semua perubahan + bukti + risiko + cara balik
  AUDIT-HASIL.txt             keluaran audit terakhir
  HASIL-BUILD-CI.md           bukti build di GitHub Actions (durasi + artifact)
```

## Prinsip kerja proyek ini

1. Setiap baris config harus **terbukti** ada di tree — `tools/audit-config.py`
   menolak simbol stale/inert, karena `olddefconfig` membuangnya diam-diam.
2. Basis defconfig adalah defconfig **lengkap** milik tree, bukan blok kita saja
   (kalau tidak, `olddefconfig` mematikan hampir semua driver).
3. Klaim performa ditulis sebagai **perubahan jalur kode**, bukan angka
   benchmark, sampai ada pengukuran di perangkat.
4. Yang tidak bisa dibuktikan ditulis di bagian “belum dikerjakan”, bukan
   disembunyikan.
