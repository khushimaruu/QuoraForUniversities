package com.MADproject.quoraforuniversities.data

import android.util.Log
import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class PostRepository {

    companion object {
        const val REPORT_HIDE_THRESHOLD = 3
        private const val TAG = "VOTE_SYSTEM"
    }

    suspend fun getPosts(): Result<List<QuestionUiModel>> {
        return runCatching<List<QuestionUiModel>> {
            val dbPosts = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)"))
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
                    currentUserVote = userVotesMap[id] ?: 0
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

    suspend fun reportPost(postId: String, reason: String): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("User must be logged in to report")
            SupabaseClient.client.from("reports").insert(
                CreateReportDto(
                    reporterId = user.id,
                    postId = postId,
                    reason = reason,
                    status = "pending"
                )
            )
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
                    currentUserVote = userVotesMap[id] ?: 0
                )
            }
        }
    }

    suspend fun getReplies(postId: String): Result<List<ReplyUiModel>> {
        return runCatching<List<ReplyUiModel>> {
            SupabaseClient.client.from("replies")
                .select(Columns.raw("*, profiles(*)")) {
                    filter {
                        eq("post_id", postId)
                    }
                }
                .decodeList<ReplyWithProfileDto>()
                .map { it.toUiModel() }
        }
    }

    suspend fun createReply(postId: String, content: String): Result<Unit> {
        return runCatching<Unit> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: error("User not logged in")
            SupabaseClient.client.from("replies").insert(CreateReplyDto(user.id, postId, content))
        }
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

    suspend fun getSavedPostIds(): Set<String> {
        return runCatching<Set<String>> {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@runCatching emptySet()
            SupabaseClient.client.from("bookmarks").select {
                filter {
                    eq("user_id", user.id)
                }
            }.decodeList<BookmarkDto>().mapNotNull { it.postId }.toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun getSavedPosts(): Result<List<QuestionUiModel>> {
        return runCatching<List<QuestionUiModel>> {
            val ids = getSavedPostIds()
            if (ids.isEmpty()) return@runCatching emptyList()
            getPosts().getOrThrow().filter { it.id in ids }
        }
    }
}