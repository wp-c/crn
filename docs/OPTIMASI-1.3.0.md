# Rollercoaster 1.3.0 — catatan optimasi (Chronicle · Critical · Roll)

Tanggal: 29 Oktober 2026
Basis: `Saamitk/kernel_xiaomi_sm6250-switchoff` branch `a`, **4.14.357-openela**, arm64, non-GKI
Perangkat: miatoll (joyeuse · curtana · excalibur · gram)
Toolchain: clang 18 + `ld.lld` + `LLVM_IAS=1` (100 % LLVM)

Dokumen ini hanya berisi hal yang **diverifikasi** terhadap Kconfig tree, bukan klaim.
Setiap perubahan punya: alasan, bukti, risiko, dan cara membatalkannya.

---

## 0. Ringkas

| # | Optimasi | Chronicle | Critical | Roll | Bukti |
|---|---|---|---|---|---|
| 1 | `DEBUG_PREEMPT` dimatikan | ✅ | ✅ | ✅ | resolved config: `CONFIG_DEBUG_PREEMPT=y` → `is not set` |
| 2 | `INIT_ON_ALLOC_DEFAULT_ON` dimatikan (zeroing heap) | ✅ | ✅ | ✅ | `security/Kconfig.hardening:56`, `mm/page_alloc.c:136` |
| 3 | Akuntansi I/O per task dimatikan | ✅ | ✅ | ➖ sengaja tidak | diff resolved config |
| 4 | Simbol mati `SCHED_INFO` dinyatakan jujur | ✅ | ✅ | ✅ | audit: dulu `inert`, sekarang `OK` |
| 5 | Profil `roll` masuk repo (dulu hanya zip) | — | — | ✅ | `profiles/roll.config` |
| 6 | Build: `-j$(nproc)` + ccache + pipa CI | ✅ | ✅ | ✅ | `build-rollercoaster.sh`, `.github/workflows/kernel.yml` |
| 7 | Audit config otomatis sebelum compile | ✅ | ✅ | ✅ | `tools/audit-config.py` |

Hasil audit terakhir: **5 profil, 516 simbol diminta, 0 stale, 0 inert**
(`docs/AUDIT-HASIL.txt`).

---

## 1. Kenapa audit config dulu, bukan langsung tuning

`make olddefconfig` **membuang** simbol yang tidak ada di tree dan simbol yang
dependensinya tidak terpenuhi — tanpa peringatan. Jadi profil bisa berisi 100
baris padahal hanya 80 yang berefek, dan tidak ada yang tahu sampai ada yang
membongkar `out-*/\.config`.

`tools/audit-config.py` (kconfiglib, tanpa cross-compiler) membandingkan
**niat** (berkas profil) dengan **model Kconfig tree** dan gagal kalau ada:

* `STALE` — simbol tidak ada di tree (salah tulis atau fitur sudah hilang)
* `INERT` — simbol ada tapi dependensi tidak terpenuhi → nilainya diabaikan

Audit ini yang menemukan satu bug nyata di profil 1.2.0:

```
00-common.config:85  CONFIG_SCHED_INFO=y   -> hasil n
          depends on: SCHEDSTATS (yang sengaja dimatikan)
```

Sudah diperbaiki (dinyatakan `is not set` + alasannya ditulis di berkas).

---

## 2. Optimasi yang dipakai

### 2.1 `DEBUG_PREEMPT` dimatikan — jalur panas

`00-common.config` menyalakan `DEBUG_KERNEL=y` **karena itu syarat
`KALLSYMS_ALL`**, dan `KALLSYMS_ALL` dibutuhkan SuSFS untuk menyembunyikan
simbol (`CONFIG_KSU_SUSFS_HIDE_KSU_SUSFS_SYMBOLS=y`).

Efek sampingnya: `DEBUG_PREEMPT` (default `y` saat `DEBUG_KERNEL && PREEMPT`)
ikut hidup. Isinya tambahan pemeriksaan di `preempt_enable()` — fungsi yang
dipanggil puluhan ribu kali per detik (spinlock, `rcu_read_lock`, per-cpu).

* Bukti ada: `lib/Kconfig.debug` (`config DEBUG_PREEMPT`, default y)
* Hasil: `# CONFIG_DEBUG_PREEMPT is not set`
* Risiko: **nol fungsional** — ini murni alat debug (deteksi preempt count
  tidak seimbang). Peringatan debug itu hilang, bukan fitur.

