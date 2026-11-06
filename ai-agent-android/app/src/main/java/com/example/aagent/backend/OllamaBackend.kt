package com.example.aagent.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.use
import org.json.JSONObject
import java.io.IOException

/**
 * Opsi tambahan: AI yang berjalan di PC kamu melalui Ollama (100% gratis, tanpa API key).
 * Aplikasi Android cukup kirim HTTP ke PC dalam jaringan Wi-Fi yang sama.
 *
 * Cara pakai:
 * 1. Install Ollama di PC: https://ollama.com
 * 2. Jalankan model, misal: ollama run llama3.2
 * 3. Ganti BASE_URL dengan IP PC kamu, contoh: http://192.168.1.100:11434
 */
class OllamaBackend(private val baseUrl: String) : AiBackend {

    override val name = "PC (Ollama)"
    private val client = OkHttpClient()

    override suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
            body.put("model", "llama3.2")
            body.put("prompt", prompt)
            body.put("stream", false)

            val request = Request.Builder()
                .url("$baseUrl/api/generate")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")!!))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val json = JSONObject(response.body!!.string())
                json.optString("response", "Tidak ada respons dari Ollama")
            }
        } catch (e: Exception) {
            "Gagal terhubung ke PC/Ollama: ${e.message ?: e.toString()}"
        }
    }

    override fun isAvailable(): Boolean = baseUrl.isNotBlank()
}