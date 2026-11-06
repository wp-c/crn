package com.example.aiagent

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aiagent.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.use
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var client: OkHttpClient

    // ================= KONFIGURASI =================
    // Pilih salah satu opsi di bawah ini:

    // --- Opsi A: Groq (gratis, butuh API key) ---
    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val API_KEY = "MASUKKAN_API_KEY_GRATIS_GROQ_KAMU_DI_SINI"
    private const val MODEL = "llama-3.1-8b-instant"

    // --- Opsi B: Ollama di PC lokal (100% gratis, tanpa API key) ---
    // private const val API_URL = "http://192.168.1.100:11434/api/chat" // ganti IP PC kamu
    // private const val API_KEY = ""
    // private const val MODEL = "llama3.2"
    // ===============================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        client = OkHttpClient()

        binding.tvChat.text = "Agen AI siap digunakan.\nKetik pesan di bawah ini.\n\n"

        binding.btnSend.setOnClickListener {
            val prompt = binding.etInput.text.toString().trim()
            if (prompt.isEmpty()) return@setOnClickListener
            binding.etInput.text.clear()
            appendChat("Kamu: $prompt\n\n")
            askAi(prompt)
        }
    }

    private fun appendChat(text: String) {
        binding.tvChat.append(text)
        binding.svChat.fullScroll(View.FOCUS_DOWN)
    }

    private fun askAi(prompt: String) {
        lifecycleScope.launch {
            val raw = withContext(Dispatchers.IO) {
                try {
                    val body = JSONObject()
                    body.put("model", MODEL)
                    val messages = JSONArray()
                    messages.put(JSONObject().put("role", "user").put("content", prompt))
                    body.put("messages", messages)
                    body.put("temperature", 0.7)

                    val request = Request.Builder()
                        .url(API_URL)
                        .header("Content-Type", "application/json")
                        .apply {
                            if (API_KEY.isNotBlank()) {
                                header("Authorization", "Bearer $API_KEY")
                            }
                        }
                        .post(RequestBody.create(body.toString(), MediaType.get("application/json")!!))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}")
                        }
                        response.body?.string() ?: ""
                    }
                } catch (e: Exception) {
                    "Error: ${e.message ?: e.toString()}"
                }
            }

            val aiText = withContext(Dispatchers.Main) {
                extractAnswer(raw)
            }
            appendChat("AI: $aiText\n\n")
        }
    }

    private fun extractAnswer(raw: String): String {
        return try {
            // Format Groq / OpenAI-compatible
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            try {
                // Format Ollama (non-stream)
                JSONObject(raw).getJSONObject("message").getString("content")
            } catch (e2: Exception) {
                raw
            }
        }
    }
}