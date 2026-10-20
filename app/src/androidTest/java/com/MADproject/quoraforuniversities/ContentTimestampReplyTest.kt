package com.MADproject.quoraforuniversities

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.MADproject.quoraforuniversities.data.CreateReplyDto
import com.MADproject.quoraforuniversities.data.GeminiService
import com.MADproject.quoraforuniversities.data.PostWithProfileDto
import com.MADproject.quoraforuniversities.data.ReplyWithProfileDto
import com.MADproject.quoraforuniversities.data.toUiModel
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import com.MADproject.quoraforuniversities.utils.TimestampFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs production composables with serialized database-response fixtures; no live DB writes. */
@RunWith(AndroidJUnit4::class)
class ContentTimestampReplyTest {
    @get:Rule val compose = createComposeRule()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val postResponse = """{"id":"post-a","user_id":"author-a","title":"Campus question","content":"Question body","theme":"General","created_at":"2020-01-01T12:00:00.123456+00:00","updated_at":"2026-10-20T12:00:00Z","profiles":{"id":"author-a","name":"Post Author","username":"post_author"}}"""
    private val replyResponse = """{"id":"reply-a","post_id":"post-a","user_id":"author-b","content":"Original answer","created_at":"2021-02-01T12:00:00+00:00","updated_at":"2026-10-20T12:00:00Z","profiles":{"id":"author-b","name":"Real Display Name","username":"real_handle"}}"""

    @Test fun homeAndDetailUseOriginalCreationTimesAfterFreshDecode() {
        val detail = mutableStateOf(false)
        // A new decode on each screen models the app refetching its server rows.
        compose.setContent {
            CampusQnATheme {
                val question = json.decodeFromString<PostWithProfileDto>(postResponse).toUiModel()
                if (detail.value) {
                    PostDetailPage(
                        question = question,
                        replies = listOf(json.decodeFromString<ReplyWithProfileDto>(replyResponse).toUiModel())
                    )
                } else {
                    HomeScreen(questions = listOf(question))
                }
            }
        }
        val postTime = TimestampFormatter.format("2020-01-01T12:00:00.123456+00:00")!!
        val replyTime = TimestampFormatter.format("2021-02-01T12:00:00+00:00")!!
        compose.onNodeWithText(postTime).assertIsDisplayed()
        compose.runOnIdle { detail.value = true }
        compose.onNodeWithText(postTime).assertIsDisplayed()
        compose.onNodeWithText(replyTime).assertIsDisplayed()
        compose.onNodeWithText("Reply").performScrollTo().assertIsEnabled()
        compose.runOnIdle { detail.value = false }
        compose.onNodeWithText(postTime).assertIsDisplayed()
        compose.runOnIdle { detail.value = true }
        compose.onNodeWithText(replyTime).assertIsDisplayed()
    }

    @Test fun mentionFocusCursorSaveRefetchAndReopenPreserveContentAndPost() {
        val question = json.decodeFromString<PostWithProfileDto>(postResponse).toUiModel()
        val serverRows = mutableStateOf(listOf(replyResponse))
        val reopened = mutableStateOf(false)
        val showingDetail = mutableStateOf(true)
        var savedPayload: String? = null
        // Exercise the existing offline moderation branch, without sending fixture
        // content to Gemini or requiring any production-service changes for tests.
        val localModeration = GeminiService().apply {
            javaClass.getDeclaredField("apiKey").apply { isAccessible = true }.set(this, "")
        }
        compose.setContent {
            CampusQnATheme {
                if (showingDetail.value) {
                    PostDetailPage(
                        question = question,
                        replies = serverRows.value.map { json.decodeFromString<ReplyWithProfileDto>(it).toUiModel() },
                        geminiService = localModeration,
                        onSubmitReply = { content, _, onResult ->
                            // Simulate the existing repository JSON contract and a
                            // server-generated created_at response, then refetch it.
                            savedPayload = json.encodeToString(CreateReplyDto("viewer", question.id, content))
                            val request = json.parseToJsonElement(savedPayload!!).jsonObject
                            val echoedContent = json.encodeToString(request.getValue("content").jsonPrimitive.content)
                            serverRows.value = listOf(
                                """{"id":"saved-reply","post_id":"post-a","user_id":"viewer","content":$echoedContent,"created_at":"2022-03-01T12:00:00.123456+00:00","updated_at":"2026-10-20T12:00:00Z","profiles":{"id":"viewer","name":"Viewer","username":"viewer_handle"}}"""
                            )
                            onResult(null)
                        }
                    )
                } else if (reopened.value) {
                    HomeScreen(questions = listOf(question))
                }
            }
        }
        val field = compose.onNode(hasSetTextAction())
        compose.onNodeWithText("Reply").performScrollTo().performClick()
        field.assertIsFocused().assertTextEquals("@real_handle ")
        assertEquals(TextRange(13), field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        field.performTextInput("Thanks for the answer!")
        // The keyboard can move the lazy reply item outside the composed viewport.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithText("Reply").performScrollTo().performClick()
        field.assertIsFocused().assertTextEquals("@real_handle Thanks for the answer!")
        assertEquals(TextRange(13), field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        compose.onNodeWithContentDescription("Send").performClick()
        compose.waitUntil(10_000) { savedPayload != null }
        compose.runOnIdle {
            val request = json.parseToJsonElement(savedPayload!!).jsonObject
            assertEquals("post-a", request.getValue("post_id").jsonPrimitive.content)
            assertEquals("viewer", request.getValue("user_id").jsonPrimitive.content)
            assertEquals("@real_handle Thanks for the answer!", request.getValue("content").jsonPrimitive.content)
            assertFalse(request.containsKey("created_at"))
            assertFalse(request.containsKey("updated_at"))
        }
        assertEquals("", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.runOnIdle { showingDetail.value = false; reopened.value = true }
        compose.runOnIdle { showingDetail.value = true }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        val savedBody = compose.onNodeWithText("@real_handle Thanks for the answer!")
        savedBody.performScrollTo().assertIsDisplayed()
        val annotatedBody = savedBody.fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertTrue(annotatedBody.spanStyles.any { it.start == 0 && it.end == 12 })
        compose.onNodeWithText(TimestampFormatter.format("2022-03-01T12:00:00.123456+00:00")!!).assertIsDisplayed()
        assertEquals("", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }
}
