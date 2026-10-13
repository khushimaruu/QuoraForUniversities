package com.MADproject.quoraforuniversities.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class AuthRepository {

    private val auth = SupabaseClient.client.auth

    suspend fun signUp(
        emailVal: String,
        passwordVal: String,
        nameVal: String,
        usernameVal: String
    ): Result<Unit> {
        return runCatching {
            auth.signUpWith(Email) {
                email = emailVal
                password = passwordVal
            }
            val userId = auth.currentUserOrNull()?.id
            if (userId != null) {
                val profile = ProfileDto(
                    id = userId,
                    username = usernameVal,
                    name = nameVal
                )
                runCatching {
                    SupabaseClient.client.from("profiles").upsert(profile)
                }
            }
        }
    }

    suspend fun signIn(emailVal: String, passwordVal: String): Result<Unit> {
        return runCatching {
            auth.signInWith(Email) {
                email = emailVal
                password = passwordVal
            }
            // Ensure profile exists on sign in as well
            val user = auth.currentUserOrNull()
            val userId = user?.id
            if (userId != null) {
                val email = user.email.orEmpty()
                val username = email.substringBefore("@").ifBlank { "student" }
                val profile = ProfileDto(
                    id = userId,
                    username = username,
                    name = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                )
                runCatching {
                    SupabaseClient.client.from("profiles").upsert(profile)
                }
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUserOrNull()?.email
    }

    fun isLoggedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }
}
