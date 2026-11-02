package com.jelajahbatikjambi.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jelajahbatikjambi.data.model.QuizQuestion
import com.jelajahbatikjambi.data.repository.BuiltInQuizRepository
import com.jelajahbatikjambi.data.repository.CustomQuizRepository
import com.jelajahbatikjambi.database.AppDatabase
import com.jelajahbatikjambi.ui.common.SoundEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizUiState(
    val isLoading: Boolean = true,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val score: Int = 0,
    val isFinished: Boolean = false
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
}

/**
 * Drives a single quiz session. The pool is the bundled quiz bank
 * (`assets/data/quiz_questions.json`, the app's own questions for the bundled
 * motifs) plus every question the user authored ([CreateQuizScreen]). The
 * session snapshots **10 shuffled questions** from that pool at start, so a
 * new scan gets a fresh random order and — once the bank grows past ten
 * questions — a fresh subset too (§ user request: "soal yang tampil ada 10
 * tapi di acak setiap scan").
 *
 * The questions are deliberately mixed across motifs rather than filtered to
 * the scanned motif (§ user request, chosen explicitly): scanning any motif
 * starts a quiz of ten random questions from the whole bank. Built-in
 * questions still carry a `batikId` so the bank stays accurate and can be
 * scoped later if that behaviour is wanted.
 *
 * The session is a snapshot, not a live Flow, so the set/shuffle stays stable
 * for the whole round even if questions are edited elsewhere — *unless* the
 * stored set itself changes while the screen is in the background, which is
 * what happens when the user leaves through Kelola Soal Kuis:
 * [reloadIfChanged] then rebuilds the session so the new question is actually
 * included.
 */
class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val customQuizRepository =
        CustomQuizRepository(AppDatabase.getInstance(application).customQuizQuestionDao())
    private val builtInQuizRepository = BuiltInQuizRepository(application.assets)

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    /**
     * The last full pool a session was built from. Kept separately from
     * [QuizUiState.questions] because the session holds only the ten picked
     * questions — comparing the capped session against the full pool would
     * look like a change on every resume and reset the user's progress.
     */
    private var loadedPool: List<QuizQuestion> = emptyList()

    init {
        load()
    }

    /**
     * Builds the question set from scratch. Split out of [init] so "Main
     * Lagi" can call it again: the fresh read is what re-shuffles the order
     * (and re-picks the ten questions) and picks up anything the user created
     * or deleted since the session started, keeping the "stable for the whole
     * session" snapshot rule intact — it just defines what a *new* session
     * snapshots.
     */
    private fun load() {
        viewModelScope.launch {
            _uiState.value = QuizUiState(isLoading = true)
            val pool = readPool()
            loadedPool = pool
            _uiState.value = QuizUiState(isLoading = false, questions = buildQuizSession(pool))
        }
    }

    /**
     * Called when the quiz screen returns to the foreground — the user just
     * came back from the "Buat Soal" form or from the manage screen.
     * The pool is read fresh and compared with the last snapshot: when the
     * two match, nothing changed, so the session carries on untouched (same
     * shuffle, score, progress); when they differ — a question was added,
     * edited or deleted while away — the session is rebuilt from that read,
     * so a newly added question is included immediately instead of only
     * after "Main Lagi".
     */
    fun reloadIfChanged() {
        // A load is already in flight; it reads whatever is stored by the time
        // it runs, so this call could only risk overwriting it with a staler set.
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            val stored = readPool()
            val current = _uiState.value
            // Same guard as above, re-checked because a load may have started
            // while this read was in flight.
            if (current.isLoading || hasSameQuestions(loadedPool, stored)) return@launch
            loadedPool = stored
            _uiState.value = QuizUiState(isLoading = false, questions = buildQuizSession(stored))
        }
    }

    /**
     * "Main Lagi": a brand-new session — score, progress and answer state are
     * dropped, and [load] re-reads everything so the round is freshly
     * shuffled and re-picked. If the rebuilt set turns out empty (e.g. the
     * bank asset is missing and every custom question was deleted
     * mid-session) the UI falls through to its existing empty state rather
     * than a 0-question quiz.
     */
    fun restart() {
        load()
    }

    fun submitAnswer(optionIndex: Int) {
        val state = _uiState.value
        if (state.selectedOptionIndex != null) return // already answered this question
        val question = state.currentQuestion ?: return

        val isCorrect = optionIndex == question.correctOptionIndex
        if (isCorrect) SoundEffects.playSuccess() else SoundEffects.playError()
        _uiState.value = state.copy(
            selectedOptionIndex = optionIndex,
            score = state.score + if (isCorrect) 1 else 0
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        _uiState.value = if (nextIndex >= state.questions.size) {
            state.copy(isFinished = true)
        } else {
            state.copy(currentIndex = nextIndex, selectedOptionIndex = null)
        }
    }

    private suspend fun readPool(): List<QuizQuestion> =
        builtInQuizRepository.getAll() + customQuizRepository.getAllOnceAsQuizQuestions()

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuizViewModel(application) as T
            }
    }
}

/**
 * Multiset comparison of two question sets: order never matters — sessions
 * are shuffled anyway — but duplicate rows do, so adding a second question
 * with the same wording still counts as the change [QuizViewModel.reloadIfChanged]
 * has to pick up.
 */
private fun hasSameQuestions(a: List<QuizQuestion>, b: List<QuizQuestion>): Boolean {
    if (a.size != b.size) return false
    val unmatched = a.groupingBy { it }.eachCount().toMutableMap()
    for (question in b) {
        val left = unmatched[question] ?: return false
        if (left == 1) unmatched.remove(question) else unmatched[question] = left - 1
    }
    return true
}
