package com.jtvocab.quiz.model

data class VocabItem(
    val id: String,
    val w: String, // Word or Definition
    val a: String, // Answer
    val h: String, // Hindi
    val cat: String // ow, sy, id
)

data class AppState(
    val streak: Int = 0,
    val completedOWS: Set<Int> = emptySet(),
    val completedSY: Set<Int> = emptySet(),
    val completedID: Set<Int> = emptySet(),
    val weakListOW: List<VocabItem> = emptyList(),
    val weakListSY: List<VocabItem> = emptyList(),
    val weakListID: List<VocabItem> = emptyList()
)
