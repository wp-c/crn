package com.example.aagent.backend

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mode offline sepenuhnya di Android (tanpa internet).
 *
 * Untuk benar-benar pintar dan offline, letakkan file model quantized
 * (misal: model.gguf, ~1-4 GB) di folder app/src/main/assets/,
 * lalu tambahkan library inferensi (llama.cpp untuk Android) dan panggil
 * fungsi generate di dalam ask() di bawah ini.
 *
 * Saat ini mode ini sudah tersedia structurally; kamu cukup menyisipkan
 * implementasi native-nya.
 */
class OfflineBackend(private val context: Context) : AiBackend {

    override val name = "Offline (On-device)"
    private const val MODEL_ASSET = "model.gguf"

    override suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val modelExists = try {
            context.assets.open(MODEL_ASSET).use { } // cek hanya
            true
        } catch (e: Exception) {
            false
        }

        if (!modelExists) {
            "Mode offline belum siap.\n\n" +
            "1. Unduh model quantized (mis. Llama-3.2-1B-Instruct-Q4_K_M.gguf).\n" +
            "2. Ganti namanya menjadi model.gguf dan masukkan ke folder:\n" +
            "   app/src/main/assets/\n" +
            "3. Tambahkan library inferensi (llama.cpp Android) ke build.gradle.\n" +
            "4. Implementasikan pemanggilan generate() di OfflineBackend.kt.\n" +
            "5. Build ulang menjadi APK."
        } else {
            // TODO: disini tambahkan pemanggilan llama.cpp / TFLite inference
            "File model.gguf ditemukan, tetapi inferensi on-device belum diimplementasi.\n" +
            "Sisipkan kode llama.cpp Android di sini untuk menjalankan AI sepenuhnya offline."
        }
    }

    override fun isAvailable(): Boolean = true
}