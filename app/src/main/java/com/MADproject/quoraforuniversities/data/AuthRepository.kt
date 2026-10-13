package com.MADproject.quoraforuniversities.data

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class AuthRepository(context: Context? = null) {

    private val auth = SupabaseClient.client.auth
    private val prefs = context?.applicationContext?.getSharedPreferences("campus_auth_prefs", Context.MODE_PRIVATE)

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
            val userId = user?.id
            val userEmail = user?.email.orEmpty().ifBlank { emailVal }

            if (userId != null) {
                val profile = ProfileDto(
                    id = userId,
                    username = usernameVal,
                    name = nameVal
                )
                runCatching {
                    SupabaseClient.client.from("profiles").upsert(profile)
                }
                saveSession(userId, userEmail)
            } else {
                saveSession("local_user", emailVal)
            }
        }
    }

    suspend fun signIn(emailVal: String, passwordVal: String): Result<Unit> {
        return runCatching {
            auth.signInWith(Email) {
                email = emailVal
                password = passwordVal
            }
            val user = auth.currentUserOrNull()
            val userId = user?.id
            val userEmail = user?.email.orEmpty().ifBlank { emailVal }

            if (userId != null) {
                val username = userEmail.substringBefore("@").ifBlank { "student" }
                val profile = ProfileDto(
                    id = userId,
                    username = username,
                    name = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                )
                runCatching {
                    SupabaseClient.client.from("profiles").upsert(profile)
                }
                saveSession(userId, userEmail)
            } else {
                saveSession("local_user", emailVal)
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return runCatching {
            runCatching { auth.signOut() }
            clearSession()
        }
    }

    private fun saveSession(userId: String, email: String) {
        prefs?.edit()
            ?.putBoolean("is_logged_in", true)
            ?.putString("user_id", userId)
            ?.putString("user_email", email)
            ?.apply()
    }

    private fun clearSession() {
        prefs?.edit()?.clear()?.apply()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id ?: prefs?.getString("user_id", null)
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUserOrNull()?.email ?: prefs?.getString("user_email", null)
    }

    fun isLoggedIn(): Boolean {
        if (auth.currentSessionOrNull() != null) return true
        return prefs?.getBoolean("is_logged_in", false) == true
    }
}
