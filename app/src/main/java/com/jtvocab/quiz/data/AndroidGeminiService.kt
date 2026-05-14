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
        modelName = "gemini-3-flash-preview",
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
            null
        }
    }

    suspend fun generatePQRS(): List<com.jtvocab.quiz.model.PQRSQuestion> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext emptyList()
        val prompt = """
            Generate 5 EXTREMELY CHALLENGING Parajumble (PQRS) questions for SSC CGL Tier 2 (Last Mile Challenge).
            Topic: Scientific breakthroughs, Philosophy, Global Economics, or High-level Literature.
            Include S1 (Fixed Start) and S6 (Fixed End) for all.
            Sentences P, Q, R, S must be complex, logically dense, and use advanced connectors (However, Moreover, Consequently, Albeit).
            Return ONLY a JSON array of objects with keys: "id", "s1", "s6", "sentences" (array of 4 strings labeled P:, Q:, R:, S:), "correctSequence" (e.g. "PRSQ"), "explanation".
        """.trimIndent()
        try {
            val response = model.generateContent(prompt)
            val array = org.json.JSONArray(response.text ?: "[]")
            val list = mutableListOf<com.jtvocab.quiz.model.PQRSQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sentencesArr = obj.getJSONArray("sentences")
                val sentences = List(sentencesArr.length()) { sentencesArr.getString(it) }
                list.add(com.jtvocab.quiz.model.PQRSQuestion(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    s1 = obj.optString("s1", null),
                    s6 = obj.optString("s6", null),
                    sentences = sentences,
                    correctSequence = obj.getString("correctSequence"),
                    explanation = obj.getString("explanation")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    suspend fun generateCloze(): List<com.jtvocab.quiz.model.ClozeQuestion> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext emptyList()
        val prompt = """
            Generate 2 Advanced Cloze Test passages (150-200 words each) for SSC CGL Tier 2 (Last Mile Level).
            Difficulty: HIGH (Focus on vocabulary found in The Hindu editorials or Scientific journals).
            Use words like 'obfuscate', 'recalcitrant', 'infinitesimal', 'paradigm', 'synergy'.
            Each passage should have 5 blanks.
            Return ONLY a JSON array of objects with keys: "id", "passage" (with (1), (2), etc. labels), "blanks" (array of objects with "index", "options", "answer", "explanation").
        """.trimIndent()
        try {
            val response = model.generateContent(prompt)
            val array = org.json.JSONArray(response.text ?: "[]")
            val list = mutableListOf<com.jtvocab.quiz.model.ClozeQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val blanksArr = obj.getJSONArray("blanks")
                val blanks = mutableListOf<com.jtvocab.quiz.model.ClozeQuestion.Blank>()
                for (j in 0 until blanksArr.length()) {
                    val bObj = blanksArr.getJSONObject(j)
                    val optsArr = bObj.getJSONArray("options")
                    blanks.add(com.jtvocab.quiz.model.ClozeQuestion.Blank(
                        index = bObj.getInt("index"),
                        options = List(optsArr.length()) { optsArr.getString(it) },
                        answer = bObj.getString("answer"),
                        explanation = bObj.getString("explanation")
                    ))
                }
                list.add(com.jtvocab.quiz.model.ClozeQuestion(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    passage = obj.getString("passage"),
                    blanks = blanks
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    suspend fun generateRC(): com.jtvocab.quiz.model.RCQuestion? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext null
        val prompt = """
            Generate 1 LONG High Difficulty Reading Comprehension passage (400-500 words) for SSC CGL Tier 2.
            Topic: Geopolitics, Quantum Physics simplified, Global Economics, or History of Enlightenment.
            Include 5 complex questions requiring deep inference.
            Provide:
            1 Title/Theme question, 1 Inference question, 1 Fact-based question, 1 Vocabulary question (Synonym/Antonym from context), 1 Tone/Style question.
            For each question, provide a "Smart Explanation" highlighting logical connectors or context clues.
            Return ONLY a JSON object with keys: "id", "passage", "questions" (array of objects with "q", "options", "a", "explanation").
        """.trimIndent()
        try {
            val response = model.generateContent(prompt)
            val cleanedResponse = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
            val obj = JSONObject(cleanedResponse)
            val qsArr = obj.getJSONArray("questions")
            val qs = mutableListOf<com.jtvocab.quiz.model.RCQuestion.RCSubQuestion>()
            for (i in 0 until qsArr.length()) {
                val qObj = qsArr.getJSONObject(i)
                val optsArr = qObj.getJSONArray("options")
                qs.add(com.jtvocab.quiz.model.RCQuestion.RCSubQuestion(
                    q = qObj.getString("q"),
                    options = List(optsArr.length()) { optsArr.getString(it) },
                    a = qObj.getString("a"),
                    explanation = qObj.getString("explanation")
                ))
            }
            com.jtvocab.quiz.model.RCQuestion(
                id = obj.optString("id", UUID.randomUUID().toString()),
                passage = obj.getString("passage"),
                questions = qs
            )
        } catch (e: Exception) { null }
    }
}
