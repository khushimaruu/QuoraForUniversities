package com.MADproject.quoraforuniversities.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class AuthRepository {

    private val auth = SupabaseClient.client.auth

    suspend fun signUp(
        emailVal: String,
        passwordVal: String,
        fullNameVal: String,
        collegeVal: String,
        bioVal: String? = null
    ): Result<Unit> {
        return runCatching {
            // 1. Authenticate with Supabase Auth
            auth.signUpWith(Email) {
                email = emailVal
                password = passwordVal
            }

            val userId = auth.currentUserOrNull()?.id
                ?: error("Account created, but could not retrieve user ID.")

            // 2. Parse First Name and Last Name
            val nameParts = fullNameVal.trim().split(" ", limit = 2)
            val firstName = nameParts.getOrNull(0) ?: fullNameVal
            val lastName = nameParts.getOrNull(1)

            // Default bio includes the college if user left bio blank
            val finalBio = if (!bioVal.isNullOrBlank()) bioVal.trim() else "Student at ${collegeVal.trim()}"
            val usernameVal = emailVal.substringBefore("@").replace(".", "_")

            val profile = ProfileDto(
                id = userId,
                username = usernameVal,
                name = firstName,
                lastName = lastName,
                bio = finalBio
            )

            // 3. Upsert into public.profiles table
            runCatching {
                SupabaseClient.client.from("profiles").upsert(profile)
            }
        }
    }

    suspend fun signIn(emailVal: String, passwordVal: String): Result<Unit> {
        return runCatching {
            auth.signInWith(Email) {
                email = emailVal
                password = passwordVal
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
        }
    }

    fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

    fun getCurrentUserEmail(): String? = auth.currentUserOrNull()?.email

    fun isLoggedIn(): Boolean = auth.currentSessionOrNull() != null
}