### 2.2 `INIT_ON_ALLOC_DEFAULT_ON` dimatikan — zeroing heap

Tree ini mem-backport `security/Kconfig.hardening` (lihat `mm/page_alloc.c:136`,
`DEFINE_STATIC_KEY_TRUE(init_on_alloc)`), dan nilainya **default y**. Artinya
page allocator + slab meng-zero **setiap** alokasi.

Teks help kernel sendiri (jujur, dari `security/Kconfig.hardening`):

> The performance impact varies by workload, but most cases see **<1% impact**.
> Some synthetic workloads have measured as high as **7%**.

* Hasil: `# CONFIG_INIT_ON_ALLOC_DEFAULT_ON is not set`
* Risiko: **ini trade-off keamanan**, bukan sekadar debug. Tanpa zeroing, isi
  heap yang tidak diinisialisasi bisa terbaca proses lain (satu kelas bug
  “uninitialized heap”).
* Batalkan kapan saja: hapus baris di `10-opt-common.config`, **atau** tanpa
  flash ulang — tambahkan `init_on_alloc=1` di cmdline kernel
  (`patch_cmdline` AnyKernel3).

### 2.3 Akuntansi I/O per task — hanya build AOSP

`CONFIG_TASK_XACCT=y` + `CONFIG_TASK_IO_ACCOUNTING=y` menambah hook di setiap
`submit_bio()`. Tidak ada komponen AOSP/Lineage yang membacanya.

**Kenapa tidak dipakai di `roll` dan `portn`:** tree ini membawa
`CONFIG_UID_SYS_STATS` (statistik I/O per-uid milik MIUI/HyperOS) yang
men-`select` `TASK_XACCT`. Mematikan `TASK_XACCT` **ikut mematikan
`UID_SYS_STATS`** — terbukti di diff config, bukan dugaan:

```
< CONFIG_TASK_IO_ACCOUNTING=y
< CONFIG_TASK_XACCT=y
< CONFIG_UID_SYS_STATS=y
```

Karena `roll`/`portn` ditujukan untuk ROM port HyperOS/ColorOS (pengaturan
penyimpanan MIUI membaca `/proc/uid_io`), optimasi ini **tidak** dipakai di
sana. Berkas `11-opt-aosp-only.config` hanya di-append untuk
`chronicle · critical · lite`.

### 2.4 Profil `roll` — dari zip menjadi sumber

Roll 3.3 sebelumnya hanya ada sebagai paket zip (AnyKernel3 + `Image.gz` +
`dtb` + `ksud`) tanpa profil di repo, jadi tidak bisa dibangun ulang secara
reproduabel. Sekarang: `profiles/roll.config` + `anykernel/anykernel-roll.sh`.

Isi profil: root stack sama dengan Chronicle (KSU + SuSFS lengkap + NoMount),
**ditambah** kompatibilitas ROM port (`MODULE_SIG` mati, `MODVERSIONS` mati,
`FORCE_LOAD`), pstore/ramoops untuk diagnosa bootloop, f2fs compression, dan
BFQ tersedia (default tetap cfq).

Perbaikan pada skrip AnyKernel3 Roll (`anykernel-roll.sh`):

* DTB **hanya** diganti kalau DTB boot image memang DTB ROM 3.0.304.0
  (sha256 `6ae7105b…f239`) **dan** berkas `./dtb` ada di paket. Kalau tidak,
  DTB asli perangkat dipertahankan — dulu `cp` yang gagal di tengah bisa
  menggagalkan pemasangan.
* cmdline ROM dipertahankan; hanya sisa eksperimen 1.3.0 (`memmap=…`,
  `ram.mem_address=…`) yang dibuang.
* `/data/adb/ksud` yang sudah ada **tidak** ditimpa.

