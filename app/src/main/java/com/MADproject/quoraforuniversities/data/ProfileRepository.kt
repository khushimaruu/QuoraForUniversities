package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.components.QuestionUiModel

class ProfileRepository {

    suspend fun getProfile(targetUserId: String? = null): Result<ProfileDto?> {
        return Result.success(null)
    }

    suspend fun updateProfile(profile: ProfileDto): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun getUserPosts(targetUserId: String? = null): Result<List<QuestionUiModel>> {
        return Result.success(emptyList())
    }
}
