package com.MADproject.quoraforuniversities.data

import android.util.Log
import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.OffsetDateTime

class PostRepository {
    private val replyVoteMutex = Mutex()
    private val reportMutex = Mutex()

    companion object {
        const val REPORT_HIDE_THRESHOLD = 3
        private const val TAG = "VOTE_SYSTEM"
    }

    suspend fun getPosts(excludeAnonymousAuthorId: String? = null): Result<List<QuestionUiModel>> {
        return runCatching<List<QuestionUiModel>> {
            val dbPosts = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)")) {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<PostWithProfileDto>()

            val allReplies = runCatching {
                SupabaseClient.client.from("replies")
                    .select()
                    .decodeList<ReplyMinimalDto>()
            }.getOrDefault(emptyList())

            val replyCounts: Map<String, Int> = allReplies
                .groupBy { it.postId }
                .mapValues { it.value.size }

            // Fetch votes safely with detailed logging
            val allVotes: List<VoteDto> = try {
                val votes = SupabaseClient.client.from("votes")
                    .select()
                    .decodeList<VoteDto>()
                Log.d(TAG, "SUCCESS: Fetched ${votes.size} votes from Supabase: $votes")
                votes.filter { !it.postId.isNullOrBlank() }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Failed to fetch votes from Supabase: ${e.message}", e)
                emptyList()
            }

            val upvoteCounts: Map<String, Int> = allVotes
                .filter { it.voteType == 1 }
                .groupBy { it.postId!! }
                .mapValues { it.value.size }

            val downvoteCounts: Map<String, Int> = allVotes
                .filter { it.voteType == -1 }
                .groupBy { it.postId!! }
                .mapValues { it.value.size }

            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
            val userVotesMap: Map<String, Int> = if (currentUserId != null) {
                allVotes.filter { it.userId == currentUserId }
                    .associate { it.postId!! to it.voteType }
            } else emptyMap()

            val allReports = runCatching {
                SupabaseClient.client.from("reports")
                    .select()
                    .decodeList<ReportDto>()
            }.getOrDefault(emptyList()).filter { it.postId != null && it.status != "dismissed" }

            val postReportCounts = allReports.groupBy { it.postId!! }.mapValues { it.value.size }
            val userReportedPostIds = if (currentUserId != null) {
                allReports.filter { it.reporterId == currentUserId }.mapNotNull { it.postId }.toSet()
            } else emptySet()

            dbPosts.filter { dto ->
                val postId = dto.id ?: ""
                (postReportCounts[postId] ?: 0) < REPORT_HIDE_THRESHOLD && !userReportedPostIds.contains(postId) &&
                    !(excludeAnonymousAuthorId != null && dto.isAnonymous && dto.userId == excludeAnonymousAuthorId)
            }.map { dto: PostWithProfileDto ->
                val id = dto.id ?: ""
                val up = upvoteCounts[id] ?: 0
                val down = downvoteCounts[id] ?: 0
                dto.toUiModel(
                    voteCount = up - down,
                    answerCount = replyCounts[id] ?: 0,
                    upvoteCount = up,
                    downvoteCount = down,
                    currentUserVote = userVotesMap[id] ?: 0,
                    currentUserId = currentUserId
                )
            }
        }.onFailure { e ->
            Log.e(TAG, "ERROR in getPosts(): ${e.message}", e)
        }
    }

    suspend fun votePost(postId: String, voteType: Int): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull()
                ?: error("User not logged in. Cannot vote.")

            Log.d(TAG, "Attempting vote: userId=${user.id}, postId=$postId, voteType=$voteType")

            // Check if vote exists
            val existing = SupabaseClient.client.from("votes").select {
                filter {
                    eq("user_id", user.id)
                    eq("post_id", postId)
                }
            }.decodeList<VoteDto>().firstOrNull()

            if (existing == null) {
                Log.d(TAG, "Inserting new vote...")
                SupabaseClient.client.from("votes").insert(
                    CreateVoteDto(
                        userId = user.id,
                        postId = postId,
                        voteType = voteType
                    )
                )
                Log.d(TAG, "Vote INSERT successful!")
            } else if (existing.voteType != voteType) {
                Log.d(TAG, "Updating existing vote (${existing.id}) from ${existing.voteType} to $voteType...")
                SupabaseClient.client.from("votes").update(
                    {
                        set("vote_type", voteType)
                    }
                ) {
                    filter {
                        eq("id", existing.id!!)
                    }
                }
                Log.d(TAG, "Vote UPDATE successful!")
            } else {
                Log.d(TAG, "Toggling vote off. Deleting vote ${existing.id}...")
                SupabaseClient.client.from("votes").delete {
                    filter {
                        eq("id", existing.id!!)
                    }
                }
                Log.d(TAG, "Vote DELETE successful!")
            }
        }.onFailure { e ->
            Log.e(TAG, "CRITICAL ERROR in votePost(): ${e.message}", e)
        }
    }

    suspend fun createPost(title: String, content: String, theme: String, isAnonymous: Boolean): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("User not logged in")
            SupabaseClient.client.from("posts").insert(
                CreatePostDto(
                    userId = user.id,
                    title = title,
                    content = content,
                    theme = theme,
                    isAnonymous = isAnonymous
                )
            )
        }
    }

    suspend fun reportPost(postId: String, reason: String): Result<Unit> =
        reportContent(postId = postId, reason = reason)

    // Request the deleted row: RLS may otherwise return success with zero rows deleted.
    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("Sign in to delete content.")
        require(postId.isNotBlank())
        val deleted = SupabaseClient.client.from("posts").delete {
            select(Columns.raw("id"))
            filter { eq("id", postId); eq("user_id", user.id) }
        }.decodeList<PostDto>()
        check(deleted.any { it.id == postId }) { "Content is unavailable or cannot be deleted by this account." }
    }

    suspend fun deleteReply(replyId: String): Result<Unit> = runCatching {
        val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("Sign in to delete content.")
        require(replyId.isNotBlank())
        val deleted = SupabaseClient.client.from("replies").delete {
            select(Columns.raw("id"))
            filter { eq("id", replyId); eq("user_id", user.id) }
        }.decodeList<ReplyDto>()
        check(deleted.any { it.id == replyId }) { "Content is unavailable or cannot be deleted by this account." }
    }

    suspend fun reportReply(replyId: String, reason: String): Result<Unit> =
        reportContent(replyId = replyId, reason = reason)

    private suspend fun reportContent(postId: String? = null, replyId: String? = null, reason: String): Result<Unit> = reportMutex.withLock {
        runCatching {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("User must be logged in to report")
            require(reason.isNotBlank()) { "Please select a report reason." }
            val targetId = postId ?: replyId ?: error("Content is missing.")
            require(targetId.isNotBlank()) { "Content is missing." }
            val contentType = if (postId != null) "post" else "reply"
            val ownerId = if (postId != null) {
                SupabaseClient.client.from("posts").select { filter { eq("id", targetId) } }
                    .decodeList<PostDto>().firstOrNull()?.userId
            } else {
                SupabaseClient.client.from("replies").select { filter { eq("id", targetId) } }
                    .decodeList<ReplyDto>().firstOrNull()?.userId
            } ?: error("Content is no longer available.")
            check(ownerId != user.id) { "You cannot report your own $contentType." }
            val existing = SupabaseClient.client.from("reports").select {
                filter {
                    eq("reporter_id", user.id)
                    eq(if (postId != null) "post_id" else "reply_id", targetId)
                }
            }.decodeList<ReportDto>()
            check(existing.isEmpty()) { "You have already reported this $contentType." }
            SupabaseClient.client.from("reports").insert(
                CreateReportDto(
                    reporterId = user.id,
                    postId = postId,
                    replyId = replyId,
                    reason = reason,
                    status = "pending"
                )
            )
            Unit
        }
    }

    suspend fun searchPosts(query: String): Result<List<QuestionUiModel>> {
        val searchText = query.trim()
        if (searchText.isBlank()) return Result.success(emptyList())

        return runCatching<List<QuestionUiModel>> {
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
                    .select()
                    .decodeList<ReplyMinimalDto>()
            }.getOrDefault(emptyList())

            val replyCounts = allReplies.groupBy { it.postId }.mapValues { it.value.size }

            val allVotes = runCatching {
                SupabaseClient.client.from("votes")
                    .select()
                    .decodeList<VoteDto>()
            }.getOrDefault(emptyList()).filter { it.postId != null }

            val upvoteCounts = allVotes.filter { it.voteType == 1 }.groupBy { it.postId ?: "" }.mapValues { it.value.size }
            val downvoteCounts = allVotes.filter { it.voteType == -1 }.groupBy { it.postId ?: "" }.mapValues { it.value.size }

            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
            val userVotesMap = if (currentUserId != null) {
                allVotes.filter { it.userId == currentUserId && it.postId != null }.associate { (it.postId ?: "") to it.voteType }
            } else emptyMap()

            val allReports = runCatching {
                SupabaseClient.client.from("reports")
                    .select()
                    .decodeList<ReportDto>()
            }.getOrDefault(emptyList()).filter { it.postId != null && it.status != "dismissed" }

            val postReportCounts = allReports.groupBy { it.postId ?: "" }.mapValues { it.value.size }
            val userReportedPostIds = if (currentUserId != null) {
                allReports.filter { it.reporterId == currentUserId }.mapNotNull { it.postId }.toSet()
            } else emptySet()

            dbPosts.filter { dto ->
                val postId = dto.id ?: ""
                (postReportCounts[postId] ?: 0) < REPORT_HIDE_THRESHOLD && !userReportedPostIds.contains(postId)
            }.map { dto: PostWithProfileDto ->
                val id = dto.id ?: ""
                val up = upvoteCounts[id] ?: 0
                val down = downvoteCounts[id] ?: 0
                dto.toUiModel(
                    voteCount = up - down,
                    answerCount = replyCounts[id] ?: 0,
                    upvoteCount = up,
                    downvoteCount = down,
                    currentUserVote = userVotesMap[id] ?: 0,
                    currentUserId = currentUserId
                )
            }.sortedWith(
                compareByDescending<QuestionUiModel> { it.upvoteCount }
                    .thenByDescending {
                        runCatching { OffsetDateTime.parse(it.createdAt).toInstant() }.getOrDefault(Instant.MIN)
                    }
            )
        }
    }

    suspend fun getReplies(postId: String): Result<List<ReplyUiModel>> {
        return runCatching<List<ReplyUiModel>> {
            val dbReplies = SupabaseClient.client.from("replies")
                .select(Columns.raw("*, profiles(*)")) {
                    filter {
                        eq("post_id", postId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ReplyWithProfileDto>()
            if (dbReplies.isEmpty()) return@runCatching emptyList()
            val votes = SupabaseClient.client.from("votes").select {
                filter { isIn("reply_id", dbReplies.mapNotNull { it.id }) }
            }.decodeList<VoteDto>().groupBy { it.replyId }
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            visibleReplies(dbReplies).map { reply ->
                val replyVotes = votes[reply.id].orEmpty()
                val up = replyVotes.count { it.voteType == 1 }
                val down = replyVotes.count { it.voteType == -1 }
                reply.toUiModel(
                    voteCount = up - down, upvoteCount = up, downvoteCount = down,
                    currentUserVote = replyVotes.firstOrNull { it.userId == userId }?.voteType ?: 0,
                    currentUserId = userId
                )
            }
        }
    }

    suspend fun createReply(postId: String, content: String, parentReplyId: String? = null): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("User not logged in")
            SupabaseClient.client.from("replies").insert(CreateReplyDto(user.id, postId, content, parentReplyId))
        }
    }

    suspend fun voteReply(replyId: String, voteType: Int): Result<Unit> = replyVoteMutex.withLock {
        runCatching {
            require(voteType == 1 || voteType == -1)
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("Sign in to vote on replies.")
            val existing = SupabaseClient.client.from("votes").select {
                filter { eq("user_id", user.id); eq("reply_id", replyId) }
            }.decodeList<VoteDto>()
            if (existing.any { it.voteType == voteType }) {
                SupabaseClient.client.from("votes").delete {
                    filter { eq("user_id", user.id); eq("reply_id", replyId) }
                }
            } else {
                SupabaseClient.client.from("votes").upsert(
                    CreateVoteDto(userId = user.id, replyId = replyId, voteType = voteType)
                ) { onConflict = "reply_id,user_id" }
            }
            Unit
        }
    }

    internal suspend fun visibleReplies(replies: List<ReplyWithProfileDto>): List<ReplyWithProfileDto> {
        if (replies.isEmpty()) return replies
        val reports = SupabaseClient.client.from("reports").select {
            filter { isIn("reply_id", replies.mapNotNull { it.id }) }
        }.decodeList<ReportDto>().filter { it.status != "dismissed" }
        val counts = reports.groupingBy { it.replyId }.eachCount()
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
        val reportedIds = reports.filter { it.reporterId == userId }.mapNotNull { it.replyId }.toSet()
        return replies.filter { (counts[it.id] ?: 0) < REPORT_HIDE_THRESHOLD && it.id !in reportedIds }
    }

    suspend fun bookmarkPost(postId: String): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("Not logged in")
            SupabaseClient.client.from("bookmarks").insert(CreateBookmarkDto(user.id, postId))
        }
    }

    suspend fun unbookmarkPost(postId: String): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("Not logged in")
            SupabaseClient.client.from("bookmarks").delete {
                filter {
                    eq("user_id", user.id)
                    eq("post_id", postId)
                }
            }
        }
    }

    suspend fun getSavedPostIds(): Result<Set<String>> {
        return runCatching<Set<String>> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@runCatching emptySet()
            SupabaseClient.client.from("bookmarks").select {
                filter {
                    eq("user_id", user.id)
                }
            }.decodeList<BookmarkDto>().mapNotNull { it.postId }.toSet()
        }
    }

    suspend fun getSavedPosts(): Result<List<QuestionUiModel>> {
        return runCatching<List<QuestionUiModel>> {
            val ids = getSavedPostIds().getOrThrow()
            if (ids.isEmpty()) return@runCatching emptyList()
            getPosts().getOrThrow().filter { it.id in ids }
        }
    }
}
