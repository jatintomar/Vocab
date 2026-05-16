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
    val streak: Int = 0,
    val score: Int = 0,
    val completedOWS: Set<Int> = emptySet(),
    val completedSY: Set<Int> = emptySet(),
    val completedID: Set<Int> = emptySet(),
    val completedPH: Set<Int> = emptySet(),
    val completedChallengeDays: Set<Int> = emptySet(),
    val weakListOW: List<VocabItem> = emptyList(),
    val weakListSY: List<VocabItem> = emptyList(),
    val weakListID: List<VocabItem> = emptyList(),
    val weakListPH: List<VocabItem> = emptyList(),
    val accentColor: Long = 0xFF3B82F6, // Standard blue
    val theme: String = "Nordic",
    val achievements: List<Achievement> = listOf(
        Achievement("early_bird", "Early Bird", "🌅"),
        Achievement("streak_3", "Fire Starter", "🔥"),
        Achievement("master_50", "Word Smith", "✍️")
    )
)


