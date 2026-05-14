package com.jtvocab.quiz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.jtvocab.quiz.model.VocabItem
import com.jtvocab.quiz.model.AppState
import com.jtvocab.quiz.model.PQRSQuestion
import com.jtvocab.quiz.model.ClozeQuestion
import com.jtvocab.quiz.data.VocabRepository

class VocabViewModel : ViewModel() {
    private val _state = mutableStateOf(AppState())
    val state: State<AppState> = _state

    private val _currentQuizBatch = mutableStateOf<List<QuizItem>>(emptyList())
    val currentQuizBatch: State<List<QuizItem>> = _currentQuizBatch

    private val _dailyPQRS = mutableStateOf(VocabRepository.dailyPQRS[0])
    val dailyPQRS: State<PQRSQuestion> = _dailyPQRS

    private val _dailyCloze = mutableStateOf(VocabRepository.dailyCloze[0])
    val dailyCloze: State<ClozeQuestion> = _dailyCloze

    data class QuizItem(
        val item: VocabItem,
        val options: List<String>,
        var isAnswered: Boolean = false,
        var selectedOption: String? = null
    )

    fun startQuiz(cat: String, setIndex: Int, isWeakMode: Boolean = false) {
        val items = if (isWeakMode) {
            when(cat) {
                "ow" -> _state.value.weakListOW
                "sy" -> _state.value.weakListSY
                else -> _state.value.weakListID
            }
        } else {
            val size = if (cat == "ow") 50 else 25
            VocabRepository.getItemsForSet(cat, setIndex, size)
        }

        _currentQuizBatch.value = items.map { item ->
            val allAnswers = when(cat) {
                "ow" -> VocabRepository.ows
                "sy" -> VocabRepository.synonyms
                else -> VocabRepository.idioms
            }.map { it.a }
            
            val distractors = allAnswers.filter { it != item.a }.shuffled().take(3)
            val options = (distractors + item.a).shuffled()
            QuizItem(item, options)
        }
    }

    fun submitAnswer(quizIndex: Int, answer: String) {
        val currentBatch = _currentQuizBatch.value.toMutableList()
        val quizItem = currentBatch[quizIndex]
        if (quizItem.isAnswered) return

        quizItem.isAnswered = true
        quizItem.selectedOption = answer
        _currentQuizBatch.value = currentBatch

        if (answer != quizItem.item.a) {
            addToWeakList(quizItem.item)
        }
    }

    private fun addToWeakList(item: VocabItem) {
        val currentState = _state.value
        val newState = currentState.copy(
            weakListOW = if (item.cat == "ow" && !currentState.weakListOW.contains(item)) currentState.weakListOW + item else currentState.weakListOW,
            weakListSY = if (item.cat == "sy" && !currentState.weakListSY.contains(item)) currentState.weakListSY + item else currentState.weakListSY,
            weakListID = if (item.cat == "id" && !currentState.weakListID.contains(item)) currentState.weakListID + item else currentState.weakListID
        )
        _state.value = newState
    }
}
