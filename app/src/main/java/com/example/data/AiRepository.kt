package com.example.data

import android.util.Log
import com.example.model.QuestionStyle
import com.example.model.QuizJsonParser
import com.example.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L
private const val MAX_INLINE_PDF_BYTES = 18 * 1024 * 1024

class AiRepository {

    private val textModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = MODEL_NAME)
    }

    private fun structuredQuizModel(style: QuestionStyle) =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schemaForStyle(style)
            }
        )

    private val pastPaperModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = pastPaperSchema()
            }
        )
    }

    suspend fun extractNotesFromPdf(pdfBytes: ByteArray): String {
        if (pdfBytes.isEmpty()) {
            throw IllegalArgumentException("The selected PDF file is empty.")
        }
        if (pdfBytes.size > MAX_INLINE_PDF_BYTES) {
            throw IllegalArgumentException("PDF exceeds the 18 MB inline upload limit. Please select a smaller file.")
        }
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val prompt = content {
                    inlineData(bytes = pdfBytes, mimeType = "application/pdf")
                    text(
                        "Extract and structure all core study and subject-related information found inside this document. " +
                            "Automatically fix any clear typos or errors, and add helpful clarifications where concepts need context. " +
                            "Structure the final output beautifully using clear Markdown (including headings, bullet points, tables, " +
                            "and blockquotes where appropriate) to ensure the notes are highly clean, scannable, and informative."
                    )
                }
                val response = textModel.generateContent(prompt)
                val text = response.text?.trim()
                if (text.isNullOrBlank()) {
                    throw IllegalStateException("AI engine returned an empty extraction.")
                }
                text
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "PDF extraction timed out", e)
            throw IllegalStateException("PDF extraction timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "PDF extraction failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to extract text from PDF.")
        }
    }

    fun generateSummaryStream(sourceText: String): Flow<String> = flow {
        val prompt =
            "You are a world-class academic summarizer. Read the following source text and provide a comprehensive, " +
                "clear, and highly structured summary. Use Markdown features natively: clear Headings (# and ##), " +
                "bulleted takeaway lists, bold terms for critical definitions, and short focused paragraphs. " +
                "Avoid code block syntax envelopes or wrappers—just send the raw Markdown text.\n\nSource Text:\n$sourceText"

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                textModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Model returned an empty summary.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Summary generation timed out", e)
            throw IllegalStateException("Summary generation timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not generate summary.")
        }
    }

    suspend fun generateQuiz(
        sourceText: String,
        count: Int,
        style: QuestionStyle,
        customPrompt: String
    ): List<QuizQuestion> {
        val extra = if (customPrompt.isNotBlank()) "\nAdditional Instructions: $customPrompt" else ""
        val prompt = when (style) {
            QuestionStyle.MCQ ->
                "Based on the following source material text, generate exactly $count multiple choice questions. " +
                    "Each question must have exactly 5 distinct options. Provide a brief explanation for why the correct answer is correct. " +
                    "Ensure \"correct_answer\" matches one of the 5 strings in \"options\" verbatim.$extra\n\nSource material text:\n$sourceText"

            QuestionStyle.TF ->
                "Based on the following source material text, generate exactly $count True/False style questions. " +
                    "Each item must contain a header topic text called \"question\", and an array of exactly 5 distinct conceptual \"statements\" related to it, " +
                    "along with a matching 5-element boolean array \"answers\", and a concise \"explanation\".$extra\n\nSource material text:\n$sourceText"

            QuestionStyle.SA ->
                "Based on the following source material text, generate exactly $count clear conceptual short answer questions. " +
                    "Each item must contain \"question\", \"correct_answer\" (definitive brief answer key phrase), " +
                    "and \"explanation\" (complete underlying context).$extra\n\nSource material text:\n$sourceText"

            QuestionStyle.SEQ ->
                "Based on the complete source material provided, construct $count comprehensive Structured Essay Questions (SEQs) " +
                    "that systematically cover the entire lecture content. Each SEQ must revolve around a core topic in \"question\" and contain " +
                    "2 to 4 \"sub_questions\" ranging from short recall/definitions to analytical essay prompts, with marks and detailed \"answer_key\" plus a complete \"model_answer\".$extra\n\nSource material text:\n$sourceText"
        }

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val model = structuredQuizModel(style)
                val response = model.generateContent(prompt)
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty quiz response.")
                val parsed = QuizJsonParser.parseQuestions(jsonText)
                if (parsed.isEmpty()) {
                    throw IllegalStateException("No questions were generated from the source text.")
                }
                parsed
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quiz generation timed out", e)
            throw IllegalStateException("Quiz generation timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quiz generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate quiz questions.")
        }
    }

    suspend fun parsePastPaperText(
        rawText: String,
        customPrompt: String = ""
    ): List<QuizQuestion> {
        val extra = if (customPrompt.isNotBlank()) "\nAdditional Instructions: $customPrompt" else ""
        val prompt =
            "Analyze the provided past paper text and classify each question into its appropriate format:\n" +
                "- \"MCQ\" (Multiple Choice with options and exact matching correct_answer)\n" +
                "- \"TF\" (True/False with statements array and boolean answers array)\n" +
                "- \"SA\" (Short Answer with correct_answer and explanation)\n" +
                "- \"SEQ\" (Structured Essay with sub_questions array and model_answer)\n" +
                "Extract all questions faithfully.$extra\n\nPast Paper Content Text:\n$rawText"

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = pastPaperModel.generateContent(prompt)
                val jsonText = response.text ?: throw IllegalStateException("No response returned from the AI model.")
                val parsed = QuizJsonParser.parseQuestions(jsonText)
                if (parsed.isEmpty()) {
                    throw IllegalStateException("Could not detect any structured questions from the provided text.")
                }
                parsed
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Past paper parsing timed out", e)
            throw IllegalStateException("Past paper parsing timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Past paper parsing failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to classify past paper text.")
        }
    }

    private fun schemaForStyle(style: QuestionStyle): Schema {
        val questionItemSchema = when (style) {
            QuestionStyle.MCQ -> Schema.obj(
                properties = mapOf(
                    "type" to Schema.string(description = "Question type: MCQ"),
                    "question" to Schema.string(description = "The multiple choice question stem"),
                    "options" to Schema.array(
                        items = Schema.string(),
                        description = "Exactly 5 distinct answer choices"
                    ),
                    "correct_answer" to Schema.string(description = "Exact matching string from options"),
                    "explanation" to Schema.string(description = "Brief explanation of why the answer is correct")
                )
            )

            QuestionStyle.TF -> Schema.obj(
                properties = mapOf(
                    "type" to Schema.string(description = "Question type: TF"),
                    "question" to Schema.string(description = "Topic context heading for the 5 statements"),
                    "statements" to Schema.array(
                        items = Schema.string(),
                        description = "Exactly 5 distinct conceptual statements"
                    ),
                    "answers" to Schema.array(
                        items = Schema.boolean(),
                        description = "Exactly 5 boolean answers matching statements"
                    ),
                    "explanation" to Schema.string(description = "Concise rationale for the statements")
                )
            )

            QuestionStyle.SA -> Schema.obj(
                properties = mapOf(
                    "type" to Schema.string(description = "Question type: SA"),
                    "question" to Schema.string(description = "Conceptual short answer question"),
                    "correct_answer" to Schema.string(description = "Definitive brief answer key phrase"),
                    "explanation" to Schema.string(description = "Detailed context explanation")
                )
            )

            QuestionStyle.SEQ -> Schema.obj(
                properties = mapOf(
                    "type" to Schema.string(description = "Question type: SEQ"),
                    "question" to Schema.string(description = "Main essay topic"),
                    "sub_questions" to Schema.array(
                        items = Schema.obj(
                            properties = mapOf(
                                "sub_question" to Schema.string(description = "Sub-question prompt e.g. (a) Define..."),
                                "marks" to Schema.integer(description = "Marks allocated"),
                                "answer_key" to Schema.string(description = "Marking rubric and key points")
                            )
                        )
                    ),
                    "model_answer" to Schema.string(description = "Complete synthesized model essay answer")
                )
            )
        }

        return Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(
                    items = questionItemSchema,
                    description = "Generated list of questions"
                )
            )
        )
    }

    private fun pastPaperSchema(): Schema {
        val subQuestionSchema = Schema.obj(
            properties = mapOf(
                "sub_question" to Schema.string(),
                "marks" to Schema.integer(),
                "answer_key" to Schema.string()
            ),
            optionalProperties = listOf("marks")
        )

        val itemSchema = Schema.obj(
            properties = mapOf(
                "type" to Schema.enumeration(
                    values = listOf("MCQ", "TF", "SA", "SEQ"),
                    description = "Detected question format"
                ),
                "question" to Schema.string(description = "Question stem or topic"),
                "options" to Schema.array(items = Schema.string(), nullable = true),
                "correct_answer" to Schema.string(nullable = true),
                "statements" to Schema.array(items = Schema.string(), nullable = true),
                "answers" to Schema.array(items = Schema.boolean(), nullable = true),
                "explanation" to Schema.string(nullable = true),
                "sub_questions" to Schema.array(items = subQuestionSchema, nullable = true),
                "model_answer" to Schema.string(nullable = true)
            ),
            optionalProperties = listOf(
                "options",
                "correct_answer",
                "statements",
                "answers",
                "explanation",
                "sub_questions",
                "model_answer"
            )
        )

        return Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(items = itemSchema)
            )
        )
    }
}
