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
fun LoginPage(authRepository: AuthRepository? = null, onBackClick: () -> Unit = {},
              onLoginSuccess: () -> Unit = {}, onNavigateToSignUp: () -> Unit = {}) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun submit() {
        if (busy) return
        val address = email.trim()
        val secret = password.trim()
        if (address.isBlank() || secret.isBlank()) error = "Please enter both email and password."
        else if (!address.contains("@") || !address.contains(".")) error = "Please enter a valid university email address."
        else {
            busy = true; error = null
            scope.launch {
                try {
                    (authRepository ?: AuthRepository()).signIn(address, secret).onSuccess { onLoginSuccess() }
                        .onFailure { error = formatAuthErrorMessage(it, "Login failed. Please check your credentials and try again.") }
                } finally { busy = false }
            }
        }
    }
    CampusAuthPage(false, listOf(
        CampusAuthField("email", "Campus email", "you@nmims.in", KeyboardType.Email),
        CampusAuthField("password", "Password", "Your password", KeyboardType.Password, true)),
        mapOf("email" to email, "password" to password),
        { key, value -> if (key == "email") email = value else password = value; error = null },
        busy, error, ::submit, onNavigateToSignUp, onBackClick)
}

@Preview(name = "Login · compact", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Login · large text", widthDp = 430, heightDp = 932, fontScale = 1.5f, showBackground = true)
@Composable
fun LoginPagePreview() { CampusQnATheme { LoginPage() } }
