package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.BuildConfig
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

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

    private class ApiFailure(val status: Int) : Exception()

    companion object {
        fun moderationErrorMessage(error: Throwable?): String = when {
            error is ApiFailure && error.status in setOf(429, 500, 502, 503, 504) ->
                "The content check is temporarily busy. Please try again shortly. Your draft has been kept."
            error is SocketTimeoutException ->
                "The content check timed out. Please try again. Your draft has been kept."
            else -> "Couldn't check the content right now. Please try again. Your draft has been kept."
        }
    }

    private val apiKey = GeminiConfig.GEMINI_API_KEY

    private fun localContentCheck(text: String): ModerationResult {
        val lower = text.lowercase()
        val toxicTerms = listOf(
            "idiot", "stupid", "dumb", "fuck", "shit", "bitch", "bastard", "asshole",
            "rude", "ugly", "hate", "loser", "shut up",
            "useless", "nasty", "horrible", "disgusting", "scam", "pathetic",
            "freak", "clown", "scum"
        )
        // Match whole words; academic terms such as mean, studies, and material
        // creep need contextual moderation by Gemini rather than substring rejection.
        val matched = toxicTerms.firstOrNull { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) }
        if (matched != null) {
            return ModerationResult(
                isAllowed = false,
                reason = "Content violates community guidelines: contains rude or inappropriate language ('$matched')."
            )
        }
        return ModerationResult(isAllowed = true)
    }

    suspend fun checkContentModeration(title: String, description: String): Result<ModerationResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val combined = "$title $description".trim()
                val localResult = localContentCheck(combined)
                if (!localResult.isAllowed) {
                    return@runCatching localResult
                }

                check(apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY_HERE") {
                    "AI moderation is unavailable. Posting is disabled until it is configured."
                }

                val prompt = """
                    You are a strict, zero-tolerance content moderation AI for a university campus Q&A app.
                    Your strict duty is to protect the community from ANY form of disrespect, toxicity, hostility, insults, rudeness, personal attacks, hate speech, profanity, or negative conduct.

                    Analyze the following post title and description carefully:
                    Post Title: "$title"
                    Post Description: "$description"

                    Rulebook:
                    1. Zero Tolerance for Insults & Rudeness: Any disrespect, insults, or rudeness (e.g., "you are mean", "you are rude", "shut up", "dumb", "loser") MUST be FLAGGED.
                    2. Zero Tolerance for Hate Speech & Bias: Any attack or hostility based on race, gender, religion, ethnicity, orientation, or background MUST be FLAGGED.
                    3. Zero Tolerance for Profanity & Harassment: Bad language, swearing, vulgarity, harassment, or toxic behavior MUST be FLAGGED.
                    4. Constructive Discussion: Only polite, respectful, safe, and academic content is ALLOWED.
                    5. Interpret words in context. Academic terms and neutral content labels such as "child reply" are not insults by themselves.

                    OUTPUT REQUIREMENT:
                    If the content violates ANY rule above, reply strictly in this format:
                    FLAGGED: <Short clear explanation of the violation>

                    If the content is clean, polite, respectful, and safe, reply strictly:
                    ALLOWED
                """.trimIndent()

                val responseText = callGeminiApi(prompt).trim()

                if (responseText.startsWith("FLAGGED:", ignoreCase = true)) {
                    val reason = responseText.substringAfter(':', missingDelimiterValue = "")
                        .trim()
                        .ifBlank { "Content flagged as rude, toxic, or inappropriate by AI moderation." }
                    ModerationResult(isAllowed = false, reason = reason)
                } else if (!responseText.equals("ALLOWED", ignoreCase = true)) {
                    ModerationResult(isAllowed = false, reason = "Content flagged by community safety guidelines.")
                } else {
                    ModerationResult(isAllowed = true)
                }
            }
        }

    suspend fun checkReplyModeration(content: String): Result<ModerationResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val textToTest = content.trim()
                val localResult = localContentCheck(textToTest)
                if (!localResult.isAllowed) {
                    return@runCatching localResult
                }

                check(apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY_HERE") {
                    "AI moderation is unavailable. Posting is disabled until it is configured."
                }

                val prompt = """
                    You are a strict, zero-tolerance content moderation AI for a university campus Q&A app.
                    Your strict duty is to protect the community from ANY form of disrespect, toxicity, hostility, insults, rudeness, personal attacks, hate speech, profanity, or negative conduct.

                    Analyze the following answer/reply content carefully:
                    Reply Content: "$content"

                    Rulebook:
                    1. Zero Tolerance for Insults & Rudeness: Any disrespect, insults, or rudeness (e.g., "you are mean", "you are rude", "shut up", "dumb", "loser") MUST be FLAGGED.
                    2. Zero Tolerance for Hate Speech & Bias: Any attack or hostility based on race, gender, religion, ethnicity, orientation, or background MUST be FLAGGED.
                    3. Zero Tolerance for Profanity & Harassment: Bad language, swearing, vulgarity, harassment, or toxic behavior MUST be FLAGGED.
                    4. Constructive Discussion: Only polite, respectful, safe, and academic content is ALLOWED.
                    5. Interpret words in context. Academic terms and neutral content labels such as "child reply" are not insults by themselves.

                    OUTPUT REQUIREMENT:
                    If the content violates ANY rule above, reply strictly in this format:
                    FLAGGED: <Short clear explanation of the violation>

                    If the content is clean, polite, respectful, and safe, reply strictly:
                    ALLOWED
                """.trimIndent()

                val responseText = callGeminiApi(prompt).trim()

                if (responseText.startsWith("FLAGGED:", ignoreCase = true)) {
                    val reason = responseText.substringAfter(':', missingDelimiterValue = "")
                        .trim()
                        .ifBlank { "Reply flagged as rude, toxic, or inappropriate by AI moderation." }
                    ModerationResult(isAllowed = false, reason = reason)
                } else if (!responseText.equals("ALLOWED", ignoreCase = true)) {
                    ModerationResult(isAllowed = false, reason = "Reply flagged by community safety guidelines.")
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
                    .substringAfter(':')
                    .trim()
                    .ifBlank { title }

                val enhancedDesc = responseText.lineSequence()
                    .find { it.startsWith("ENHANCED_DESCRIPTION:", ignoreCase = true) }
                    .orEmpty()
                    .substringAfter(':')
                    .trim()
                    .ifBlank { description }

                EnhancedPost(title = enhancedTitle, description = enhancedDesc)
            }
        }

    private suspend fun callGeminiApi(promptText: String): String {
        val modelsToTry = listOf(GeminiConfig.MODEL_NAME, "gemini-3.5-flash-lite", "gemini-1.5-flash")
        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
            for (attempt in 0..2) {
                currentCoroutineContext().ensureActive()
                try {
                    val response = executeGeminiRequest(promptText, endpointUrl)
                    currentCoroutineContext().ensureActive()
                    return response
                } catch (e: ApiFailure) {
                    Log.w("GEMINI_API", "Request failed: HTTP ${e.status}, attempt ${attempt + 1}")
                    lastError = e
                    if (e.status == 404) break
                    if (e.status !in setOf(429, 500, 502, 503, 504) || attempt == 2) throw e
                    delay(1000L shl attempt)
                } catch (e: CancellationException) {
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
            if (statusCode != 200) {
                connection.errorStream?.close()
                throw ApiFailure(statusCode)
            }

            val responseString = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }

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
