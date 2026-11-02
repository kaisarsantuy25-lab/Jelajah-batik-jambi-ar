package com.jelajahbatikjambi.ui.quiz

import com.jelajahbatikjambi.data.model.QuizQuestion
import kotlin.random.Random

/** How many questions one quiz session presents. */
const val QUIZ_QUESTIONS_PER_SESSION = 10

/**
 * Builds one quiz session from [pool]: at most [QUIZ_QUESTIONS_PER_SESSION]
 * questions, shuffled so every new session (including right after a fresh
 * scan) shows a different order and — once the bank holds more than ten
 * questions — a different subset.
 *
 * Pulled out of [QuizViewModel] as a pure function so the selection rule is
 * unit-testable without an Android runtime.
 */
fun buildQuizSession(
    pool: List<QuizQuestion>,
    random: Random = Random.Default
): List<QuizQuestion> = pool.shuffled(random).take(QUIZ_QUESTIONS_PER_SESSION)
