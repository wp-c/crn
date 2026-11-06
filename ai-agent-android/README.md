# AI Agent Gratis — APK untuk Android

Proyek Android Studio lengkap untuk membuat APK agen AI sendiri.
Tidak perlu PC untuk menjalankan aplikasi; PC hanya opsi jika kamu ingin pakai Ollama lokal.

## Fitur

| Mode | Keterangan | Biaya | Internet |
|------|-----------|-------|----------|
| **AI Gratis (Cloud)** | Groq Llama-3.1-8b-instant (default, pintar) | Gratis tier | Diperlukan |
| **Offline** | Model quantized di assets (on-device) | Gratis | Tidak diperlukan |
| **PC (Ollama)** | Model berjalan di PC kamu via Wi-Fi | Gratis | Lokal saja |

## Cara build menjadi APK

1. Install **Android Studio**: https://developer.android.com/studio
2. Buka folder `ai-agent-android` sebagai project.
3. Tunggu Gradle sync selesai.
4. (Opsional) Dapatkan API key gratis Groq di https://console.groq.com, lalu edit file:
   ```
   app/src/main/java/com/example/aagent/backend/CloudBackend.kt
   ```
   Ganti `MASUKKAN_API_KEY_GROQ_DI_SINI` dengan API key kamu.
5. Klik tombol **Run ▶** untuk mencoba di emulator atau ponsel (aktifkan USB Debugging).
6. Untuk buat APK: **Build → Bundle(s) / APK(s) → Build APK(s)**.
   Hasil: `app/build/outputs/apk/debug/app-debug.apk`

> Catatan: Saya tidak bisa menghasilkan file `.apk` biner di sini karena tidak ada Android SDK dan kunci signing. Kamu perlu buildnya sendiri di Android Studio (5-10 menit).

## Mode Offline (sepenuhnya tanpa internet)

1. Unduh model LLM quantized, contoh: `Llama-3.2-1B-Instruct-Q4_K_M.gguf` (atau model lain yang ringan).
2. Ganti nama file jadi `model.gguf`.
3. Masukkan ke folder: `app/src/main/assets/model.gguf`
4. Tambahkan library inferensi Android (contoh: llama.cpp AAR) ke `app/build.gradle`.
5. Implementasikan pemanggilan generate di `OfflineBackend.kt`.
6. Build ulang menjadi APK.

## Mode PC (Ollama) — opsi tambahan

1. Install Ollama di PC: https://ollama.com
2. Jalankan model: `ollama run llama3.2`
3. Ganti IP di `MainActivity.dart` (baris `OllamaBackend(...)`) dengan IP PC kamu, contoh `http://192.168.1.100:11434`.
4. Pastikan ponsel dan PC dalam satu jaringan Wi-Fi.

## Peringatan

Jangan pernah commit API key asli atau file model besar ke Git. File `.gitignore` sudah mencegahnya.

## Struktur

```
ai-agent-android/
├── app/src/main/java/com/example/aagent/
│   ├── MainActivity.kt
│   └── backend/
│       ├── AiBackend.kt
│       ├── CloudBackend.kt   # Gratis, default
│       ├── OllamaBackend.kt  # PC lokal
│       └── OfflineBackend.kt # On-device
├── app/src/main/res/layout/activity_main.xml
└── build.gradle (project & app)
```

Selamat mencoba!