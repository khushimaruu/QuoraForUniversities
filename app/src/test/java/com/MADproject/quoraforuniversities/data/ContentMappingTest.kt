package com.MADproject.quoraforuniversities.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ContentMappingTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test fun mapsDatabaseTimesAndProfileHandleWithoutChangingReplyContent() {
        val dto = json.decodeFromString<ReplyWithProfileDto>(
            """{"id":"reply","post_id":"post","user_id":"user","content":"@riya Thanks!","created_at":"2026-10-20T11:45:00.123456+00:00","profiles":{"id":"user","name":"Sam Sharma","username":"sam"}}"""
        )
        val reply = dto.toUiModel()
        assertEquals(dto.createdAt, reply.createdAt)
        assertEquals("sam", reply.authorUsername)
        assertEquals("Sam Sharma", reply.authorName)
        assertEquals("@riya Thanks!", reply.body)
        assertEquals("2026-10-20T11:45:00Z", PostDto(createdAt = "2026-10-20T11:45:00Z").toUiModel().createdAt)
        assertEquals("2026-10-20T11:45:00Z", PostWithProfileDto(createdAt = "2026-10-20T11:45:00Z").toUiModel().createdAt)
        assertEquals(dto.createdAt, ReplyDto(createdAt = dto.createdAt).toUiModel().createdAt)
    }

    @Test fun insertPayloadsOmitTimestampsAndKeepPostAssociationAndMention() {
        val reply = json.parseToJsonElement(json.encodeToString(CreateReplyDto("user", "post", "@riya Thanks!"))).jsonObject
        assertEquals("\"post\"", reply["post_id"].toString())
        assertEquals("\"@riya Thanks!\"", reply["content"].toString())
        assertFalse(reply.containsKey("created_at"))
        val post = json.parseToJsonElement(json.encodeToString(CreatePostDto("user", "Title", "Body", "General"))).jsonObject
        assertFalse(post.containsKey("created_at"))
    }

    @Test fun freshRefetchesKeepCreationTimesWhenUpdatedAtChanges() {
        val createdAt = "2020-01-01T12:00:00.123456+00:00"
        listOf("2026-01-01T12:00:00Z", "2026-10-20T12:00:00Z").forEach { updatedAt ->
            // Fresh serializers model decoding an existing row after an app restart.
            val freshJson = Json { ignoreUnknownKeys = true }
            val post = freshJson.decodeFromString<PostWithProfileDto>(
                """{"id":"post","created_at":"$createdAt","updated_at":"$updatedAt"}"""
            )
            val reply = freshJson.decodeFromString<ReplyWithProfileDto>(
                """{"id":"reply","post_id":"post","content":"@riya Thanks!","created_at":"$createdAt","updated_at":"$updatedAt","profiles":{"id":"user","name":"Different Display Name","username":"actual_handle"}}"""
            )
            assertEquals(createdAt, post.toUiModel().createdAt)
            assertEquals(createdAt, reply.toUiModel().createdAt)
            assertEquals("@riya Thanks!", reply.toUiModel().body)
            assertEquals("actual_handle", reply.toUiModel().authorUsername)
            assertEquals("post", reply.postId)
        }
    }
}
