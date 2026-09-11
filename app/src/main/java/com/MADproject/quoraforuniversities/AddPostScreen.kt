package com.MADproject.quoraforuniversities.ui.addpost

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import com.MADproject.quoraforuniversities.ui.theme.HeaderGradientEnd
import com.MADproject.quoraforuniversities.ui.theme.HeaderGradientMid
import com.MADproject.quoraforuniversities.ui.theme.HeaderGradientStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Reuse the same tag vocabulary visible on the home feed
val availableThemes = listOf(
    "CSE", "Year2", "Registration", "CampusLife",
    "Academics", "Events", "Hostel", "General"
)

enum class PostCheckState { IDLE, CHECKING, PASSED, FAILED }

// ---------- Screen ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPostScreen(
    onBack: () -> Unit = {},
    onPosted: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedThemes by remember { mutableStateOf(setOf<String>()) }
    var isAnonymous by remember { mutableStateOf(false) }
    var checkState by remember { mutableStateOf(PostCheckState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(HeaderGradientStart, HeaderGradientMid, HeaderGradientEnd)
                        )
                    )
                    .padding(top = 20.dp, bottom = 20.dp, start = 4.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "New Post",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title + description
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Title", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; errorMessage = null },
                        placeholder = { Text("e.g. How do I add a backlog course?") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = circleTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Text("Description", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; errorMessage = null },
                        placeholder = { Text("Add details to help others answer...") },
                        shape = RoundedCornerShape(10.dp),
                        colors = circleTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }

            // Theme picker
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Choose theme", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Select one or more tags",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableThemes.forEach { theme ->
                            val selected = theme in selectedThemes
                            ThemeChip(
                                label = theme,
                                selected = selected,
                                onClick = {
                                    selectedThemes = if (selected) selectedThemes - theme else selectedThemes + theme
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }
            }

            // Anonymous toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Post anonymously", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (isAnonymous) "Your name will be hidden" else "Your name will be visible",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            errorMessage?.let { msg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            Button(
                onClick = {
                    val validationError = validatePost(title, description, selectedThemes)
                    if (validationError != null) {
                        errorMessage = validationError
                        return@Button
                    }
                    errorMessage = null
                    checkState = PostCheckState.CHECKING
                    scope.launch {
                        val passed = runDummyQualityCheck(title, description)
                        if (passed) {
                            checkState = PostCheckState.PASSED
                            showSuccessDialog = true
                        } else {
                            checkState = PostCheckState.FAILED
                            errorMessage = "This post didn't pass our quality check. Add more detail or rephrase it."
                        }
                    }
                },
                enabled = checkState != PostCheckState.CHECKING,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (checkState == PostCheckState.CHECKING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Checking quality...", color = Color.White, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Post", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onPosted()
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            title = { Text("Posted!", fontWeight = FontWeight.Bold) },
            text = { Text("Your post is live on CampusCircle.") },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ---------- Reusable theme chip ----------
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun circleTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.secondary
)

// ---------- Validation ----------
private fun validatePost(title: String, description: String, themes: Set<String>): String? {
    return when {
        title.isBlank() -> "Please add a title for your post."
        title.trim().length < 6 -> "Title is too short — add a bit more detail."
        description.isBlank() -> "Please add a description."
        description.trim().length < 15 -> "Description is too short — give others enough context."
        themes.isEmpty() -> "Pick at least one theme for your post."
        else -> null
    }
}

// ---------- Dummy quality check ----------
// Placeholder for the real check. Later, replace the body of this function with
// a call to the Gemini API (send title + description, parse a pass/fail + reason).
private val bannedWords = listOf("idiot", "stupid", "hate", "dumb")

private suspend fun runDummyQualityCheck(title: String, description: String): Boolean {
    // Simulate network/API latency
    delay(1400)

    val combined = (title + " " + description).lowercase()
    val containsBannedWord = bannedWords.any { combined.contains(it) }
    val isLongEnough = description.trim().length >= 15
    val isNotAllCaps = title != title.uppercase() || title.length < 8

    // TODO: swap this block for a real Gemini API call, e.g.:
    // val result = geminiClient.checkPostQuality(title, description)
    // return result.passed

    return !containsBannedWord && isLongEnough && isNotAllCaps
}

// ---------- Preview ----------
@Preview(showBackground = true)
@Composable
private fun AddPostScreenPreview() {
    CampusQnATheme {
        AddPostScreen()
    }
}