package com.example.ui.viewmodel

import com.example.model.QuestionStyle
import com.example.model.QuizQuestion

data class QuizSessionState(
    val deck: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val runningScore: Int = 0,
    val quizFinished: Boolean = false,
    val chosenAnswer: String? = null,
    val tfSelections: Map<Int, Boolean?> = emptyMap(),
    val tfChecked: Boolean = false,
    val tfQuestionScore: Int = 0,
    val saInputText: String = "",
    val saChecked: Boolean = false,
    val showSeqAnswer: Boolean = false,
    val seqUserNotes: String = ""
) {
    val currentQuestion: QuizQuestion?
        get() = deck.getOrNull(currentIndex)

    fun maxPossibleScore(overrideStyle: QuestionStyle? = null): Int {
        if (deck.isEmpty()) return 0
        val style = overrideStyle ?: currentQuestion?.detectedStyle() ?: QuestionStyle.MCQ
        return when (style) {
            QuestionStyle.TF -> deck.size * 5
            QuestionStyle.SEQ -> deck.size * 10
            QuestionStyle.MCQ, QuestionStyle.SA -> deck.size
        }
    }
}

fun QuizSessionState.selectMcqOption(option: String, correctAnswer: String?): QuizSessionState {
    return selectMcqOption(option, listOfNotNull(correctAnswer))
}

fun QuizSessionState.selectMcqOption(option: String, correctResponses: List<String>): QuizSessionState {
    val isCorrect = correctResponses.any { it.equals(option, ignoreCase = true) }
    return if (chosenAnswer == option) {
        copy(
            chosenAnswer = null,
            runningScore = if (isCorrect) (runningScore - 1).coerceAtLeast(0) else runningScore
        )
    } else if (chosenAnswer == null) {
        copy(
            chosenAnswer = option,
            runningScore = if (isCorrect) runningScore + 1 else runningScore
        )
    } else {
        this
    }
}

fun QuizSessionState.toggleTfStatement(index: Int, value: Boolean): QuizSessionState {
    if (tfChecked) return this
    val updated = tfSelections.toMutableMap()
    updated[index] = if (updated[index] == value) null else value
    return copy(tfSelections = updated)
}

fun QuizSessionState.evaluateCurrentTf(): QuizSessionState {
    val q = currentQuestion ?: return this
    val count = q.statements?.size ?: 5
    var score = 0
    for (i in 0 until count) {
        val selected = tfSelections[i]
        val real = q.answers?.getOrNull(i)
        if (selected != null && real != null) {
            if (selected == real) score += 1 else score -= 1
        }
    }
    val clamped = score.coerceAtLeast(0)
    return copy(
        tfQuestionScore = clamped,
        runningScore = runningScore + clamped,
        tfChecked = true
    )
}

fun QuizSessionState.advanceToNextQuestion(): QuizSessionState {
    return if (currentIndex + 1 < deck.size) {
        copy(
            currentIndex = currentIndex + 1,
            chosenAnswer = null,
            tfSelections = emptyMap(),
            tfChecked = false,
            tfQuestionScore = 0,
            saInputText = "",
            saChecked = false,
            showSeqAnswer = false,
            seqUserNotes = ""
        )
    } else {
        copy(quizFinished = true)
    }
}
