package com.MADproject.quoraforuniversities.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {

    private val auth = SupabaseClient.client.auth
    private val postgrest = SupabaseClient.client.postgrest

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
            val user = auth.currentUserOrNull()
            if (user != null) {
                val profile = ProfileDto(
                    id = user.id,
                    username = usernameVal.ifBlank { emailVal.substringBefore("@") },
                    name = nameVal.ifBlank { "Student" }
                )
                runCatching {
                    postgrest["profiles"].insert(profile)
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
