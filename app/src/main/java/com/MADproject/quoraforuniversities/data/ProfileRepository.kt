package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class ProfileRepository {

    suspend fun getProfile(targetUserId: String? = null): Result<ProfileDto?> {
        return runCatching {
            val uid = targetUserId ?: SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@runCatching null
            val profiles = SupabaseClient.client.from("profiles")
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeList<ProfileDto>()
            profiles.firstOrNull()
        }
    }

    suspend fun updateProfile(profile: ProfileDto): Result<Unit> {
        return runCatching {
            SupabaseClient.client.from("profiles").upsert(profile)
        }
    }

    suspend fun getUserPosts(targetUserId: String? = null): Result<List<QuestionUiModel>> {
        return runCatching {
            val profileOwnerId = targetUserId ?: SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@runCatching emptyList<QuestionUiModel>()
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

            val dbPosts: List<PostWithProfileDto> = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)")) {
                    filter {
                        eq("user_id", profileOwnerId)
                    }
                }
                .decodeList<PostWithProfileDto>()

            val allReplies: List<ReplyMinimalDto> = runCatching {
                SupabaseClient.client.from("replies")
                    .select(Columns.raw("post_id"))
                    .decodeList<ReplyMinimalDto>()
            }.getOrDefault(emptyList<ReplyMinimalDto>())

            val replyCounts: Map<String, Int> = allReplies.groupBy { it.postId }.mapValues { it.value.size }

            val allVotes: List<VoteMinimalDto> = runCatching {
                SupabaseClient.client.from("votes")
                    .select(Columns.raw("post_id, vote_type, user_id"))
                    .decodeList<VoteMinimalDto>()
            }.getOrDefault(emptyList<VoteMinimalDto>()).filter { it.postId != null }

            val upvoteCounts: Map<String, Int> = allVotes.filter { it.voteType == 1 }
                .groupBy { it.postId ?: "" }
                .mapValues { it.value.size }

            val downvoteCounts: Map<String, Int> = allVotes.filter { it.voteType == -1 }
                .groupBy { it.postId ?: "" }
                .mapValues { it.value.size }

            val userVotesMap: Map<String, Int> = if (currentUserId != null) {
                allVotes.filter { it.userId == currentUserId }
                    .associate { (it.postId ?: "") to it.voteType }
            } else emptyMap()

            dbPosts.map { dto: PostWithProfileDto ->
                val postId = dto.id ?: ""
                val upvotes = upvoteCounts[postId] ?: 0
                val downvotes = downvoteCounts[postId] ?: 0
                dto.toUiModel(
                    voteCount = upvotes - downvotes,
                    answerCount = replyCounts[postId] ?: 0,
                    upvoteCount = upvotes,
                    downvoteCount = downvotes,
                    currentUserVote = userVotesMap[postId] ?: 0
                )
            }
        }.recover {
            emptyList()
        }
    }
}