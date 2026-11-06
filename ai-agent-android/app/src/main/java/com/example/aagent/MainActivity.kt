package com.example.aagent

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aagent.backend.*
import com.example.aagent.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentBackend: AiBackend = CloudBackend(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default: Cloud Gratis (pintar)
        binding.rbCloud.isChecked = true
        updateStatus("Mode: ${currentBackend.name}")

        binding.rgMode.setOnCheckedChangeListener { _, checkedId ->
            currentBackend = when (checkedId) {
                binding.rbCloud.id -> CloudBackend(this)
                binding.rbOffline.id -> OfflineBackend(this)
                binding.rbPc.id -> OllamaBackend("http://192.168.1.100:11434") // ganti IP PC
                else -> CloudBackend(this)
            }
            updateStatus("Mode: ${currentBackend.name}\n${if (currentBackend is CloudBackend && !currentBackend.isAvailable()) "⚠ Isi API key Groq dulu!" else ""}")
        }

        binding.btnSend.setOnClickListener {
            val prompt = binding.etInput.text.toString().trim()
            if (prompt.isEmpty()) return@setOnClickListener

            binding.etInput.text.clear()
            appendChat("Kamu: $prompt\n\n")

            if (!currentBackend.isAvailable()) {
                appendChat("AI: Backend ini belum tersedia / belum dikonfigurasi.\n\n")
                return@setOnClickListener
            }

            askAi(prompt)
        }
    }

    private fun askAi(prompt: String) {
        lifecycleScope.launch {
            val answer = withContext(Dispatchers.IO) {
                currentBackend.ask(prompt)
            }
            withContext(Dispatchers.Main) {
                appendChat("AI: $answer\n\n")
            }
        }
    }

    private fun appendChat(text: String) {
        binding.tvChat.append(text)
        binding.svChat.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
    }

    private fun updateStatus(text: String) {
        binding.tvStatus.text = text
    }
}