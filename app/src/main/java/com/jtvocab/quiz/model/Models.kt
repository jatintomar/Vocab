package com.jtvocab.quiz.model

data class VocabItem(
    val id: String,
    val w: String, // Word or Definition
    val a: String, // Answer
    val h: String, // Hindi
    val cat: String // ow, sy, id
)

data class Achievement(
    val id: String,
    val title: String,
    val icon: String,
    val unlocked: Boolean = false
)

data class AppState(
    val streak: Int = 3,
    val completedOWS: Set<Int> = emptySet(),
    val completedSY: Set<Int> = emptySet(),
    val completedID: Set<Int> = emptySet(),
    val weakListOW: List<VocabItem> = emptyList(),
    val weakListSY: List<VocabItem> = emptyList(),
    val weakListID: List<VocabItem> = emptyList(),
    val accentColor: Long = 0xFF6366F1, // Default indigo
    val theme: String = "Nordic",
    val achievements: List<Achievement> = listOf(
        Achievement("early_bird", "Early Bird", "🌅"),
        Achievement("streak_3", "Fire Starter", "🔥"),
        Achievement("master_50", "Word Smith", "✍️")
    )
)

data class PQRSQuestion(
    val id: String,
    val s1: String? = null,
    val s6: String? = null,
    val sentences: List<String>,
    val correctSequence: String,
    val explanation: String
)

data class ClozeQuestion(
    val id: String,
    val passage: String,
    val blanks: List<Blank>
) {
    data class Blank(
        val index: Int,
        val options: List<String>,
        val answer: String,
        val explanation: String
    )
}
