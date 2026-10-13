
package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class PostRepository {

    // FETCH ALL POSTS
    suspend fun getPosts(): Result<List<QuestionUiModel>> {
        return runCatching {
            val dbPosts = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)"))
                .decodeList<PostWithProfileDto>()

            val allReplies = runCatching {
                SupabaseClient.client.from("replies")
                    .select(Columns.raw("id, post_id"))
                    .decodeList<ReplyDto>()
            }.getOrDefault(emptyList())

            val replyCounts = allReplies.groupBy { it.postId }.mapValues { it.value.size }

            dbPosts.map { dto ->
                dto.toUiModel(
                    answerCount = replyCounts[dto.id] ?: 0
                )
            }
        }.recover {
            emptyList()
        }
    }

    // SEARCH POSTS DIRECTLY THROUGH SUPABASE
    suspend fun searchPosts(
        query: String
    ): Result<List<QuestionUiModel>> {

        val searchText = query.trim()

        if (searchText.isBlank()) {
            return Result.success(emptyList())
        }

        return try {

            val dbPosts = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)")) {
                    filter {
                        or {
                            ilike("title", "%$searchText%")
                            ilike("content", "%$searchText%")
                            ilike("theme", "%$searchText%")
                        }
                    }
                }
                .decodeList<PostWithProfileDto>()

            val allReplies = runCatching {
                SupabaseClient.client.from("replies")
                    .select(Columns.raw("id, post_id"))
                    .decodeList<ReplyDto>()
            }.getOrDefault(emptyList())

            val replyCounts = allReplies.groupBy { it.postId }.mapValues { it.value.size }

            Result.success(
                dbPosts.map { dto ->
                    dto.toUiModel(
                        answerCount = replyCounts[dto.id] ?: 0
                    )
                }
            )

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // CREATE NEW POST
    suspend fun createPost(
        title: String,
        content: String,
        theme: String,
        isAnonymous: Boolean
    ): Result<Unit> {

        return runCatching {

            val currentUser =
                SupabaseClient.client.auth.currentUserOrNull()

            val currentUserId =
                currentUser?.id ?: error("User not logged in")

            val email = currentUser.email.orEmpty()

            val username = email.substringBefore("@")
                .ifBlank { "student" }

            val profileDto = ProfileDto(
                id = currentUserId,
                username = username,
                name = username.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase()
                    } else {
                        it.toString()
                    }
                }
            )

            runCatching {
                SupabaseClient.client.from("profiles")
                    .upsert(profileDto)
            }

            val dto = CreatePostDto(
                userId = currentUserId,
                title = title,
                content = content,
                theme = theme,
                isAnonymous = isAnonymous
            )

            SupabaseClient.client.from("posts").insert(dto)
        }
    }

    // VOTE POST
    suspend fun votePost(postId: String): Result<Unit> {
        return Result.success(Unit)
    }

    // FETCH REPLIES
    suspend fun getReplies(
        postId: String
    ): Result<List<ReplyUiModel>> {
        return runCatching {
            val dbReplies = SupabaseClient.client.from("replies")
                .select(Columns.raw("*, profiles(*)")) {
                    filter {
                        eq("post_id", postId)
                    }
                }
                .decodeList<ReplyWithProfileDto>()

            dbReplies.map { it.toUiModel() }
        }.recover {
            runCatching {
                val plainReplies = SupabaseClient.client.from("replies")
                    .select {
                        filter {
                            eq("post_id", postId)
                        }
                    }
                    .decodeList<ReplyDto>()
                plainReplies.map { it.toUiModel("Student") }
            }.getOrDefault(emptyList())
        }
    }

    // CREATE REPLY
    suspend fun createReply(
        postId: String,
        content: String
    ): Result<Unit> {
        return runCatching {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: error("User not logged in")
            val email = currentUser.email.orEmpty()
            val username = email.substringBefore("@").ifBlank { "student" }

            val profileDto = ProfileDto(
                id = currentUserId,
                username = username,
                name = username.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase()
                    } else {
                        it.toString()
                    }
                }
            )

            runCatching {
                SupabaseClient.client.from("profiles").upsert(profileDto)
            }

            val dto = CreateReplyDto(
                userId = currentUserId,
                postId = postId,
                content = content.trim()
            )

            SupabaseClient.client.from("replies").insert(dto)
        }
    }
}
