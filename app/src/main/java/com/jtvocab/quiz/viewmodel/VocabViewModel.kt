package com.jtvocab.quiz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.launch
import com.jtvocab.quiz.model.VocabItem
import com.jtvocab.quiz.model.AppState
import com.jtvocab.quiz.data.VocabRepository

class VocabViewModel : ViewModel() {
    private val _state = mutableStateOf(AppState())
    val state: State<AppState> = _state

    private val _currentQuizBatch = mutableStateOf<List<QuizItem>>(emptyList())
    val currentQuizBatch: State<List<QuizItem>> = _currentQuizBatch

    private val _currentSetIndex = mutableStateOf(0)
    val currentSetIndex: State<Int> = _currentSetIndex

    private val _score = mutableStateOf(0)
    val score: State<Int> = _score

    private val _loadingAI = mutableStateOf(false)
    val loadingAI: State<Boolean> = _loadingAI

    private val _currentInsight = mutableStateOf<com.jtvocab.quiz.data.WordInsight?>(null)
    val currentInsight: State<com.jtvocab.quiz.data.WordInsight?> = _currentInsight

    private val _dailyPulse = mutableStateOf<org.json.JSONObject?>(null)
    val dailyPulse: State<org.json.JSONObject?> = _dailyPulse

    init {
        fetchDailyPulse()
    }

    private fun fetchDailyPulse() {
        viewModelScope.launch {
            _dailyPulse.value = com.jtvocab.quiz.data.AndroidGeminiService.getDailyInsight()
        }
    }

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

    fun setAccentColor(color: Long) {
        _state.value = _state.value.copy(accentColor = color)
    }

    fun setTheme(theme: String) {
        _state.value = _state.value.copy(theme = theme)
    }

    private val _isChallengeMode = mutableStateOf(false)
    val isChallengeMode: State<Boolean> = _isChallengeMode

    private val _challengeDay = mutableStateOf(1)
    val challengeDay: State<Int> = _challengeDay

    fun setChallengeMode(enabled: Boolean, category: String = "ow") {
        _isChallengeMode.value = enabled
        if (enabled) {
            startChallengeQuiz(_challengeDay.value, category)
        }
    }

    fun setChallengeDay(day: Int, category: String) {
        _challengeDay.value = day
        if (_isChallengeMode.value) {
            startChallengeQuiz(day, category)
        }
    }

    fun startChallengeQuiz(day: Int, cat: String) {
        val quotas = mapOf("ow" to 27, "sy" to 24, "id" to 24, "ph" to 7)
        val perDay = quotas[cat] ?: 10
        val list = when(cat) {
            "ow" -> VocabRepository.ows
            "sy" -> VocabRepository.synonyms
            "id" -> VocabRepository.idioms
            "ph" -> VocabRepository.phrasal
            else -> emptyList()
        }
        val start = (day - 1) * perDay
        val end = minOf(start + perDay, list.size)
        val filteredItems = if (start < list.size) list.subList(start, end) else emptyList()

        _currentQuizBatch.value = filteredItems.map { item ->
            val allAnswers = when(item.cat) {
                "ow" -> VocabRepository.ows
                "sy" -> VocabRepository.synonyms
                "ph" -> VocabRepository.phrasal
                else -> VocabRepository.idioms
            }.map { it.a }
            
            val sameInitial = allAnswers.filter { it.startsWith(item.a.take(1), ignoreCase = true) && it != item.a }.shuffled()
            val remaining = allAnswers.filter { !it.startsWith(item.a.take(1), ignoreCase = true) && it != item.a }.shuffled()
            val distractors = (sameInitial + remaining).take(3)
            val options = (distractors + item.a).shuffled()
            QuizItem(item, options)
        }
    }

    fun startQuiz(cat: String, setIndex: Int, isWeakMode: Boolean = false) {
        _currentSetIndex.value = if (isWeakMode) -1 else setIndex
        _score.value = 0
        val items = if (isWeakMode) {
            (_state.value.weakListOW + _state.value.weakListSY + _state.value.weakListID + _state.value.weakListPH).shuffled()
        } else {
            val size = when(cat) {
                "ow" -> 50
                "sy" -> 25
                "id" -> 25
                "ph" -> 10
                else -> 25
            }
            VocabRepository.getItemsForSet(cat, setIndex, size)
        }

        _currentQuizBatch.value = items.map { item ->
            val allAnswers = when(item.cat) {
                "ow" -> VocabRepository.ows
                "sy" -> VocabRepository.synonyms
                "ph" -> VocabRepository.phrasal
                else -> VocabRepository.idioms
            }.map { it.a }
            
            val sameInitial = allAnswers.filter { it.startsWith(item.a.take(1), ignoreCase = true) && it != item.a }.shuffled()
            val remaining = allAnswers.filter { !it.startsWith(item.a.take(1), ignoreCase = true) && it != item.a }.shuffled()
            val distractors = (sameInitial + remaining).take(3)
            val options = (distractors + item.a).shuffled()
            QuizItem(item, options)
        }
    }

    fun clearWeakList() {
        _state.value = _state.value.copy(
            weakListOW = emptyList(),
            weakListSY = emptyList(),
            weakListID = emptyList(),
            weakListPH = emptyList()
        )
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

        if (answer == quizItem.item.a) {
            _score.value += 1
            if (_score.value % 10 == 0) {
                _state.value = _state.value.copy(streak = _state.value.streak + 1)
            }
        } else {
            addToWeakList(quizItem.item)
        }

        // Auto-check if session is finished
        if (currentBatch.all { it.isAnswered }) {
            finishSession()
        }
    }

    fun finishSession() {
        val currentBatch = _currentQuizBatch.value
        if (currentBatch.isEmpty()) return
        
        val correctCount = currentBatch.count { it.selectedOption == it.item.a }
        val totalCount = currentBatch.size
        val successThreshold = totalCount * 0.8
        
        if (correctCount >= successThreshold) {
            if (_isChallengeMode.value) {
                _state.value = _state.value.copy(
                    completedChallengeDays = _state.value.completedChallengeDays + _challengeDay.value
                )
            } else if (_currentSetIndex.value >= 0) {
                val cat = currentBatch.firstOrNull()?.item?.cat ?: ""
                _state.value = when(cat) {
                    "ow" -> _state.value.copy(completedOWS = _state.value.completedOWS + _currentSetIndex.value)
                    "sy" -> _state.value.copy(completedSY = _state.value.completedSY + _currentSetIndex.value)
                    "id" -> _state.value.copy(completedID = _state.value.completedID + _currentSetIndex.value)
                    "ph" -> _state.value.copy(completedPH = _state.value.completedPH + _currentSetIndex.value)
                    else -> _state.value
                }
            }
        }
    }

    private fun addToWeakList(item: VocabItem) {
        val currentState = _state.value
        val newState = currentState.copy(
            weakListOW = if (item.cat == "ow" && !currentState.weakListOW.contains(item)) currentState.weakListOW + item else currentState.weakListOW,
            weakListSY = if (item.cat == "sy" && !currentState.weakListSY.contains(item)) currentState.weakListSY + item else currentState.weakListSY,
            weakListID = if (item.cat == "id" && !currentState.weakListID.contains(item)) currentState.weakListID + item else currentState.weakListID,
            weakListPH = if (item.cat == "ph" && !currentState.weakListPH.contains(item)) currentState.weakListPH + item else currentState.weakListPH
        )
        _state.value = newState
    }
}
