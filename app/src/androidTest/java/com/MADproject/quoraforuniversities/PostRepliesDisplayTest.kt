package com.MADproject.quoraforuniversities

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.data.PostDto
import com.MADproject.quoraforuniversities.data.PostRepository
import com.MADproject.quoraforuniversities.data.ReplyDto
import com.MADproject.quoraforuniversities.data.ReplyWithProfileDto
import com.MADproject.quoraforuniversities.data.SupabaseClient
import com.MADproject.quoraforuniversities.data.toUiModel
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import com.MADproject.quoraforuniversities.utils.TimestampFormatter
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostRepliesDisplayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allThreeMappedRepliesCanBeScrolledWithInputFixedAndMentionsIntact() {
        val json = Json { ignoreUnknownKeys = true }
        val replies = (1..3).map { index ->
            json.decodeFromString<ReplyWithProfileDto>(
                """{"id":"reply-$index","post_id":"post-three","content":"@student$index Answer $index","created_at":"2020-01-0${index}T12:00:00+00:00","profiles":{"id":"author-$index","name":"Author $index","username":"student$index"}}"""
            ).toUiModel()
        }
        assertEquals(3, replies.size)
        assertEquals(3, replies.map { it.id }.distinct().size)
        val question = QuestionUiModel(
            id = "post-three", title = "Post with three answers",
            body = "Question details. ".repeat(60), authorName = "Post author",
            isAnonymous = false, tags = emptyList(), answerCount = 99
        )
        compose.setContent {
            CampusQnATheme { PostDetailPage(question = question, replies = replies) }
        }
        val list = compose.onNode(hasScrollToIndexAction())
        list.performScrollToNode(hasText("3 Answers"))
        compose.onAllNodesWithText("3 Answers").onFirst().assertExists()
        compose.onNodeWithText("99 Answers").assertDoesNotExist()
        val input = compose.onNode(hasSetTextAction())
        val inputBounds = input.fetchSemanticsNode().boundsInRoot
        replies.forEachIndexed { index, reply ->
            list.performScrollToIndex(index + 2)
            compose.onNodeWithText(reply.body).assertIsDisplayed()
            compose.onNodeWithText(TimestampFormatter.format(reply.createdAt)!!).assertIsDisplayed()
            val annotated = compose.onNodeWithText(reply.body).fetchSemanticsNode()
                .config[SemanticsProperties.Text].single()
            assertTrue(annotated.spanStyles.any { it.start == 0 && it.end == 9 })
            input.assertIsDisplayed()
            assertEquals(inputBounds, input.fetchSemanticsNode().boundsInRoot)
        }
        // Exercise the final reply action after scrolling to the end of the list.
        compose.onAllNodesWithText("Reply").onLast().performScrollTo().performClick()
        input.assertIsFocused().assertTextEquals("@student3 ")
    }

    /** Opt-in, read-only integration check against an existing signed-in emulator. */
    @Test fun liveRepositoryReturnsEveryReplyForTheSelectedPost() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val title = arguments.getString("livePostTitle")
        assumeTrue("Pass livePostTitle to run the read-only integration check", !title.isNullOrBlank())
        withTimeout(30_000) {
            val client = SupabaseClient.client
            client.auth.awaitInitialization()
            assertNotNull("The live check requires the existing authenticated app session", client.auth.currentUserOrNull())
            val posts = client.from("posts").select { filter { eq("title", title!!) } }.decodeList<PostDto>()
            assertEquals("Select a unique existing post title", 1, posts.size)
            val postId = requireNotNull(posts[0].id)
            val databaseRows = client.from("replies").select {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<ReplyDto>()
            val mappedReplies = PostRepository().getReplies(postId).getOrThrow()
            val expectedCount = arguments.getString("expectedReplyCount")?.toInt()
            if (expectedCount != null) assertEquals(expectedCount, mappedReplies.size)
            assertEquals(databaseRows.map { it.id }, mappedReplies.map { it.id })
            assertEquals(databaseRows.map { it.content }, mappedReplies.map { it.body })
            assertEquals(databaseRows.map { it.createdAt }, mappedReplies.map { it.createdAt })
            assertTrue(databaseRows.all { it.postId == postId })
        }
    }
}
