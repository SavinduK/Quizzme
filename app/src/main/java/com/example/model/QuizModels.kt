package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

enum class QuestionStyle(val label: String, val shortLabel: String) {
    MCQ("Multiple Choice", "MCQ"),
    TF("True / False", "True / False"),
    SA("Short Answer", "Short Answer"),
    SEQ("Essay (SEQ)", "Structured");

    companion object {
        fun fromString(raw: String?): QuestionStyle {
            return when (raw?.trim()?.uppercase()) {
                "TF", "TRUE_FALSE" -> TF
                "SA", "SHORT_ANSWER" -> SA
                "SEQ", "STRUCTURED" -> SEQ
                else -> MCQ
            }
        }
    }
}

enum class SourceMode(val label: String) {
    AI("AI Generated Questions"),
    PAST_PAPER("Saved Past Papers")
}

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val tierLabel: String,
    val description: String
) {
    GEMINI_2_5_FLASH(
        modelId = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        tierLabel = "2.5 Flash",
        description = "Reliable hybrid reasoning model for fast PDF extraction & quizzes"
    ),
    GEMINI_2_5_FLASH_LITE(
        modelId = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash-Lite",
        tierLabel = "2.5 Lite",
        description = "Low-latency, cost-efficient model for quick summaries and quizzes"
    ),
    GEMINI_2_5_PRO(
        modelId = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        tierLabel = "2.5 Pro",
        description = "High-precision thinking model for dense notes and structured essays"
    ),
    FLASH(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        tierLabel = "3.8 Flash",
        description = "Next-gen fast, balanced intelligence for extraction & quizzes"
    ),
    FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        tierLabel = "3.5 Lite",
        description = "Ultra-fast, lightweight Gemini 3.5 model for high-speed runs"
    ),
    PRO(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        tierLabel = "3.1 Pro",
        description = "Deepest reasoning preview for complex past papers and essays"
    );

    companion object {
        fun fromIdOrName(raw: String?): GeminiModelOption {
            val clean = raw?.trim().orEmpty()
            return entries.firstOrNull {
                it.name.equals(clean, ignoreCase = true) ||
                    it.modelId.equals(clean, ignoreCase = true)
            } ?: GEMINI_2_5_FLASH
        }
    }
}

@Serializable
data class SeqSubQuestion(
    @SerialName("sub_question")
    val subQuestion: String = "",
    val marks: Int? = null,
    @SerialName("answer_key")
    val answerKey: String = ""
)

@Serializable
data class QuizQuestion(
    val type: String? = null,
    val question: String = "",
    val options: List<String>? = null,
    @SerialName("correct_answer")
    val correctAnswer: String? = null,
    @SerialName("correct_answers")
    val correctAnswers: List<String>? = null,
    val statements: List<String>? = null,
    val answers: List<Boolean>? = null,
    val explanation: String? = null,
    @SerialName("sub_questions")
    val subQuestions: List<SeqSubQuestion>? = null,
    @SerialName("model_answer")
    val modelAnswer: String? = null,
    val year: String? = null,
    val lesson: String? = null
) {
    fun detectedStyle(): QuestionStyle {
        return when {
            !subQuestions.isNullOrEmpty() -> QuestionStyle.SEQ
            !statements.isNullOrEmpty() && !answers.isNullOrEmpty() -> QuestionStyle.TF
            !options.isNullOrEmpty() && (correctAnswer != null || !correctAnswers.isNullOrEmpty()) -> QuestionStyle.MCQ
            !explanation.isNullOrBlank() && (correctAnswer != null || !correctAnswers.isNullOrEmpty()) -> QuestionStyle.SA
            !type.isNullOrBlank() -> QuestionStyle.fromString(type)
            else -> QuestionStyle.MCQ
        }
    }

    fun primaryCorrectAnswer(): String? {
        return correctAnswer?.takeIf { it.isNotBlank() } ?: correctAnswers?.firstOrNull { it.isNotBlank() }
    }

    fun resolvedCorrectResponses(): List<String> {
        val list = buildList {
            if (!correctAnswers.isNullOrEmpty()) {
                addAll(correctAnswers.filter { it.isNotBlank() })
            }
            val single = correctAnswer?.takeIf { it.isNotBlank() }
            if (single != null && none { it.equals(single, ignoreCase = true) }) {
                add(0, single)
            }
        }
        return list
    }
}

@Serializable
data class QuizDeckEnvelope(
    val questions: List<QuizQuestion> = emptyList()
)

@Serializable
data class TrackerTaskJsonItem(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val title: String? = null,
    val subject: String? = null,
    val completed: Boolean? = null,
    val isCustom: Boolean? = null
)

data class QuizSettings(
    val geminiApiKey: String = "",
    val selectedModel: GeminiModelOption = GeminiModelOption.GEMINI_2_5_FLASH,
    val questionCount: Int = 5,
    val questionStyle: QuestionStyle = QuestionStyle.MCQ,
    val customPrompt: String = ""
)

object QuizJsonParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    fun cleanJsonInput(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace(Regex("^```(?:json)?\\s*"), "")
                .replace(Regex("\\s*```$"), "")
        }
        val firstObj = cleaned.indexOf('{')
        val firstArr = cleaned.indexOf('[')
        val startIdx = when {
            firstObj == -1 -> firstArr
            firstArr == -1 -> firstObj
            else -> minOf(firstObj, firstArr)
        }
        val lastObj = cleaned.lastIndexOf('}')
        val lastArr = cleaned.lastIndexOf(']')
        val endIdx = maxOf(lastObj, lastArr)
        if (startIdx != -1 && endIdx > startIdx) {
            cleaned = cleaned.substring(startIdx, endIdx + 1)
        }
        return cleaned.trim()
    }

    fun parseQuestions(rawJson: String): List<QuizQuestion> {
        val cleaned = cleanJsonInput(rawJson)
        if (cleaned.isBlank()) return emptyList()
        val element = json.parseToJsonElement(cleaned)
        return when (element) {
            is JsonArray -> json.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(QuizQuestion.serializer()),
                element
            )
            is JsonObject -> {
                if (element.containsKey("questions")) {
                    json.decodeFromJsonElement(QuizDeckEnvelope.serializer(), element).questions
                } else {
                    listOf(json.decodeFromJsonElement(QuizQuestion.serializer(), element))
                }
            }
            else -> emptyList()
        }
    }

    fun encodeDeck(questions: List<QuizQuestion>): String {
        return json.encodeToString(QuizDeckEnvelope.serializer(), QuizDeckEnvelope(questions))
    }
}
