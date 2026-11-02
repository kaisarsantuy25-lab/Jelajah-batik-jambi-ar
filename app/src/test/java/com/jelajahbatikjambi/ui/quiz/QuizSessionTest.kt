package com.jelajahbatikjambi.ui.quiz

import com.jelajahbatikjambi.data.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

/**
 * Covers the session selection rule: ten questions at most, no duplicates,
 * and a different order/subset for a new session. Pure JVM logic, so no
 * Android runtime is needed.
 */
class QuizSessionTest {

    private fun question(index: Int) = QuizQuestion(
        batikId = index % 6 + 1,
        prompt = "Pertanyaan $index",
        options = listOf("A", "B", "C", "D"),
        correctOptionIndex = 0
    )

    @Test
    fun `caps a session at ten questions`() {
        val session = buildQuizSession((1..30).map(::question), Random(1))

        assertEquals(QUIZ_QUESTIONS_PER_SESSION, session.size)
    }

    @Test
    fun `shows every question when the pool is smaller than ten`() {
        val pool = (1..4).map(::question)

        assertEquals(pool.toSet(), buildQuizSession(pool, Random(1)).toSet())
    }

    @Test
    fun `never repeats a question inside one session`() {
        val session = buildQuizSession((1..30).map(::question), Random(1))

        assertEquals(session.size, session.toSet().size)
    }

    @Test
    fun `a new session gets a different order and subset`() {
        val pool = (1..30).map(::question)

        val first = buildQuizSession(pool, Random(1)).map { it.prompt }
        val second = buildQuizSession(pool, Random(2)).map { it.prompt }

        assertNotEquals(first, second)
    }
}
