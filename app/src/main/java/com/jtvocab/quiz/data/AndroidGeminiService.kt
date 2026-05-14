package com.jtvocab.quiz.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class WordInsight(
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
        if (apiKey.isEmpty()) return@withContext null
        
        val prompt = """
            Analyze the following English word: "${word}" (Category: ${category}).
            Provide:
            1. An usage context (sentence) specifically for SSC Exam 2026.
            2. A powerful mnemonic (memory trick) to remember it forever.
            3. 3 synonyms.
            Return ONLY a valid JSON object with keys: "context", "mnemonic", "synonyms" (array).
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val json = JSONObject(response.text ?: "{}")
            
            WordInsight(
                context = json.optString("context", "No context available"),
                mnemonic = json.optString("mnemonic", "No mnemonic available"),
                synonyms = (0 until json.optJSONArray("synonyms")?.length()!!).map { 
                    json.optJSONArray("synonyms")?.optString(it) ?: ""
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
