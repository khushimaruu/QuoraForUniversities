package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiConfig {
    val GEMINI_API_KEY: String get() = BuildConfig.GEMINI_API_KEY
    const val MODEL_NAME = "gemini-3.5-flash"
}

data class ModerationResult(
    val isAllowed: Boolean,
    val reason: String? = null
)

data class EnhancedPost(
    val title: String,
    val description: String
)

class GeminiService {

    private val apiKey = GeminiConfig.GEMINI_API_KEY

    suspend fun checkContentModeration(title: String, description: String): Result<ModerationResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                    // Fallback local moderation if API key is not set
                    val combined = "$title $description".lowercase()
                    val bannedWords = listOf("idiot", "stupid", "hate", "dumb", "fuck", "shit", "bitch", "bastard")
                    val foundWord = bannedWords.firstOrNull { combined.contains(it) }
                    if (foundWord != null) {
                        return@runCatching ModerationResult(
                            isAllowed = false,
                            reason = "Contains inappropriate word: '$foundWord'"
                        )
                    }
                    return@runCatching ModerationResult(isAllowed = true)
                }

                val prompt = """
                    You are a content moderation AI for a university campus Q&A application.
                    Analyze the following post title and description for bad language, hate speech, profanity, toxicity, harassment, or inappropriate content.
                    
                    Post Title: "$title"
                    Post Description: "$description"
                    
                    Instructions:
                    If the post contains bad language, profanity, harassment, or inappropriate content, respond strictly with:
                    FLAGGED: <Short clear reason for rejection>
                    
                    If the post is safe, appropriate, and clean, respond strictly with:
                    ALLOWED
                """.trimIndent()

                val responseText = callGeminiApi(prompt)

                if (responseText.trim().startsWith("FLAGGED:", ignoreCase = true)) {
                    val reason = responseText.substringAfter("FLAGGED:").trim()
                        .ifBlank { "Content violates community guidelines." }
                    ModerationResult(isAllowed = false, reason = reason)
                } else if (responseText.contains("FLAGGED", ignoreCase = true)) {
                    ModerationResult(isAllowed = false, reason = "Content flagged as inappropriate by AI moderation.")
                } else {
                    ModerationResult(isAllowed = true)
                }
            }
        }

    suspend fun enhancePostContent(title: String, description: String): Result<EnhancedPost> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                    // Fallback mock enhancement if API key is missing
                    return@runCatching EnhancedPost(
                        title = title.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        description = description.trim() + " [Refined for clarity]"
                    )
                }

                val prompt = """
                    You are an expert writing assistant for a university Q&A forum.
                    Improve the following post title and description to make them clearer, well-punctuated, professional, and grammatically correct. Keep the original intent and tone.
                    
                    Original Title: $title
                    Original Description: $description
                    
                    Output format strictly as:
                    ENHANCED_TITLE: <improved title>
                    ENHANCED_DESCRIPTION: <improved description>
                """.trimIndent()

                val responseText = callGeminiApi(prompt)

                val enhancedTitle = responseText.lineSequence()
                    .find { it.startsWith("ENHANCED_TITLE:", ignoreCase = true) }
                    .orEmpty()
                    .substringAfter("ENHANCED_TITLE:")
                    .trim()
                    .ifBlank { title }

                val enhancedDesc = responseText.lineSequence()
                    .find { it.startsWith("ENHANCED_DESCRIPTION:", ignoreCase = true) }
                    .orEmpty()
                    .substringAfter("ENHANCED_DESCRIPTION:")
                    .trim()
                    .ifBlank { description }

                EnhancedPost(title = enhancedTitle, description = enhancedDesc)
            }
        }

    private fun callGeminiApi(promptText: String): String {
        val modelsToTry = listOf("gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-1.5-flash")
        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            try {
                return executeGeminiRequest(promptText, endpointUrl)
            } catch (e: Exception) {
                lastError = e
                val msg = e.message.orEmpty().lowercase()
                if (!msg.contains("404") && !msg.contains("not_found") && !msg.contains("not found")) {
                    throw e
                }
            }
        }
        throw lastError ?: Exception("Failed to connect to Gemini API.")
    }

    private fun executeGeminiRequest(promptText: String, fullUrl: String): String {
        val url = URL(fullUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.doOutput = true
            connection.connectTimeout = 20000
            connection.readTimeout = 20000

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val textObj = JSONObject().apply {
                                put("text", promptText)
                            }
                            put(textObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            val inputStream = if (statusCode == 200) connection.inputStream else connection.errorStream
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val responseString = reader.readText()
            reader.close()

            if (statusCode != 200) {
                val apiErrorMessage = runCatching {
                    JSONObject(responseString).optJSONObject("error")?.optString("message")
                }.getOrNull()?.ifBlank { null }

                val cleanMessage = apiErrorMessage ?: "HTTP $statusCode error"
                error("Gemini API Error ($statusCode): $cleanMessage")
            }

            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text")
                }
            }
            return ""
        } finally {
            connection.disconnect()
        }
    }
}
