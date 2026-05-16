package com.jtvocab.quiz.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.launch
import com.jtvocab.quiz.model.VocabItem
import com.jtvocab.quiz.model.AppState
import com.jtvocab.quiz.data.VocabRepository
import com.google.gson.Gson

class VocabViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = mutableStateOf(AppState())
    val state: State<AppState> = _state

    private val _currentQuizBatch = mutableStateOf<List<QuizItem>>(emptyList())
    val currentQuizBatch: State<List<QuizItem>> = _currentQuizBatch

    private val _currentSetIndex = mutableStateOf(0)
    val currentSetIndex: State<Int> = _currentSetIndex

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _searchResults = mutableStateOf<List<VocabItem>>(emptyList())
    val searchResults: State<List<VocabItem>> = _searchResults

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        val lowercaseQuery = query.lowercase()
        val allItems = VocabRepository.ows + VocabRepository.synonyms + VocabRepository.idioms + VocabRepository.phrasal
        _searchResults.value = allItems.filter { 
            it.w.contains(lowercaseQuery, ignoreCase = true) || 
            it.a.contains(lowercaseQuery, ignoreCase = true) ||
            it.h.contains(lowercaseQuery, ignoreCase = true)
        }.take(50)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    init {
        loadState()
    }

    private fun loadState() {
        val json = prefs.getString("jt_vocab_state_v1", null)
        if (json != null) {
            try {
                _state.value = gson.fromJson(json, AppState::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveState() {
        prefs.edit().putString("jt_vocab_state_v1", gson.toJson(_state.value)).apply()
    }

    data class QuizItem(
        val item: VocabItem,
        val options: List<String>,
        val isAnswered: Boolean = false,
        val selectedOption: String? = null
    )

    fun setAccentColor(color: Long) {
        _state.value = _state.value.copy(accentColor = color)
        saveState()
    }

    fun setTheme(theme: String) {
        _state.value = _state.value.copy(theme = theme)
        saveState()
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
        _isChallengeMode.value = false
        _currentSetIndex.value = if (isWeakMode) -1 else setIndex
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
        saveState()
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
            val newScore = _state.value.score + 1
            if (newScore % 10 == 0) {
                _state.value = _state.value.copy(score = newScore, streak = _state.value.streak + 1)
            } else {
                _state.value = _state.value.copy(score = newScore)
            }
            saveState()
        } else {
            addToWeakList(quizItem.item)
        }

        // Auto-check if session is finished
        if (currentBatch.all { it.isAnswered }) {
            finishSession()
        }
    }

    fun markSetAsLearned() {
        val currentBatch = _currentQuizBatch.value
        if (currentBatch.isEmpty()) return
        
        if (_isChallengeMode.value) {
            _state.value = _state.value.copy(
                completedChallengeDays = _state.value.completedChallengeDays + _challengeDay.value
            )
            saveState()
        } else if (_currentSetIndex.value >= 0) {
            val cat = currentBatch.firstOrNull()?.item?.cat ?: ""
            _state.value = when(cat) {
                "ow" -> _state.value.copy(completedOWS = _state.value.completedOWS + _currentSetIndex.value)
                "sy" -> _state.value.copy(completedSY = _state.value.completedSY + _currentSetIndex.value)
                "id" -> _state.value.copy(completedID = _state.value.completedID + _currentSetIndex.value)
                "ph" -> _state.value.copy(completedPH = _state.value.completedPH + _currentSetIndex.value)
                else -> _state.value
            }
            saveState()
        }
    }

    fun finishSession() {
        val currentBatch = _currentQuizBatch.value
        val cat = currentBatch.firstOrNull()?.item?.cat ?: "ow"
        val isWeakMode = _currentSetIndex.value == -1
        
        markSetAsLearned()
        
        if (!_isChallengeMode.value && !isWeakMode) {
            val nextSet = _currentSetIndex.value + 1
            val setItemsCount = when(cat) {
                "ow" -> 50
                "sy" -> 25
                "id" -> 25
                "ph" -> 10
                else -> 25
            }
            val listSize = when(cat) {
                "ow" -> VocabRepository.ows.size
                "sy" -> VocabRepository.synonyms.size
                "id" -> VocabRepository.idioms.size
                "ph" -> VocabRepository.phrasal.size
                else -> 0
            }
            val totalSets = if (listSize > 0) (listSize + setItemsCount - 1) / setItemsCount else 0
            
            if (nextSet < totalSets) {
                startQuiz(cat, nextSet)
            } else {
                _currentQuizBatch.value = emptyList()
            }
        } else if (_isChallengeMode.value) {
             // For challenge mode, let user pick next cat or day manually for now
             // Or we could auto-advance day if all cats finished?
             // User just mentioned "next set", so standard sets are priority.
             _currentQuizBatch.value = emptyList()
        } else {
            _currentQuizBatch.value = emptyList()
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
        saveState()
    }
}