> **Soal versi 4.14.370:** paket Roll 3.3 melaporkan `4.14.370`, tetapi semua
> tree publik yang dipakai proyek ini (`swiitchOFF-miatoll/kernel_xiaomi_sm6250`
> dan `Saamitk/…-switchoff`, branch `a`, `16-QPR2`, `16.0`, `a-20260629`) masih
> **4.14.357-openela** (dicek langsung dari `Makefile` tiap branch). Patch stabil
> 358–370 tidak pernah masuk ke repo publik, jadi build ulang di sini
> menghasilkan **4.14.357** — bukan 370. Itu dinyatakan apa adanya; kalau tree
> 370-mu ada, jalankan `SOURCE_REPO=… SOURCE_REF=… bash rollercoaster/build-rollercoaster.sh …`
> dan nomor versi akan ikut otomatis (tidak di-hardcode).

---

## 3. Yang **sengaja tidak** diubah (dan alasannya)

| Kandidat | Status | Alasan |
|---|---|---|
| `SCHED_DEBUG` dimatikan | ❌ tidak | Menghapus `sched_nr_migrate` dan `sched_time_avg_ms` dari `/proc/sys/kernel/` (`kernel/sysctl.c:574-642`). Skrip `init.qcom.post_boot.sh` menulis `sched_nr_migrate` — tuning ROM jadi gagal senyap. Keuntungannya kecil, biayanya nyata. |
| `KALLSYMS_ALL` / `DEBUG_KERNEL` dimatikan | ❌ tidak | SuSFS butuh `KALLSYMS_ALL` untuk `hide_ksu_susfs_symbols`. Lebih baik matikan anak-anak debug yang mahal (2.1) daripada mencabut akarnya. |
| `LTO_CLANG` | ❌ tidak | Simbolnya **ada** (`ARCH_SUPPORTS_LTO_CLANG`), tapi LTO global pada 4.14 vendor + modul KSU + SuSFS berisiko gagal taut dan sulit didiagnosa dari jarak jauh. Bisa dicoba sebagai eksperimen terpisah. |
| `ZSMALLOC_PGTABLE_MAPPING` | ❌ tidak tersedia | Simbolnya **tidak ada** di tree ini (dicek). Jangan ditambahkan — `olddefconfig` akan membuangnya diam-diam dan orang mengira zram-nya lebih cepat. |
| `IOSCHED_BFQ` jadi default | ❌ tidak | BFQ tersedia dan diaktifkan sebagai opsi, tapi default tetap cfq (sama dengan ROM port). Mengganti default tanpa data = menebak. |
| `PSI` dimatikan | ❌ tidak | lmkd (low memory killer Android) membaca PSI. Mematikannya = aplikasi lebih sering dibunuh. |
| `SCHEDSTATS` dinyalakan untuk mengukur | ❌ tidak | Overhead tetap vs manfaat sesaat; audit cukup dari config. |

---

## 4. “Max CPU” — build memakai seluruh core

Masalah lama: `build-rollercoaster.sh` default **`JOBS=2`**. Di mesin 8 core
build tetap berjalan seperti mesin 2 core.

Sekarang:

```bash
# 1 profil, semua core otomatis
bash rollercoaster/build-rollercoaster.sh tree chronicle

# semua profil berurutan (aman RAM kecil), tiap-tiap -j$(nproc)
bash rollercoaster/build-all.sh tree chronicle critical roll lite

# semua profil sekaligus, core dibagi rata (mesin core banyak + RAM >= 16 GB)
MODE=parallel bash rollercoaster/build-all.sh tree chronicle critical roll
```

* `JOBS=auto` (default) → `nproc`; `build-all.sh` menampilkan `core=… jobs=…`.
* **ccache** disiapkan otomatis (`CCACHE_DIR`, 8 GB, `CCACHE_BASEDIR=$TREE`).
  Karena keempat profil berbagi ±99 % objek yang sama, build profil kedua dan
  seterusnya jauh lebih cepat.
* Tiap profil punya `O=out-<profil>` sendiri, jadi aman dijalankan paralel
  (tidak ada dua `make` menulis objek yang sama).
* Panduan RAM: ±0,5 GB per job. 4 job = ±2 GB, 8 job = ±4 GB. Kalau ragu,
  pakai `MODE=serial`.

### Pipeline GitHub Actions (`.github/workflows/kernel.yml`)

| Job | Kapan | Isi |
|---|---|---|
| `audit` | setiap push/PR | partial clone Kconfig (±25 MB) + `audit-config.py` untuk 5 profil, **gagal** kalau ada stale/inert |
| `build` | `workflow_dispatch` / tag `v*` | matrix profil, pasang clang-18 + lld-18, cache ccache, `-j$(nproc)`, upload zip + `SHA256SUMS` |
| `release` | tag `v*` | kumpulkan artifact, `gh release create` |

