package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class PostRepository {

    // FETCH ALL POSTS WITH LIVE VOTE COUNTS FROM ALL USERS
    suspend fun getPosts(): Result<List<QuestionUiModel>> {
        return runCatching<List<QuestionUiModel>> {
            val dbPosts = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)"))
                .decodeList<PostWithProfileDto>()

            val allReplies = runCatching {
                SupabaseClient.client.from("replies")
                    .select(Columns.raw("id, post_id"))
                    .decodeList<ReplyDto>()
            }.getOrDefault(emptyList())

            val replyCounts = allReplies.groupBy { it.postId }.mapValues { it.value.size }

            // Fetch votes across all users from Supabase
            val allVotes = runCatching {
                SupabaseClient.client.from("votes")
                    .select()
                    .decodeList<VoteDto>()
            }.getOrElse { e ->
                android.util.Log.e("VOTE_DEBUG", "Error fetching votes: ${e.message}", e)
                emptyList()
            }.filter { it.postId != null }

            val upvoteCounts = allVotes.filter { it.voteType == 1 }
                .groupBy { it.postId ?: "" }
                .mapValues { it.value.size }

            val downvoteCounts = allVotes.filter { it.voteType == -1 }
                .groupBy { it.postId ?: "" }
                .mapValues { it.value.size }

            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
            val userVotes = if (currentUserId != null) {
                allVotes.filter { it.userId == currentUserId && it.postId != null }
                    .associate { (it.postId ?: "") to it.voteType }
            } else {
                emptyMap()
            }

            dbPosts.map { dto ->
                val upvotes = upvoteCounts[dto.id ?: ""] ?: 0
                val downvotes = downvoteCounts[dto.id ?: ""] ?: 0
                dto.toUiModel(
                    voteCount = upvotes - downvotes,
                    answerCount = replyCounts[dto.id ?: ""] ?: 0,
                    upvoteCount = upvotes,
                    downvoteCount = downvotes,
                    currentUserVote = userVotes[dto.id ?: ""] ?: 0
                )
            }
        }.recover {
            emptyList()
        }
    }

    // SEARCH POSTS DIRECTLY THROUGH SUPABASE
    suspend fun searchPosts(query: String): Result<List<QuestionUiModel>> {
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

            val allVotes = runCatching {
                SupabaseClient.client.from("votes")
                    .select()
                    .decodeList<VoteDto>()
            }.getOrDefault(emptyList()).filter { it.postId != null }

            val upvoteCounts = allVotes.filter { it.voteType == 1 }
                .groupBy { it.postId ?: "" }
                .mapValues { it.value.size }

            val downvoteCounts = allVotes.filter { it.voteType == -1 }
                .groupBy { it.postId ?: "" }
                .mapValues { it.value.size }

            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
            val userVotes = if (currentUserId != null) {
                allVotes.filter { it.userId == currentUserId && it.postId != null }
                    .associate { (it.postId ?: "") to it.voteType }
            } else {
                emptyMap()
            }

            Result.success(
                dbPosts.map { dto ->
                    val upvotes = upvoteCounts[dto.id ?: ""] ?: 0
                    val downvotes = downvoteCounts[dto.id ?: ""] ?: 0
                    dto.toUiModel(
                        voteCount = upvotes - downvotes,
                        answerCount = replyCounts[dto.id ?: ""] ?: 0,
                        upvoteCount = upvotes,
                        downvoteCount = downvotes,
                        currentUserVote = userVotes[dto.id ?: ""] ?: 0
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
        return runCatching<Unit> {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: error("User not logged in")
            val email = currentUser.email.orEmpty()
            val username = email.substringBefore("@").ifBlank { "student" }

            val profileDto = ProfileDto(
                id = currentUserId,
                username = username,
                name = username.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            )

            runCatching {
                SupabaseClient.client.from("profiles").upsert(profileDto)
            }

            val dto = CreatePostDto(
                userId = currentUserId,
                title = title,
                content = content,
                theme = theme,
                isAnonymous = isAnonymous
            )

            SupabaseClient.client.from("posts").insert(dto)
            Unit
        }
    }

    // VOTE POST: Explicitly typed with <Unit> to prevent Result<Any> mismatch
    suspend fun votePost(postId: String, voteType: Int): Result<Unit> {
        return runCatching<Unit> {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                ?: error("User not logged in")

            val existingVotes = SupabaseClient.client.from("votes")
                .select {
                    filter {
                        eq("user_id", currentUser.id)
                        eq("post_id", postId)
                    }
                }
                .decodeList<VoteDto>()

            val existingVote = existingVotes.firstOrNull()

            if (existingVote == null) {
                SupabaseClient.client.from("votes").insert(
                    CreateVoteDto(
                        userId = currentUser.id,
                        postId = postId,
                        voteType = voteType
                    )
                )
            } else if (existingVote.voteType != voteType) {
                SupabaseClient.client.from("votes").update(
                    {
                        set("vote_type", voteType)
                    }
                ) {
                    filter {
                        eq("user_id", currentUser.id)
                        eq("post_id", postId)
                    }
                }
            }
            Unit
        }.onFailure { e ->
            android.util.Log.e("VOTE_DEBUG", "Failed to vote in Supabase: ${e.message}", e)
        }
    }

    suspend fun votePost(postId: String, voteType: String): Result<Unit> {
        return votePost(postId, if (voteType == "downvote") -1 else 1)
    }

    suspend fun votePost(postId: String): Result<Unit> = votePost(postId, 1)
    suspend fun upvotePost(postId: String): Result<Unit> = votePost(postId, 1)
    suspend fun downvotePost(postId: String): Result<Unit> = votePost(postId, -1)

    // FETCH REPLIES
    suspend fun getReplies(postId: String): Result<List<ReplyUiModel>> {
        return runCatching<List<ReplyUiModel>> {
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
    suspend fun createReply(postId: String, content: String): Result<Unit> {
        return runCatching<Unit> {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: error("User not logged in")
            val email = currentUser.email.orEmpty()
            val username = email.substringBefore("@").ifBlank { "student" }

            val profileDto = ProfileDto(
                id = currentUserId,
                username = username,
                name = username.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
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
            Unit
        }
    }

    // BOOKMARK / SAVE POST
    suspend fun bookmarkPost(postId: String): Result<Unit> {
        return runCatching<Unit> {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: error("User not logged in")
            val dto = CreateBookmarkDto(userId = currentUserId, postId = postId)
            SupabaseClient.client.from("bookmarks").insert(dto)
            Unit
        }
    }

    // UNBOOKMARK / REMOVE SAVED POST
    suspend fun unbookmarkPost(postId: String): Result<Unit> {
        return runCatching<Unit> {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: error("User not logged in")
            SupabaseClient.client.from("bookmarks").delete {
                filter {
                    eq("user_id", currentUserId)
                    eq("post_id", postId)
                }
            }
            Unit
        }
    }

    // FETCH SAVED POST IDS FOR CURRENT USER
    suspend fun getSavedPostIds(): Set<String> {
        return runCatching {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: return emptySet()
            val bookmarks = SupabaseClient.client.from("bookmarks")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<BookmarkDto>()
            bookmarks.mapNotNull { it.postId }.toSet()
        }.getOrDefault(emptySet())
    }

    // FETCH SAVED POSTS
    suspend fun getSavedPosts(): Result<List<QuestionUiModel>> {
        return runCatching<List<QuestionUiModel>> {
            val savedIds = getSavedPostIds()
            if (savedIds.isEmpty()) return Result.success(emptyList())
            val allPosts = getPosts().getOrDefault(emptyList())
            allPosts.filter { it.id in savedIds }
        }
    }
}