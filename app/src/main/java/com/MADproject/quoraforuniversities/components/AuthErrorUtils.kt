package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error Icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

fun formatAuthErrorMessage(throwable: Throwable?, fallback: String): String {
    val rawMsg = throwable?.message.orEmpty()
    val lower = rawMsg.lowercase()

    return when {
        lower.contains("invalid login credentials") ||
                lower.contains("invalid_grant") ||
                lower.contains("invalid credentials") -> "Incorrect email or password. Please check your credentials and try again."

        lower.contains("user already registered") ||
                lower.contains("already_registered") ||
                lower.contains("user_already_exists") -> "An account with this email already exists. Try logging in instead."

        lower.contains("email not confirmed") -> "Your email address is not verified yet. Please check your inbox."

        lower.contains("unable to resolve host") ||
                lower.contains("failed to connect") ||
                lower.contains("connectexception") ||
                lower.contains("unknownhostexception") ||
                lower.contains("sockettimeoutexception") -> "Unable to connect to the database. Please check your internet connection."

        lower.contains("rate limit") || lower.contains("too many requests") -> "Too many failed attempts. Please wait a minute before trying again."

        lower.contains("password should be") -> "Password must be at least 6 characters long."

        rawMsg.isNotBlank() && !rawMsg.startsWith("{") && !rawMsg.contains("HttpResponse") -> rawMsg.replaceFirstChar { it.uppercase() }

        else -> fallback
    }
}
