package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class ProfileRepository {

    private val postgrest = SupabaseClient.client.postgrest
    private val auth = SupabaseClient.client.auth

    suspend fun getProfile(targetUserId: String? = null): Result<ProfileDto?> {
        return runCatching {
            val uid = targetUserId ?: auth.currentUserOrNull()?.id ?: return@runCatching null
            val profiles = postgrest["profiles"]
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
            postgrest["profiles"].upsert(profile)
        }
    }

    suspend fun getUserPosts(targetUserId: String? = null): Result<List<QuestionUiModel>> {
        return runCatching {
            val uid = targetUserId ?: auth.currentUserOrNull()?.id ?: return@runCatching emptyList()
            val posts = postgrest["posts"]
                .select {
                    filter {
                        eq("user_id", uid)
                    }
                }
                .decodeList<PostDto>()

            val profile = getProfile(uid).getOrNull()
            val authorName = profile?.name ?: profile?.username ?: "Student"

            posts.map { post ->
                post.toUiModel(authorName = authorName)
            }
        }
    }
}
