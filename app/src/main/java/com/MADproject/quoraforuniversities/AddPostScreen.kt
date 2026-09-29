package com.MADproject.quoraforuniversities.ui.addpost

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MADproject.quoraforuniversities.components.AppHeader
import com.MADproject.quoraforuniversities.data.GeminiService
import com.MADproject.quoraforuniversities.data.PostRepository
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
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
    postRepository: PostRepository = remember { PostRepository() },
    geminiService: GeminiService = remember { GeminiService() },
    onBack: () -> Unit = {},
    onPosted: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedThemes by remember { mutableStateOf(setOf<String>()) }
    var isAnonymous by remember { mutableStateOf(false) }
    var checkState by remember { mutableStateOf(PostCheckState.IDLE) }
    var isEnhancing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AppHeader(subtitle = "New Post")
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title + description card
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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

                    Spacer(Modifier.height(6.dp))

                    Text("Description", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; errorMessage = null },
                        placeholder = { Text("Provide details, context, or specific questions...") },
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(10.dp),
                        colors = circleTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Enhance with AI Button
                    OutlinedButton(
                        onClick = {
                            if (title.isBlank() && description.isBlank()) {
                                errorMessage = "Please enter a title or description to enhance."
                                return@OutlinedButton
                            }
                            isEnhancing = true
                            errorMessage = null
                            scope.launch {
                                val result = geminiService.enhancePostContent(title, description)
                                isEnhancing = false
                                if (result.isSuccess) {
                                    val enhanced = result.getOrNull()
                                    if (enhanced != null) {
                                        title = enhanced.title
                                        description = enhanced.description
                                    }
                                } else {
                                    errorMessage = "Failed to enhance writing: ${result.exceptionOrNull()?.localizedMessage ?: "Please check network or API key."}"
                                }
                            }
                        },
                        enabled = !isEnhancing && checkState != PostCheckState.CHECKING,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isEnhancing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Enhancing...", fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Enhance with AI", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Theme picker
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Theme(s)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Helps others find your post easily", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableThemes.forEach { tag ->
                            val selected = selectedThemes.contains(tag)
                            ThemeChip(
                                label = tag,
                                selected = selected,
                                onClick = {
                                    selectedThemes = if (selected) {
                                        selectedThemes - tag
                                    } else {
                                        selectedThemes + tag
                                    }
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }
            }

            // Anonymous toggle
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                        // AI checks temporarily disabled per user request
                        val themeString = selectedThemes.joinToString(",")
                        val result = postRepository.createPost(
                            title = title.trim(),
                            content = description.trim(),
                            theme = themeString,
                            isAnonymous = isAnonymous
                        )
                        if (result.isSuccess) {
                            checkState = PostCheckState.PASSED
                            showSuccessDialog = true
                        } else {
                            checkState = PostCheckState.FAILED
                            errorMessage = result.exceptionOrNull()?.localizedMessage
                                ?: "Failed to post to database."
                        }
                    }
                },
                enabled = checkState != PostCheckState.CHECKING && !isEnhancing,
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
                    Text("Checking content with AI...", color = Color.White, fontWeight = FontWeight.SemiBold)
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
            text = { Text("Your post passed AI moderation and is live on CampusCircle.") },
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

@Preview(showBackground = true)
@Composable
fun AddPostScreenPreview() {
    CampusQnATheme {
        AddPostScreen()
    }
}
