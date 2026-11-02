package com.jelajahbatikjambi.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Exercises [parseQuizQuestionsJson] directly with hardcoded JSON — no
 * [android.content.res.AssetManager] involved, so it runs as a plain local
 * JVM test (the standalone `org.json:json` test dependency supplies the
 * parser, like [BatikJsonParsingTest]).
 */
class BuiltInQuizParsingTest {

    @Test
    fun `parses every field of a built-in question`() {
        val json = """
            [
              {
                "id": 1,
                "package": 3,
                "batikId": 1,
                "prompt": "Sikap yang sesuai dengan filosofi motif Angso Duo adalah apa?",
                "options": [
                  "Menjaga keharmonisan hubungan antarmakhluk hidup",
                  "Mengutamakan kepentingan pribadi",
                  "Menghindari kerja sama",
                  "Mudah menyerah saat menghadapi kesulitan"
                ],
                "correctOptionIndex": 0
              }
            ]
        """.trimIndent()

        val result = parseQuizQuestionsJson(json)

        assertEquals(1, result.size)
        assertEquals(1, result.first().batikId)
        assertEquals(
            "Sikap yang sesuai dengan filosofi motif Angso Duo adalah apa?",
            result.first().prompt
        )
        assertEquals(4, result.first().options.size)
        assertEquals("Menghindari kerja sama", result.first().options[2])
        assertEquals(0, result.first().correctOptionIndex)
    }

    @Test
    fun `keeps the given answer index when the correct option is not first`() {
        val json = """
            [
              {
                "batikId": 6,
                "prompt": "Pesan kehidupan yang terkandung dalam filosofi Durian Pecah adalah apa?",
                "options": ["A", "B", "C", "D"],
                "correctOptionIndex": 2
              }
            ]
        """.trimIndent()

        assertEquals(2, parseQuizQuestionsJson(json).first().correctOptionIndex)
    }

    @Test
    fun `rejects an entry that does not have exactly four options`() {
        val json = """
            [
              {
                "batikId": 1,
                "prompt": "Soal rusak",
                "options": ["A", "B", "C"],
                "correctOptionIndex": 0
              }
            ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parseQuizQuestionsJson(json)
        }
    }

    @Test
    fun `rejects an out-of-range correct option index`() {
        val json = """
            [
              {
                "batikId": 1,
                "prompt": "Soal rusak",
                "options": ["A", "B", "C", "D"],
                "correctOptionIndex": 4
              }
            ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parseQuizQuestionsJson(json)
        }
    }
}
