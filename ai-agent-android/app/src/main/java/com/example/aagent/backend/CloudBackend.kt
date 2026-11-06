package com.example.aagent.backend

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.use
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Default: AI gratis dari Groq (Llama-3.1-8b-instant).
 * Cepat, pintar, dan tier gratis biasanya tidak memerlukan kartu kredit.
 * Dapatkan API key di https://console.groq.com
 */
class CloudBackend(private val context: Context) : AiBackend {

    override val name = "AI Gratis (Cloud)"

    // Ganti dengan API key gratis milik kamu
    private const val API_KEY = "MASUKKAN_API_KEY_GROQ_DI_SINI"
    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "llama-3.1-8b-instant"

    private val client = OkHttpClient()

    override suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            "API key Groq belum diisi. Edit CloudBackend.kt lalu build ulang."
        } else {
            try {
                val body = JSONObject()
                body.put("model", MODEL)
                body.put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt)
                ))
                body.put("temperature", 0.7)

                val request = Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer $API_KEY")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json")!!))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    val json = JSONObject(response.body!!.string())
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }
            } catch (e: Exception) {
                "Gagal menghubungi AI gratis: ${e.message ?: e.toString()}"
            }
        }
    }

    override fun isAvailable(): Boolean =
        API_KEY.isNotBlank() && API_KEY != "MASUKKAN_API_KEY_GROQ_DI_SINI"
}