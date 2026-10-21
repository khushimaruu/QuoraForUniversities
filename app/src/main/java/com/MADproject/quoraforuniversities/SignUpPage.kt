package com.MADproject.quoraforuniversities

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.MADproject.quoraforuniversities.components.*
import com.MADproject.quoraforuniversities.data.AuthRepository
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import kotlinx.coroutines.launch

@Composable
fun SignUpPage(authRepository: AuthRepository? = null, onBackClick: () -> Unit = {},
               onSignUpSuccess: () -> Unit = {}, onNavigateToLogin: () -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }
    var university by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun submit() {
        if (busy) return
        val address = email.trim().lowercase()
        val secret = password.trim()
        error = when {
            name.isBlank() || university.isBlank() || address.isBlank() || secret.isBlank() -> "Please fill in all required fields."
            !address.endsWith("@nmims.in") || address.length <= "@nmims.in".length || address.startsWith("@") ->
                "Only NMIMS email addresses ending with @nmims.in are allowed."
            secret.length < 6 -> "Password must be at least 6 characters long."
            secret != confirmation.trim() -> "Passwords do not match."
            else -> null
        }
        if (error != null) return
        busy = true
        scope.launch {
            try {
                (authRepository ?: AuthRepository()).signUp(emailVal = address, passwordVal = secret,
                    fullNameVal = name.trim(), collegeVal = university.trim(), bioVal = null)
                    .onSuccess { onSignUpSuccess() }
                    .onFailure { error = formatAuthErrorMessage(it, "Sign up failed. Please check your details and try again.") }
            } finally { busy = false }
        }
    }
    CampusAuthPage(true, listOf(
        CampusAuthField("name", "Full name", "Your full name"),
        CampusAuthField("university", "Campus institution", "Your university / college"),
        CampusAuthField("email", "Campus email", "you@nmims.in", KeyboardType.Email),
        CampusAuthField("password", "Password", "At least 6 characters", KeyboardType.Password, true),
        CampusAuthField("confirmation", "Confirm password", "Your password, once more", KeyboardType.Password, true)),
        mapOf("name" to name, "university" to university, "email" to email, "password" to password, "confirmation" to confirmation),
        { key, value -> when (key) { "name" -> name = value; "university" -> university = value
            "email" -> email = value; "password" -> password = value; "confirmation" -> confirmation = value }; error = null },
        busy, error, ::submit, onNavigateToLogin, onBackClick)
}

@Preview(name = "Sign up · compact", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Sign up · large text", widthDp = 430, heightDp = 932, fontScale = 1.5f, showBackground = true)
@Composable
fun SignUpPagePreview() { CampusQnATheme { SignUpPage() } }
