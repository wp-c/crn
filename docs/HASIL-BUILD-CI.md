# Bukti build di GitHub Actions (Rollercoaster 1.3.0)

Semua angka di bawah ini diambil dari API GitHub setelah job selesai,
bukan perkiraan.

Runner: `ubuntu-24.04`, **4 vCPU / 16 GB**, build memakai `-j$(nproc)` (= 4).
Toolchain: `clang-18` + `ld.lld-18` + `LLVM_IAS=1` (100 % LLVM, tanpa gcc).

## Hasil

| Run | Event | Profil | Durasi | Hasil |
|---|---|---|---|---|
| [40813843003](https://github.com/wp-c/crn/actions/runs/40813843003) | PR (label `build`) | chronicle | 03:44:34 → 04:05:20 (20m46s) | ✅ sukses, artifact 21.163.648 B |
| [40818083656](https://github.com/wp-c/crn/actions/runs/40818083656) | PR (label `build-all`) | chronicle · critical · roll (paralel) | 04:30:03 → 04:50:46 (20m43s) | ✅ ketiganya sukses |
| [40818083480](https://github.com/wp-c/crn/actions/runs/40818083480) | PR (label `build-all`) | chronicle · critical · roll (paralel) | 04:30:03 → 04:50:45 (20m42s) | ✅ ketiganya sukses |

Rincian job pada run `40818083656` (ketiga runner pararel):

| Job | Mulai | Selesai | Durasi |
|---|---|---|---|
| `audit` | 04:30:05 | 04:30:15 | 10 s |
| `setup` | 04:30:17 | 04:30:20 | 3 s |
| `build (roll)` | 04:30:22 | 04:39:25 | **9m03s** |
| `build (critical)` | 04:30:24 | 04:45:34 | **15m10s** |
| `build (chronicle)` | 04:30:23 | 04:50:45 | **20m22s** |

Artifact yang dihasilkan (zip AnyKernel3 siap flash):

| Artifact | Ukuran |
|---|---|
| `Rollercoaster-chronicle` | 21.163.698 B |
| `Rollercoaster-critical` | 21.164.987 B |
| `Rollercoaster-roll` | 21.147.113 B |

## Catatan penting

1. **Bukan cuma config yang lolos** — job build menjalankan seluruh jalur:
   patch wajib (apk_sign v3, aw8624, vermagic untuk profil port), audit config
   di dalam tree, `make Image.gz`, pemaketan AnyKernel3 + `ksud`, verifikasi
   isi zip, lalu upload artifact. Ketiganya berakhir `success`.
2. **Roll 9 menit** karena profil `roll` tidak memakai `CONFIG_KSU_SUSFS_TRY_UMOUNT`
   / `SUS_MEMFD` (tidak ada implementasinya) dan ccache sebagian hangat;
   chronicle paling lama karena paling banyak modul SuSFS.
3. **Antrian paralel itu nyata**: tiga profil dibangun di tiga runner berbeda
   pada saat yang sama, jadi wall-clock ±20 menit untuk tiga kernel — bukan
   3 × 20 menit. Di mesin lokal, `MODE=parallel bash rollercoaster/build-all.sh`
   memberi efek yang sama selama RAM cukup (±0,5 GB per job).
4. Artifact disimpan 30 hari. Untuk rilis permanen: push tag `v1.3.0` →
   job `release` mengunggah semua zip + `SHA256SUMS` ke GitHub Release.
5. Yang **belum** dibuktikan: perilaku di perangkat (boot, root Manager,
   suhu, FPS). Build sukses ≠ boot sukses; uji di joyeuse/curtana masih tugas
   berikutnya.
