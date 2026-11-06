# Panduan: Buat APK Agen AI Sendiri (Gratis, Seperti Hermes)

**Jawaban singkat: bisa.** Kamu bisa membuat aplikasi Android (APK) sendiri yang berperan sebagai agen AI, tanpa biaya lisensi, dengan menggunakan model AI gratis.

Repo `crn` saat ini adalah proyek **kernel Android** (Rollercoaster untuk miatoll/SM6250), bukan aplikasi. Di bawah ini adalah starter proyek aplikasi AI Agent terpisah yang bisa kamu bangun menjadi APK.

---

## Pilihan AI Gratis yang Bisa Dipakai

| Opsi | Biaya | Keterangan |
|------|-------|------------|
| **Groq** | Gratis (tier gratis, biasanya tanpa kartu kredit) | API sangat cepat, model Llama-3.1-8b-instant. Daftar di https://console.groq.com |
| **Google Gemini** | Gratis tier | API Key dari Google AI Studio |
| **Ollama (lokal PC)** | 100% gratis, **tanpa API key** | Model berjalan di PC kamu sendiri; APK cukup kirim HTTP ke PC |
| **Hugging Face / OpenRouter** | Ada model gratis | Butuh API key |

Rekomendasi pemula: **Groq** (cepat dan gratis) atau **Ollama** (kalau ingin sepenuhnya gratis, tidak perlu daftar kartu kredit).

---

## Starter Project: `ai-agent-starter/`

Aplikasi chat Android (Kotlin) siap pakai:
- UI chat sederhana (Kamu vs AI)
- Memanggil API AI melalui HTTP
- Bisa dibangun menjadi APK lewat Android Studio

### Daftar file starter

```
ai-agent-starter/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── local.properties.example
├── .gitignore
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/aiagent/MainActivity.kt
        ├── res/layout/activity_main.xml
        ├── res/values/strings.xml
        └── res/values/colors.xml
```

### Cara pakai

1. Install **Android Studio** (https://developer.android.com/studio).
2. Buka folder `ai-agent-starter` sebagai project di Android Studio.
3. Tunggu Gradle sync selesai.
4. Dapatkan API Key gratis:
   - Groq: https://console.groq.com → menu **API Keys** (gratisan)
   - Atau pakai **Ollama** di PC (lihat bagian "Ollama Lokal" di bawah).
5. Edit file `ai-agent-starter/app/src/main/java/com/example/aiagent/MainActivity.kt`, ganti:
   ```kotlin
   private const val API_KEY = "MASUKKAN_API_KEY_GRATIS_GROQ_KAMU_DI_SINI"
   ```
   Sesuaikan `API_URL` dan `MODEL` jika ingin pakai penyedia lain.
6. Jalankan ke emulator atau ponsel (aktifkan **USB Debugging** di ponsel): klik **Run ▶**.
7. Bangun APK: **Build → Bundle(s) / APK(s) → Build APK(s)**. Hasilnya ada di:
   `app/build/outputs/apk/debug/app-debug.apk`

Tanpa Android Studio (jika Gradle sudah terinstall di komputer):

```bash
cd ai-agent-starter
gradle wrapper          # jika belum ada berkas gradlew
./gradlew assembleDebug
```

---

## Ollama Lokal — 100% Gratis, Tanpa API Key

Jika tidak mau daftar API key sama sekali, jalankan model AI di PC kamu sendiri:

1. Install **Ollama** di PC: https://ollama.com
2. Unduh dan jalankan model, misalnya:
   ```bash
   ollama run llama3.2
   # atau: ollama run qwen2.5
   ```
3. Di `MainActivity.kt`, ganti konfigurasi jadi:
   ```kotlin
   private const val API_URL = "http://<IP-PC-KAMU>:11434/api/chat" // contoh: 192.168.1.100
   private const val API_KEY = ""                                   // tidak diperlukan
   private const val MODEL = "llama3.2"
   ```
   Hapus pula header `Authorization` di dalam fungsi `askAi()`, dan pastikan ponsel serta PC dalam **sama sekali satu jaringan Wi-Fi**. Izinkan port 11434 di firewall PC jika diblokir.

---

## Peringatan Keamanan Penting

**Jangan pernah commit API Key asli ke Git.** Simpan kunci API di `local.properties` atau masukkan manual sebelum build, lalu tambahkan `local.properties` ke `.gitignore` agar tidak terunggah ke repositori publik.

---

## Ide Pengembangan Lanjutan

- Tombol mikrofon → ubah suara menjadi teks (`SpeechRecognizer`)
- Bacaan jawaban keras keras → `TextToSpeech`
- Riwayat obrolan tersimpan (Room / SQLite)
- RAG: masukkan dokumen PDF pribadi agar AI menjawab berdasarkan file kamu
- Function calling / tools: AI bisa menyalakan lampu, cek cuaca, kirim pesan, dll.

---

## Ringkasan

Ya, kamu bisa membuat APK agen AI sendiri dengan gratis. Pilih penyedia AI gratis (Groq, Gemini, atau Ollama), gunakan starter Kotlin di atas, isi API key, lalu bangun APK lewat Android Studio.

Selamat mencoba! Kalau ingin versi kode yang sudah disesuaikan khusus untuk Ollama (parsing respons yang berbeda), saya bisa buatkan.