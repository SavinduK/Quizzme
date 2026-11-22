package com.example.data

import android.util.Base64
import android.util.Log
import com.example.model.QuestionStyle
import com.example.model.QuizJsonParser
import com.example.model.QuizQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AiRepository"
private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
private const val AI_TIMEOUT_MS = 60_000L
private const val MAX_INLINE_BYTES = 18 * 1024 * 1024

class AiRepository(
    private val settingsRepository: SettingsRepository
) {

    private suspend fun requireCredentials(): Pair<String, String> {
        val settings = settingsRepository.getSettingsOnce()
        val apiKey = settings.geminiApiKey.trim()
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "Gemini API key is not configured. Open Settings -> AI Settings to enter your API key."
            )
        }
        return apiKey to settings.selectedModel.modelId
    }

    suspend fun extractNotesFromPdf(pdfBytes: ByteArray): String {
        if (pdfBytes.isEmpty()) {
            throw IllegalArgumentException("The selected PDF file is empty.")
        }
        if (pdfBytes.size > MAX_INLINE_BYTES) {
            throw IllegalArgumentException("PDF exceeds the 18 MB inline upload limit. Please select a smaller file.")
        }
        val (apiKey, modelId) = requireCredentials()
        val encodedPdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
        val payload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("inline_data", buildJsonObject {
                                put("mime_type", "application/pdf")
                                put("data", encodedPdf)
                            })
                        })
                        add(buildJsonObject {
                            put(
                                "text",
                                "Extract and structure all core study and subject-related information found inside this document. " +
                                    "Automatically fix any clear typos or errors, and add helpful clarifications where concepts need context. " +
                                    "Format the entire output as clean, well-structured semantic HTML using tags such as " +
                                    "<h1>, <h2>, <h3>, <p>, <ul>, <ol>, <li>, <strong>, <em>, <blockquote>, <table>, <thead>, <tbody>, <tr>, <th>, <td>, <code>, <pre>, and <hr>. " +
                                    "Output raw HTML fragment content directly—do NOT use Markdown, do NOT include <html>/<head>/<body> wrapper tags, and do NOT wrap in code fences."
                            )
                        })
                    })
                })
            })
        }

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val rawText = executeGenerateContent(apiKey, modelId, payload.toString())
                val cleaned = cleanHtmlOutput(rawText)
                if (cleaned.isBlank()) {
                    throw IllegalStateException("AI engine returned an empty extraction.")
                }
                cleaned
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
        val (apiKey, modelId) = requireCredentials()
        val prompt =
            "You are a world-class academic summarizer. Read the following source notes (which may be HTML or plain text) " +
                "and provide a comprehensive, clear, and highly structured summary formatted as clean semantic HTML. " +
                "Organize the summary into logical sections starting with <h2> headings, followed by focused <p> paragraphs, " +
                "<ul> or <ol> takeaway lists, <strong> key definitions, <table> comparisons when useful, and <blockquote> clinical/exam pearls. " +
                "Output raw HTML fragment content directly—do NOT use Markdown, do NOT include <html>/<head>/<body> wrapper tags, and do NOT wrap in code fences.\n\nSource Notes:\n$sourceText"

        val payload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
        }

        val accumulated = StringBuilder()
        var connection: HttpURLConnection? = null
        try {
            withTimeout(AI_TIMEOUT_MS) {
                val url = URL("$GEMINI_API_BASE/$modelId:streamGenerateContent?alt=sse")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("x-goog-api-key", apiKey)
                }

                OutputStreamWriter(connection!!.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                }

                val code = connection!!.responseCode
                if (code !in 200..299) {
                    val errorBody = connection!!.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IllegalStateException(parseGeminiError(code, errorBody))
                }

                BufferedReader(InputStreamReader(connection!!.inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        currentCoroutineContext().ensureActive()
                        val rawLine = line?.trim().orEmpty()
                        if (!rawLine.startsWith("data:")) continue
                        val jsonPart = rawLine.removePrefix("data:").trim()
                        if (jsonPart.isEmpty() || jsonPart == "[DONE]") continue
                        val chunkText = extractCandidateText(jsonPart)
                        if (chunkText.isNotEmpty()) {
                            accumulated.append(chunkText)
                            val cleaned = cleanHtmlOutput(accumulated.toString())
                            if (cleaned.isNotEmpty()) {
                                emit(cleaned)
                            }
                        }
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
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateQuiz(
        sourceText: String,
        count: Int,
        style: QuestionStyle,
        customPrompt: String,
        referencePastPaperQuestions: List<QuizQuestion> = emptyList()
    ): List<QuizQuestion> {
        val (apiKey, modelId) = requireCredentials()
        val extra = if (customPrompt.isNotBlank()) "\nAdditional Instructions: $customPrompt" else ""
        val pastPaperReference = buildPastPaperReferenceSection(referencePastPaperQuestions)
        val prompt = when (style) {
            QuestionStyle.MCQ ->
                "Based on the following lecture notes, generate exactly $count multiple choice questions. " +
                    "Each question must have exactly 5 distinct options. Provide a brief explanation for why the correct answer is correct. " +
                    "Ensure \"correct_answer\" matches one of the 5 strings in \"options\" verbatim.$extra$pastPaperReference\n\nLecture notes:\n$sourceText"

            QuestionStyle.TF ->
                "Based on the following lecture notes, generate exactly $count True/False style questions. " +
                    "Each item must contain a header topic text called \"question\", and an array of exactly 5 distinct conceptual \"statements\" related to it, " +
                    "along with a matching 5-element boolean array \"answers\", and a concise \"explanation\".$extra$pastPaperReference\n\nLecture notes:\n$sourceText"

            QuestionStyle.SA ->
                "Based on the following lecture notes, generate exactly $count clear conceptual short answer questions. " +
                    "Each item must contain \"question\", \"correct_answer\" (definitive brief answer key phrase), " +
                    "and \"explanation\" (complete underlying context).$extra$pastPaperReference\n\nLecture notes:\n$sourceText"

            QuestionStyle.SEQ ->
                "Based on the complete lecture notes provided, construct $count comprehensive Structured Essay Questions (SEQs) " +
                    "that systematically cover the entire lecture content. Each SEQ must revolve around a core topic in \"question\" and contain " +
                    "2 to 4 \"sub_questions\" ranging from short recall/definitions to analytical essay prompts, with marks and detailed \"answer_key\" plus a complete \"model_answer\".$extra$pastPaperReference\n\nLecture notes:\n$sourceText"
        }

        val payload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("responseSchema", schemaForStyle(style))
            })
        }

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val jsonText = executeGenerateContent(apiKey, modelId, payload.toString())
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
        val (apiKey, modelId) = requireCredentials()
        val extra = if (customPrompt.isNotBlank()) "\nAdditional Instructions: $customPrompt" else ""
        val prompt =
            "Analyze the provided past paper text and classify each question into its appropriate format:\n" +
                "- \"MCQ\" (Multiple Choice with options and exact matching correct_answer)\n" +
                "- \"TF\" (True/False with statements array and boolean answers array)\n" +
                "- \"SA\" (Short Answer with correct_answer and explanation)\n" +
                "- \"SEQ\" (Structured Essay with sub_questions array and model_answer)\n" +
                "Extract all questions faithfully.$extra\n\nPast Paper Content Text:\n$rawText"

        val payload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("responseSchema", pastPaperSchema())
            })
        }

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val jsonText = executeGenerateContent(apiKey, modelId, payload.toString())
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

    suspend fun parsePastPaperImages(
        imageBytesList: List<ByteArray>,
        customPrompt: String = ""
    ): List<QuizQuestion> {
        if (imageBytesList.isEmpty()) {
            throw IllegalArgumentException("No past paper images were provided.")
        }
        val totalBytes = imageBytesList.sumOf { it.size.toLong() }
        if (totalBytes > MAX_INLINE_BYTES) {
            throw IllegalArgumentException("Selected images exceed the 18 MB upload limit. Please upload fewer or smaller images.")
        }
        val (apiKey, modelId) = requireCredentials()

        val extra = if (customPrompt.isNotBlank()) "\nAdditional Instructions: $customPrompt" else ""
        val promptText =
            "Analyze all attached past paper images thoroughly. Transcribe and extract every question shown across the images " +
                "and classify each into its appropriate format:\n" +
                "- \"MCQ\": Multiple Choice with all options and exact matching \"correct_answer\" plus concise \"explanation\".\n" +
                "- \"TF\": True/False stem with \"statements\" array, matching boolean \"answers\" array, and \"explanation\".\n" +
                "- \"SA\": Short Answer with \"correct_answer\" and \"explanation\".\n" +
                "- \"SEQ\": Structured Essay with \"sub_questions\" (sub_question, marks, answer_key) and \"model_answer\".\n" +
                "If an image shows questions without a visible answer key, solve them accurately and populate the answer key and explanation.$extra"

        val payload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        imageBytesList.forEach { bytes ->
                            add(buildJsonObject {
                                put("inline_data", buildJsonObject {
                                    put("mime_type", "image/jpeg")
                                    put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                                })
                            })
                        }
                        add(buildJsonObject { put("text", promptText) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("responseSchema", pastPaperSchema())
            })
        }

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val jsonText = executeGenerateContent(apiKey, modelId, payload.toString())
                val parsed = QuizJsonParser.parseQuestions(jsonText)
                if (parsed.isEmpty()) {
                    throw IllegalStateException("Could not detect any structured questions in the uploaded images.")
                }
                parsed
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Past paper image analysis timed out", e)
            throw IllegalStateException("Past paper image analysis timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Past paper image analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to analyze past paper images.")
        }
    }

    private suspend fun executeGenerateContent(
        apiKey: String,
        modelId: String,
        requestBodyJson: String
    ): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$GEMINI_API_BASE/$modelId:generateContent")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBodyJson)
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException(parseGeminiError(code, body))
            }

            val text = extractCandidateText(body)
            if (text.isBlank()) {
                throw IllegalStateException("Gemini API returned an empty response.")
            }
            text
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractCandidateText(responseJson: String): String {
        val root = runCatching { QuizJsonParser.json.parseToJsonElement(responseJson).jsonObject }
            .getOrNull() ?: return ""
        val candidates = root["candidates"] as? JsonArray ?: return ""
        val first = candidates.firstOrNull() as? JsonObject ?: return ""
        val content = first["content"] as? JsonObject ?: return ""
        val parts = content["parts"] as? JsonArray ?: return ""
        return parts.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            (obj["text"] as? JsonPrimitive)?.content
        }.joinToString("")
    }

    private fun parseGeminiError(statusCode: Int, errorBody: String): String {
        val msg = runCatching {
            val root = QuizJsonParser.json.parseToJsonElement(errorBody).jsonObject
            root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return if (!msg.isNullOrBlank()) {
            "Gemini API error ($statusCode): $msg"
        } else {
            "Gemini API request failed with HTTP $statusCode."
        }
    }

    private fun buildPastPaperReferenceSection(referenceQuestions: List<QuizQuestion>): String {
        if (referenceQuestions.isEmpty()) return ""
        val sample = referenceQuestions.take(15)
        val formatted = sample.mapIndexed { index, q ->
            val style = q.detectedStyle().shortLabel
            buildString {
                append("${index + 1}. [$style] ${q.question}")
                if (!q.options.isNullOrEmpty()) {
                    append(" | Options: ${q.options.joinToString(" / ")}")
                }
                if (!q.statements.isNullOrEmpty()) {
                    append(" | Statements: ${q.statements.joinToString("; ")}")
                }
                if (!q.subQuestions.isNullOrEmpty()) {
                    append(" | Sub-questions: ${q.subQuestions.joinToString("; ") { it.subQuestion }}")
                }
            }
        }.joinToString("\n")

        return "\n\nReference Past Paper Questions (refer closely to these past paper questions to match exam style, difficulty, " +
            "clinical/academic framing, phrasing, and high-yield tested topics—generate fresh, non-duplicate questions aligned with these past paper patterns):\n$formatted"
    }

    private fun cleanHtmlOutput(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace(Regex("^```(?:html|xml)?\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*```$"), "")
                .trim()
        }
        val bodyMatch = Regex("<body\\b[^>]*>([\\s\\S]*?)</body>", RegexOption.IGNORE_CASE).find(cleaned)
        if (bodyMatch != null) {
            cleaned = bodyMatch.groupValues[1].trim()
        } else {
            cleaned = cleaned
                .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("</?(?:html|head|body)\\b[^>]*>", RegexOption.IGNORE_CASE), "")
                .trim()
        }
        return cleaned
    }

    private fun schemaForStyle(style: QuestionStyle): JsonObject {
        val questionItemSchema = when (style) {
            QuestionStyle.MCQ -> buildJsonObject {
                put("type", "OBJECT")
                put("properties", buildJsonObject {
                    put("type", stringSchema("Question type: MCQ"))
                    put("question", stringSchema("The multiple choice question stem"))
                    put("options", arraySchema(stringSchema(), "Exactly 5 distinct answer choices"))
                    put("correct_answer", stringSchema("Exact matching string from options"))
                    put("explanation", stringSchema("Brief explanation of why the answer is correct"))
                })
                put("required", buildJsonArray {
                    add(JsonPrimitive("type"))
                    add(JsonPrimitive("question"))
                    add(JsonPrimitive("options"))
                    add(JsonPrimitive("correct_answer"))
                    add(JsonPrimitive("explanation"))
                })
            }

            QuestionStyle.TF -> buildJsonObject {
                put("type", "OBJECT")
                put("properties", buildJsonObject {
                    put("type", stringSchema("Question type: TF"))
                    put("question", stringSchema("Topic context heading for the 5 statements"))
                    put("statements", arraySchema(stringSchema(), "Exactly 5 distinct conceptual statements"))
                    put("answers", arraySchema(boolSchema(), "Exactly 5 boolean answers matching statements"))
                    put("explanation", stringSchema("Concise rationale for the statements"))
                })
                put("required", buildJsonArray {
                    add(JsonPrimitive("type"))
                    add(JsonPrimitive("question"))
                    add(JsonPrimitive("statements"))
                    add(JsonPrimitive("answers"))
                    add(JsonPrimitive("explanation"))
                })
            }

            QuestionStyle.SA -> buildJsonObject {
                put("type", "OBJECT")
                put("properties", buildJsonObject {
                    put("type", stringSchema("Question type: SA"))
                    put("question", stringSchema("Conceptual short answer question"))
                    put("correct_answer", stringSchema("Definitive brief answer key phrase"))
                    put("explanation", stringSchema("Detailed context explanation"))
                })
                put("required", buildJsonArray {
                    add(JsonPrimitive("type"))
                    add(JsonPrimitive("question"))
                    add(JsonPrimitive("correct_answer"))
                    add(JsonPrimitive("explanation"))
                })
            }

            QuestionStyle.SEQ -> buildJsonObject {
                put("type", "OBJECT")
                put("properties", buildJsonObject {
                    put("type", stringSchema("Question type: SEQ"))
                    put("question", stringSchema("Main essay topic"))
                    put(
                        "sub_questions",
                        arraySchema(
                            buildJsonObject {
                                put("type", "OBJECT")
                                put("properties", buildJsonObject {
                                    put("sub_question", stringSchema("Sub-question prompt e.g. (a) Define..."))
                                    put("marks", intSchema("Marks allocated"))
                                    put("answer_key", stringSchema("Marking rubric and key points"))
                                })
                                put("required", buildJsonArray {
                                    add(JsonPrimitive("sub_question"))
                                    add(JsonPrimitive("marks"))
                                    add(JsonPrimitive("answer_key"))
                                })
                            }
                        )
                    )
                    put("model_answer", stringSchema("Complete synthesized model essay answer"))
                })
                put("required", buildJsonArray {
                    add(JsonPrimitive("type"))
                    add(JsonPrimitive("question"))
                    add(JsonPrimitive("sub_questions"))
                    add(JsonPrimitive("model_answer"))
                })
            }
        }

        return buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("questions", arraySchema(questionItemSchema, "Generated list of questions"))
            })
            put("required", buildJsonArray { add(JsonPrimitive("questions")) })
        }
    }

    private fun pastPaperSchema(): JsonObject {
        val subQuestionSchema = buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("sub_question", stringSchema())
                put("marks", intSchema(nullable = true))
                put("answer_key", stringSchema())
            })
            put("required", buildJsonArray {
                add(JsonPrimitive("sub_question"))
                add(JsonPrimitive("answer_key"))
            })
        }

        val itemSchema = buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("type", buildJsonObject {
                    put("type", "STRING")
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("MCQ"))
                        add(JsonPrimitive("TF"))
                        add(JsonPrimitive("SA"))
                        add(JsonPrimitive("SEQ"))
                    })
                    put("description", "Detected question format")
                })
                put("question", stringSchema("Question stem or topic"))
                put("options", arraySchema(stringSchema(), nullable = true))
                put("correct_answer", stringSchema(nullable = true))
                put("statements", arraySchema(stringSchema(), nullable = true))
                put("answers", arraySchema(boolSchema(), nullable = true))
                put("explanation", stringSchema(nullable = true))
                put("sub_questions", arraySchema(subQuestionSchema, nullable = true))
                put("model_answer", stringSchema(nullable = true))
            })
            put("required", buildJsonArray {
                add(JsonPrimitive("type"))
                add(JsonPrimitive("question"))
            })
        }

        return buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("questions", arraySchema(itemSchema))
            })
            put("required", buildJsonArray { add(JsonPrimitive("questions")) })
        }
    }

    private fun stringSchema(description: String? = null, nullable: Boolean = false): JsonObject =
        buildJsonObject {
            put("type", "STRING")
            if (description != null) put("description", description)
            if (nullable) put("nullable", true)
        }

    private fun intSchema(description: String? = null, nullable: Boolean = false): JsonObject =
        buildJsonObject {
            put("type", "INTEGER")
            if (description != null) put("description", description)
            if (nullable) put("nullable", true)
        }

    private fun boolSchema(description: String? = null, nullable: Boolean = false): JsonObject =
        buildJsonObject {
            put("type", "BOOLEAN")
            if (description != null) put("description", description)
            if (nullable) put("nullable", true)
        }

    private fun arraySchema(
        items: JsonObject,
        description: String? = null,
        nullable: Boolean = false
    ): JsonObject = buildJsonObject {
        put("type", "ARRAY")
        put("items", items)
        if (description != null) put("description", description)
        if (nullable) put("nullable", true)
    }
}
