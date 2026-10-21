package com.MADproject.quoraforuniversities.ui.addpost

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.components.*
import com.MADproject.quoraforuniversities.data.GeminiService
import com.MADproject.quoraforuniversities.data.PostRepository
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import kotlinx.coroutines.launch

val availableThemes = listOf("CSE", "Year2", "Registration", "CampusLife", "Academics", "Events", "Hostel", "General")

@Composable
fun AddPostScreen(
    postRepository: PostRepository = remember { PostRepository() },
    geminiService: GeminiService = remember { GeminiService() },
    profileName: String = "My profile",
    onBack: () -> Unit = {}, onPosted: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var themes by rememberSaveable { mutableStateOf(listOf<String>()) }
    var anonymous by rememberSaveable { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var enhancing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    QuestionComposer(
        title, description, themes.toSet(), anonymous, posting, enhancing, error, profileName = profileName,
        onTitle = { title = it; error = null }, onDescription = { description = it; error = null },
        onTheme = { tag -> themes = if (tag in themes) themes - tag else themes + tag; error = null },
        onAnonymous = { anonymous = it }, onBack = onBack,
        onEnhance = {
            if (title.isBlank() && description.isBlank()) error = "Add a question or some context first."
            else {
                enhancing = true; error = null
                scope.launch {
                    try {
                        geminiService.enhancePostContent(title, description).onSuccess {
                            title = it.title; description = it.description
                        }.onFailure { error = "Couldn't enhance your writing. Check your connection and try again." }
                    } finally { enhancing = false }
                }
            }
        }, onPost = {
            error = validatePost(title, description, themes.toSet())
            if (error == null) {
                posting = true
                scope.launch {
                    try {
                        val moderation = geminiService.checkContentModeration(title.trim(), description.trim())
                        if (moderation.isFailure) {
                            error = "Couldn't check the content right now. Please try again."
                        } else if (moderation.getOrThrow().isAllowed) {
                            postRepository.createPost(title.trim(), description.trim(), themes.joinToString(","), anonymous)
                                .onSuccess { success = true }
                                .onFailure { error = "Couldn't post your question. Please try again." }
                        } else error = moderation.getOrThrow().reason ?: "Please revise your question to meet community guidelines."
                    } finally { posting = false }
                }
            }
        }
    )
    if (success) AlertDialog(onDismissRequest = { success = false; onPosted() },
        icon = { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Question posted") }, text = { Text("Your question is live in your campus community.") },
        confirmButton = { TextButton(onClick = { success = false; onPosted() }) { Text("Back to feed") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionComposer(
    title: String, description: String, themes: Set<String>, anonymous: Boolean,
    posting: Boolean, enhancing: Boolean, error: String?,
    onTitle: (String) -> Unit, onDescription: (String) -> Unit, onTheme: (String) -> Unit,
    onAnonymous: (Boolean) -> Unit, onBack: () -> Unit, onEnhance: () -> Unit, onPost: () -> Unit,
    profileName: String = "My profile"
) {
    val enabled = !posting && !enhancing
    Scaffold(topBar = { AppHeader(subtitle = "Ask a question", onBackClick = onBack) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).imePadding().readingWidth(600.dp)
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard {
                Text("QUESTION TITLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(title, onTitle, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What would you like to know?") }, minLines = 2,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                Text("Start with what, how, or why to get helpful answers.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard {
                Text("TAGS & TOPICS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableThemes.forEach { theme -> FilterChip(selected = theme in themes,
                        onClick = { onTheme(theme) }, enabled = enabled, label = { Text(theme) }) }
                }
                Text("Help the right students find your question.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard {
                Text("QUESTION DETAILS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(description, onDescription, enabled = enabled, modifier = Modifier.fillMaxWidth(), minLines = 5,
                    placeholder = { Text("Share the context, what you’ve tried, and what you need help with.") })
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Campus AI writing assistant", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Make your question clearer while keeping your original meaning.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onEnhance, enabled = enabled) {
                        if (enhancing) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                        Text(if (enhancing) "Enhancing…" else "Enhance with AI")
                    }
                }
            }
            SectionCard {
                Text("POSTING IDENTITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PostingIdentity(anonymous, profileName, enabled, onAnonymous)
            }
            if (error != null) ErrorBanner(error)
            Button(onClick = onPost, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (posting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp)); Text(if (posting) "Checking and posting…" else "Post question")
            }
            Text("Keep it helpful and respectful. Posts are checked against community guidelines.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun validatePost(title: String, description: String, themes: Set<String>): String? = when {
    title.isBlank() -> "Please add a title for your post."
    title.trim().length < 6 -> "Title is too short — add a bit more detail."
    description.isBlank() -> "Please add a description."
    description.trim().length < 15 -> "Description is too short — give others enough context."
    themes.isEmpty() -> "Pick at least one topic for your post."
    else -> null
}

@Preview(name = "Ask · compact", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Ask · large text", widthDp = 430, heightDp = 932, fontScale = 1.5f, showBackground = true)
@Composable
fun AddPostScreenPreview() { CampusQnATheme {
    QuestionComposer("How should I prepare for my next programming exam?", "", setOf("CSE"), true,
        false, false, null, {}, {}, {}, {}, {}, {}, {})
} }
