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

    suspend fun generateAllComp(): Triple<List<com.jtvocab.quiz.model.PQRSQuestion>, List<com.jtvocab.quiz.model.ClozeQuestion>, com.jtvocab.quiz.model.RCQuestion?> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext Triple(emptyList(), emptyList(), null)
        
        val prompt = """
            Generate exactly 3 parajumble (PQRS) questions, exactly 1 Advanced Cloze Test passage, and exactly 1 Reading Comprehension passage for SSC CGL 2026.
            
            Return ONLY a valid JSON object with matches the following structure:
            {
              "pqrs": [ 
                { "id": "uuid", "s1": "start text", "s6": "end text", "sentences": ["P...", "Q...", "R...", "S..."], "correctSequence": "PQRS", "explanation": "text", "logicalConnectors": ["pivot", "next"] }
              ],
              "cloze": [
                { "id": "uuid", "passage": "text with (1), (2)...", "blanks": [ { "index": 1, "options": ["A", "B", "C", "D"], "answer": "correct", "explanation": "why" } ] }
              ],
              "rc": {
                "id": "uuid", "passage": "long text", "questions": [ { "q": "text", "options": ["A", "B", "C", "D"], "a": "correct", "explanation": "why" } ]
              }
            }
            Ensure the quality reflects the 2026 SSC CGL pattern.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val cleaned = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
            val root = JSONObject(cleaned)
            
            // Parse PQRS
            val pqrsList = mutableListOf<com.jtvocab.quiz.model.PQRSQuestion>()
            val pqrsArr = root.optJSONArray("pqrs")
            if (pqrsArr != null) {
                for (i in 0 until pqrsArr.length()) {
                    val obj = pqrsArr.getJSONObject(i)
                    val sArr = obj.getJSONArray("sentences")
                    val connectorsArr = obj.optJSONArray("logicalConnectors")
                    pqrsList.add(com.jtvocab.quiz.model.PQRSQuestion(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        s1 = obj.optString("s1", null),
                        s6 = obj.optString("s6", null),
                        sentences = List(sArr.length()) { sArr.getString(it) },
                        correctSequence = obj.getString("correctSequence"),
                        explanation = obj.getString("explanation"),
                        logicalConnectors = if(connectorsArr != null) List(connectorsArr.length()) { connectorsArr.getString(it) } else emptyList()
                    ))
                }
            }

            // Parse Cloze
            val clozeList = mutableListOf<com.jtvocab.quiz.model.ClozeQuestion>()
            val clozeArr = root.optJSONArray("cloze")
            if (clozeArr != null) {
                for (i in 0 until clozeArr.length()) {
                    val obj = clozeArr.getJSONObject(i)
                    val bArr = obj.getJSONArray("blanks")
                    val blanks = List(bArr.length()) { j ->
                        val bObj = bArr.getJSONObject(j)
                        val oArr = bObj.getJSONArray("options")
                        com.jtvocab.quiz.model.ClozeQuestion.Blank(
                            index = bObj.getInt("index"),
                            options = List(oArr.length()) { oArr.getString(it) },
                            answer = bObj.getString("answer"),
                            explanation = bObj.getString("explanation")
                        )
                    }
                    clozeList.add(com.jtvocab.quiz.model.ClozeQuestion(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        passage = obj.getString("passage"),
                        blanks = blanks
                    ))
                }
            }

            // Parse RC
            var rcResult: com.jtvocab.quiz.model.RCQuestion? = null
            val rcObj = root.optJSONObject("rc")
            if (rcObj != null) {
                val qsArr = rcObj.getJSONArray("questions")
                val qs = List(qsArr.length()) { i ->
                    val qObj = qsArr.getJSONObject(i)
                    val oArr = qObj.getJSONArray("options")
                    com.jtvocab.quiz.model.RCQuestion.RCSubQuestion(
                        q = qObj.getString("q"),
                        options = List(oArr.length()) { oArr.getString(it) },
                        a = qObj.getString("a"),
                        explanation = qObj.getString("explanation")
                    )
                }
                rcResult = com.jtvocab.quiz.model.RCQuestion(
                    id = rcObj.optString("id", UUID.randomUUID().toString()),
                    passage = rcObj.getString("passage"),
                    questions = qs
                )
            }

            Triple(pqrsList, clozeList, rcResult)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple(com.jtvocab.quiz.data.VocabRepository.dailyPQRS, com.jtvocab.quiz.data.VocabRepository.dailyCloze, com.jtvocab.quiz.data.VocabRepository.dailyRC)
        }
    }

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

    suspend fun generatePQRS(): List<com.jtvocab.quiz.model.PQRSQuestion> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext emptyList()
        val prompt = """
            Generate 3 CONCISE but CHALLENGING Parajumble (PQRS) questions for SSC CGL 2026.
            Include S1 (Start) and S6 (End).
            Return ONLY a JSON array of objects with keys: "id", "s1", "s6", "sentences" (array of 4), "correctSequence", "explanation", "logicalConnectors" (array).
        """.trimIndent()
        try {
            val response = model.generateContent(prompt)
            val array = org.json.JSONArray(response.text ?: "[]")
            val list = mutableListOf<com.jtvocab.quiz.model.PQRSQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sentencesArr = obj.getJSONArray("sentences")
                val sentences = List(sentencesArr.length()) { sentencesArr.getString(it) }
                val connectorsArr = obj.optJSONArray("logicalConnectors")
                val connectors = if (connectorsArr != null) List(connectorsArr.length()) { connectorsArr.getString(it) } else emptyList()
                list.add(com.jtvocab.quiz.model.PQRSQuestion(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    s1 = obj.optString("s1", null),
                    s6 = obj.optString("s6", null),
                    sentences = sentences,
                    correctSequence = obj.getString("correctSequence"),
                    explanation = obj.getString("explanation"),
                    logicalConnectors = connectors
                ))
            }
            list
        } catch (e: Exception) { 
            com.jtvocab.quiz.data.VocabRepository.dailyPQRS 
        }
    }

    suspend fun generateCloze(): List<com.jtvocab.quiz.model.ClozeQuestion> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext emptyList()
        val prompt = """
            Generate 1 Advanced Cloze Test passage (150 words) for SSC 2026 Pattern.
            Return ONLY a JSON array of objects with keys: "id", "passage", "blanks" (array of index, options, answer, explanation).
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
        } catch (e: Exception) { 
            com.jtvocab.quiz.data.VocabRepository.dailyCloze 
        }
    }

    suspend fun generateRC(): com.jtvocab.quiz.model.RCQuestion? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext null
        val prompt = """
            Generate 1 LONG Reading Comprehension passage (450-600 words) for SSC CGL/CHSL 2026 Pattern.
            Topic: Advanced Geopolitics, Quantum Physics, Global Economics, or Modern Philosophy.
            Include exactly 5 complex questions:
            1. Theme/Main Idea question.
            2. High-level Inference question.
            3. Direct Fact discovery question.
            4. Contextual Vocabulary (Finding synonyms/antonyms).
            5. Author's Tone or Passage Style.
            
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
        } catch (e: Exception) { 
            com.jtvocab.quiz.data.VocabRepository.dailyRC 
        }
    }
}
