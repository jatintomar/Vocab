package com.jtvocab.quiz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.launch
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

    private val _dailyPQRS = mutableStateOf(VocabRepository.dailyPQRS)
    val dailyPQRS: State<List<PQRSQuestion>> = _dailyPQRS

    private val _dailyCloze = mutableStateOf(VocabRepository.dailyCloze)
    val dailyCloze: State<List<ClozeQuestion>> = _dailyCloze

    private val _dailyRC = mutableStateOf(VocabRepository.dailyRC)
    val dailyRC: State<com.jtvocab.quiz.model.RCQuestion> = _dailyRC

    private val _loadingAI = mutableStateOf(false)
    val loadingAI: State<Boolean> = _loadingAI

    private val _currentInsight = mutableStateOf<com.jtvocab.quiz.data.WordInsight?>(null)
    val currentInsight: State<com.jtvocab.quiz.data.WordInsight?> = _currentInsight

    fun fetchInsight(word: String, category: String) {
        viewModelScope.launch {
            _loadingAI.value = true
            _currentInsight.value = com.jtvocab.quiz.data.AndroidGeminiService.getWordInsight(word, category)
            _loadingAI.value = false
        }
    }

    fun clearInsight() {
        _currentInsight.value = null
    }

    data class QuizItem(
        val item: VocabItem,
        val options: List<String>,
        val isAnswered: Boolean = false,
        val selectedOption: String? = null
    )

    private val _currentSetIndex = mutableStateOf(0)
    val currentSetIndex: State<Int> = _currentSetIndex

    fun setAccentColor(color: Long) {
        _state.value = _state.value.copy(accentColor = color)
    }

    fun setTheme(theme: String) {
        _state.value = _state.value.copy(theme = theme)
    }

    fun startQuiz(cat: String, setIndex: Int, isWeakMode: Boolean = false) {
        _currentSetIndex.value = setIndex
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

        currentBatch[quizIndex] = quizItem.copy(
            isAnswered = true,
            selectedOption = answer
        )
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