Runner standar GitHub = **4 vCPU / 16 GB**, jadi angka maksimum yang bisa
dipakai di sana adalah `-j4`. Perkiraan waktu (4 job, ccache dingin):
chronicle ±45–60 menit, critical ±45–60, roll ±45–60, lite ±40 (tanpa KSU).
Dengan ccache panas, profil berikutnya ±10–20 menit.

Menjalankan manual:

```bash
gh workflow run kernel.yml -f profiles="chronicle critical roll" -f jobs=auto
gh run watch
```

---

## 5. Tuning runtime (userspace, bisa dibalik)

`tools/tune-device.sh` menyentuh knob yang **memang ada** di tree ini
(WALT `sched_boost`, `cpu_boost`, scheduler blok, `fq_codel`/BBR, KGSL
governor). Setiap penulisan dicek: kalau node tidak ada → `skip`, tidak error.

```bash
su -c 'sh tools/tune-device.sh apply critical'   # gaming
su -c 'sh tools/tune-device.sh apply chronicle'  # harian
su -c 'sh tools/tune-device.sh revert'           # kembali seperti semula
su -c 'sh tools/tune-device.sh status'
```

Nilai asli disimpan lebih dulu di `/data/local/tmp/rc-tune-backup.txt`, jadi
`revert` mengembalikan nilai persis. Ini **bukan** pengganti config kernel:
kalau setelah tuning terasa lebih buruk, cukup `revert`, tanpa flash ulang.

---

## 6. Risiko & cara membatalkan

| Perubahan | Risiko | Batalkan |
|---|---|---|
| `DEBUG_PREEMPT=n` | tidak ada efek fungsional | hapus baris di `10-opt-common.config` |
| `INIT_ON_ALLOC_DEFAULT_ON=n` | data heap tak terinisialisasi bisa terbaca (satu kelas bug) | hapus baris, atau boot dengan `init_on_alloc=1` |
| `TASK_XACCT=n` (AOSP saja) | `/proc/uid_io` hilang; statistik penyimpanan MIUI **tidak** terpengaruh karena build ROM port tidak memakai blok ini | hapus `11-opt-aosp-only.config` dari daftar profil |
| `roll` (NoMount) | ROM port yang butuh mount nyata (Zygisk Next/metamodule) → pakai `portn` | pilih profil `portn` |

---

## 7. Sudah dibuktikan di GitHub Actions

Build nyata (bukan perkiraan) untuk **chronicle, critical, dan roll**:

| Profil | Durasi (runner 4 vCPU, `-j4`) | Hasil |
|---|---|---|
| roll | 9m03s | ✅ zip 21.147.113 B |
| critical | 15m10s | ✅ zip 21.164.987 B |
| chronicle | 20m22s | ✅ zip 21.163.698 B |

Ketiganya berjalan **paralel di tiga runner** (wall-clock ±20 menit untuk tiga
kernel) dan melewati seluruh jalur: patch wajib → audit config di dalam tree →
`make Image.gz` → paket AnyKernel3 + `ksud` → verifikasi isi zip → artifact.
Rincian + link run: [`docs/HASIL-BUILD-CI.md`](HASIL-BUILD-CI.md).

## 8. Belum dikerjakan (jujur)

* **Belum ada uji di perangkat.** Semua klaim performa di dokumen ini adalah
  klaim **struktural** (jalur kode yang tidak lagi dieksekusi), bukan angka
  benchmark. Angka nyata harus diukur di joyeuse/curtana: `uname -r`,
  `cat /proc/version`, `su -c id`, suhu saat main, dan FPS.
* Build sandbox sesi ini (2 core / 3 GB, tanpa clang) hanya dipakai untuk
  audit config + `bash -n` + diff config *resolved*; kompilasi penuh dilakukan
  di GitHub Actions seperti tabel di atas.
* LTO_CLANG, patch stabil 358–370 (Roll 3.3 melaporkan 370, tree publik masih
  357), dan backport interface `ioctl` KernelSU v2 untuk varian legacy masih
  terbuka.
