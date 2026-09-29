package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class PostRepository {

    private val repliesMap = mutableMapOf<String, MutableList<ReplyUiModel>>()

    suspend fun getPosts(): Result<List<QuestionUiModel>> {
        return runCatching {
            val dbPosts = SupabaseClient.client.from("posts")
                .select(Columns.raw("*, profiles(*)"))
                .decodeList<PostWithProfileDto>()

            dbPosts.map { it.toUiModel() }
        }.recover {
            emptyList()
        }
    }

    suspend fun searchPosts(query: String): Result<List<QuestionUiModel>> {
        val currentPosts = getPosts().getOrDefault(emptyList())
        if (query.isBlank()) return Result.success(currentPosts)
        val filtered = currentPosts.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.body.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        return Result.success(filtered)
    }

    suspend fun createPost(
        title: String,
        content: String,
        theme: String,
        isAnonymous: Boolean
    ): Result<Unit> {
        return runCatching {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id ?: error("User not logged in")
            val email = currentUser.email.orEmpty()
            val username = email.substringBefore("@").ifBlank { "student" }

            // Ensure profile exists in 'profiles' table to satisfy foreign key constraint 'posts_user_id_fkey'
            val profileDto = ProfileDto(
                id = currentUserId,
                username = username,
                name = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
        }
    }

    suspend fun votePost(postId: String): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun getReplies(postId: String): Result<List<ReplyUiModel>> {
        val list = repliesMap[postId] ?: emptyList()
        return Result.success(list.toList())
    }

    suspend fun createReply(postId: String, content: String): Result<Unit> {
        val list = repliesMap.getOrPut(postId) { mutableListOf() }
        val newReply = ReplyUiModel(
            id = System.currentTimeMillis().toString(),
            authorName = "Student",
            body = content,
            voteCount = 0
        )
        list.add(newReply)
        return Result.success(Unit)
    }
}
