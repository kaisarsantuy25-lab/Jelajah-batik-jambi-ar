package com.jelajahbatikjambi.data.repository

import android.content.res.AssetManager
import com.jelajahbatikjambi.data.model.QuizQuestion
import org.json.JSONArray

private const val BUILT_IN_QUIZ_ASSET_PATH = "data/quiz_questions.json"

/**
 * Loads the bundled quiz bank from `assets/data/quiz_questions.json` — the
 * built-in questions that ship with the app, written for the bundled motifs.
 * User-authored questions still live in Room ([CustomQuizRepository]); the
 * quiz merges both sources.
 *
 * The bank is organised in packages (the `package` field) but the parser
 * deliberately ignores it: [QuizQuestion] only needs the fields the quiz
 * screen reads. New packages are added by appending entries to the JSON.
 */
class BuiltInQuizRepository(private val assetManager: AssetManager) {

    private val allQuestions: List<QuizQuestion> by lazy {
        val json = assetManager.open(BUILT_IN_QUIZ_ASSET_PATH).use { it.reader().readText() }
        parseQuizQuestionsJson(json)
    }

    fun getAll(): List<QuizQuestion> = allQuestions
}

/**
 * Parses the `quiz_questions.json` array into [QuizQuestion]s. A plain
 * function (no [AssetManager]) so it can be covered by a local JVM test.
 *
 * Each entry must carry exactly four options and a correct-option index in
 * range. Failing fast here beats a quiz that silently loses a question or
 * marks every answer wrong because the JSON drifted.
 */
fun parseQuizQuestionsJson(json: String): List<QuizQuestion> {
    val array = JSONArray(json)
    return (0 until array.length()).map { index ->
        val obj = array.getJSONObject(index)
        val optionsJson = obj.getJSONArray("options")
        val options = (0 until optionsJson.length()).map { optionsJson.getString(it) }
        val correctOptionIndex = obj.getInt("correctOptionIndex")
        require(options.size == 4) {
            "Question ${obj.optInt("id", index + 1)} must have exactly 4 options, got ${options.size}"
        }
        require(correctOptionIndex in options.indices) {
            "Question ${obj.optInt("id", index + 1)} has out-of-range correctOptionIndex $correctOptionIndex"
        }
        QuizQuestion(
            batikId = obj.getInt("batikId"),
            prompt = obj.getString("prompt"),
            options = options,
            correctOptionIndex = correctOptionIndex
        )
    }
}
