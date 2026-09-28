package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class PostRepository {

    private val postgrest = SupabaseClient.client.postgrest
    private val auth = SupabaseClient.client.auth

    suspend fun getPosts(): Result<List<QuestionUiModel>> {
        return runCatching {
            val posts = postgrest["posts"].select().decodeList<PostDto>()
            val profilesMap = runCatching {
                postgrest["profiles"].select().decodeList<ProfileDto>()
                    .associateBy { it.id }
            }.getOrDefault(emptyMap())

            val votesMap = runCatching {
                postgrest["votes"].select().decodeList<VoteDto>()
                    .filter { it.postId != null }
                    .groupingBy { it.postId!! }
                    .eachCount()
            }.getOrDefault(emptyMap())

            val repliesMap = runCatching {
                postgrest["replies"].select().decodeList<ReplyDto>()
                    .groupingBy { it.postId }
                    .eachCount()
            }.getOrDefault(emptyMap())

            posts.map { post ->
                val author = if (post.userId != null) profilesMap[post.userId] else null
                val authorName = author?.name ?: author?.username ?: "Student"
                val votes = post.id?.let { votesMap[it] } ?: 0
                val answers = post.id?.let { repliesMap[it] } ?: 0

                post.toUiModel(
                    authorName = authorName,
                    voteCount = votes,
                    answerCount = answers
                )
            }
        }
    }

    suspend fun searchPosts(query: String): Result<List<QuestionUiModel>> {
        return getPosts().map { list ->
            if (query.isBlank()) list
            else list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }
    }

    suspend fun createPost(
        title: String,
        content: String,
        theme: String,
        isAnonymous: Boolean
    ): Result<Unit> {
        return runCatching {
            val userId = auth.currentUserOrNull()?.id
            val newPost = CreatePostDto(
                userId = userId,
                title = title,
                content = content,
                theme = theme,
                isAnonymous = isAnonymous
            )
            postgrest["posts"].insert(newPost)
        }
    }

    suspend fun votePost(postId: String): Result<Unit> {
        return runCatching {
            val userId = auth.currentUserOrNull()?.id ?: return@runCatching
            val newVote = CreateVoteDto(
                userId = userId,
                postId = postId,
                voteType = "upvote"
            )
            postgrest["votes"].insert(newVote)
        }
    }

    suspend fun getReplies(postId: String): Result<List<ReplyUiModel>> {
        return runCatching {
            val replies = postgrest["replies"]
                .select {
                    filter {
                        eq("post_id", postId)
                    }
                }
                .decodeList<ReplyDto>()

            val profilesMap = runCatching {
                postgrest["profiles"].select().decodeList<ProfileDto>()
                    .associateBy { it.id }
            }.getOrDefault(emptyMap())

            val votesMap = runCatching {
                postgrest["votes"].select().decodeList<VoteDto>()
                    .filter { it.replyId != null }
                    .groupingBy { it.replyId!! }
                    .eachCount()
            }.getOrDefault(emptyMap())

            replies.map { reply ->
                val author = if (reply.userId != null) profilesMap[reply.userId] else null
                val authorName = author?.name ?: author?.username ?: "Student"
                val votes = reply.id?.let { votesMap[it] } ?: 0

                reply.toUiModel(
                    authorName = authorName,
                    voteCount = votes
                )
            }
        }
    }

    suspend fun createReply(postId: String, content: String): Result<Unit> {
        return runCatching {
            val userId = auth.currentUserOrNull()?.id
            val newReply = CreateReplyDto(
                userId = userId,
                postId = postId,
                content = content
            )
            postgrest["replies"].insert(newReply)
        }
    }
}
