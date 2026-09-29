package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.components.QuestionUiModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

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
            val uid = targetUserId ?: SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@runCatching emptyList()
            val posts = SupabaseClient.client.from("posts")
                .select {
                    filter {
                        eq("user_id", uid)
                    }
                }
                .decodeList<PostDto>()
            posts.map { it.toUiModel() }
        }.recover {
            emptyList()
        }
    }
}
