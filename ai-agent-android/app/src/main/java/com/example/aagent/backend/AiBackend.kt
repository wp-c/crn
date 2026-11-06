package com.example.aagent.backend

/**
 * Setiap backend AI harus mengimplementasikan antarmuka ini.
 * Mode yang tersedia: Cloud (gratis), PC (Ollama), Offline (on-device).
 */
interface AiBackend {
    val name: String
    suspend fun ask(prompt: String): String
    fun isAvailable(): Boolean
}