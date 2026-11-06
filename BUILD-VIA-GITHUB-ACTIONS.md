# Build APK melalui GitHub Actions (tanpa Android Studio)

Kamu tidak perlu install Android Studio. Repositori ini sudah menyertakan workflow GitHub Actions yang akan membangun APK AI Agent secara otomatis.

## Langkah-langkah

### 1. Push perubahan ke GitHub

Dari komputer kamu, jalankan:

```bash
cd /home/user/crn
git add .
git commit -m "Add AI Agent Android project + GitHub Actions workflow"
git push origin arena/f6c1ea79-crn
```

### 2. Aktifkan GitHub Actions

- Buka repositori `wp-c/crn` di GitHub (browser)
- Masuk ke tab **Actions**
- Klik tombol **I understand my workflows, go ahead and enable them**

### 3. Jalankan build

- Masuk ke tab **Actions**
- Pilih workflow **"Build AI Agent APK"** di sebelah kiri
- Klik **Run workflow** → pilih branch `arena/f6c1ea79-crn` → klik tombol hijau **Run workflow**

### 4. Unduh APK

- Tunggu sampai build selesai (status berkedip kuning → hijau)
- Masuk ke hasil build (run) yang baru selesai
- Di sebelah kanan bawah, cari bagian **Artifacts**
- Klik `ai-agent-apk` untuk mengunduh berkas `.zip`
- Ekstrak zip, di dalamnya ada `app-debug.apk`

## Instal APK di ponsel Android

1. Pindahkan `app-debug.apk` ke ponsel (via USB, email, drive, dll.)
2. Di ponsel, buka berkas `.apk`
3. Aktifkan **Install unknown apps** jika ditanya
4. Klik **Install**

Aplikasi akan muncul sebagai **AI Agent Gratis**.

## Mode default

- **AI Gratis (Cloud)** — pakai Groq (gratis, pintar, butuh API key)
  - Isi API key di `ai-agent-android/app/src/main/java/com/example/aagent/backend/CloudBackend.kt`
  - Dapatkan API key gratis di https://console.groq.com
- **Offline** — butuh file `model.gguf` di `app/src/main/assets/`
- **PC (Ollama)** — opsi tambahan, ganti IP PC di `MainActivity.kt`

## Catatan

- Build ini menghasilkan **Debug APK** (tidak ditandatangani untuk rilis production, tapi bisa di-install di ponsel untuk pribadi)
- Artifact tersedia selama 30 hari
- Saya (agent) tidak bisa menjalankan build ini secara langsung — kamu yang harus *push* dan klik *Run workflow* di GitHub
