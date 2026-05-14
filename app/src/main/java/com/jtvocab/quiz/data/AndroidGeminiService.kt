package com.jtvocab.quiz.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class WordInsight(
    val word: String,
    val context: String,
    val mnemonic: String,
    val synonyms: List<String>
)

object AndroidGeminiService {
    // API KEY will be fetched from protected environment or user config
    private val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun getWordInsight(word: String, category: String): WordInsight? = withContext(Dispatchers.IO) {
        val fallback = WordInsight(
            word = word,
            context = "Commonly used in competitive exams to test vocabulary depth.",
            mnemonic = "Connect this with a similar sounding familiar word for easier recall.",
            synonyms = emptyList()
        )

        if (apiKey.isEmpty()) return@withContext fallback
        
        val prompt = """
            Analyze the following English word: "${word}" (Category: ${category}).
            Provide:
            1. An usage context (sentence) specifically for SSC CGL Exam.
            2. A powerful mnemonic (memory trick) to remember it forever.
            Return ONLY a valid JSON object with keys: "context", "mnemonic".
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val json = JSONObject(response.text ?: "{}")
            
            WordInsight(
                word = word,
                context = json.optString("context", "No context available"),
                mnemonic = json.optString("mnemonic", "No mnemonic available"),
                synonyms = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            fallback
        }
    }

    suspend fun getDailyInsight(): JSONObject? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext null
        val prompt = """
            Provide a "Daily Vocabulary Pulse" for an SSC aspirant preparing for 2026.
            Pick one very important high-yield word and provide:
            1. The word itself.
            2. A "Power Insight": Why this word is crucial specifically for competitive exams.
            3. A "Modern Usage": How it might appear in a current 2026 news context.
            Return ONLY a valid JSON object with keys: "word", "insight", "usage".
        """.trimIndent()
        try {
            val response = model.generateContent(prompt)
            JSONObject(response.text ?: "{}")
        } catch (e: Exception) { 
            // Fallback to a simple static pulse if API fails
            JSONObject().apply {
                put("word", "Resilient")
                put("insight", "Essential for competitive exams where setbacks are common.")
                put("usage", "The aspirant showed resilient spirit despite technical glitches.")
            }
        }
    }
}
