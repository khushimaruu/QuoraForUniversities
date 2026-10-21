package com.MADproject.quoraforuniversities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import com.MADproject.quoraforuniversities.data.ProfileAnswerUiModel
import com.MADproject.quoraforuniversities.utils.TimestampFormatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import com.MADproject.quoraforuniversities.components.readingWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MADproject.quoraforuniversities.components.ErrorBanner
import com.MADproject.quoraforuniversities.components.SectionCard
import com.MADproject.quoraforuniversities.components.ScreenMessage
import com.MADproject.quoraforuniversities.components.AppHeader
import com.MADproject.quoraforuniversities.components.BottomNavBar
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionCard
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.data.AuthRepository
import com.MADproject.quoraforuniversities.data.ProfileDto
import com.MADproject.quoraforuniversities.data.ProfileRepository
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() },
    savedCount: Int = 0,
    savedPostIds: Set<String> = emptySet(),
    userPosts: List<QuestionUiModel> = emptyList(),
    isLoadingPosts: Boolean = false,
    savedQuestions: List<QuestionUiModel> = emptyList(),
    userAnswers: List<ProfileAnswerUiModel> = emptyList(),
    isLoadingAnswers: Boolean = false,
    onQuestionClick: (String) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {},
    onDownvoteClick: (String) -> Unit = {},
    onBookmarkClick: (String) -> Unit = {},
    onReportClick: (String, String) -> Unit = { _, _ -> },
    onSavedPostsClick: () -> Unit = {},
    onLogOutClick: () -> Unit = {},
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var emailText by remember { mutableStateOf("") }

    // Profile photo state
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Edit Bio Dialog State
    var showEditBioDialog by remember { mutableStateOf(false) }
    var editedBioText by remember { mutableStateOf("") }
    var isSavingBio by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = authRepository.getCurrentUserId()
        val email = authRepository.getCurrentUserEmail() ?: ""
        emailText = email
        
        // Fetch Profile from Supabase
        val fetchedResult = profileRepository.getProfile(uid)
        val fetched = fetchedResult.getOrNull()
        if (fetchedResult.isFailure) profileError = "Could not load your profile. Check your connection and reopen this page."
        if (fetched != null) {
            profile = fetched
            fetched.avatarUrl?.let { avatarStr ->
                if (avatarStr.isNotBlank()) {
                    val decodedBitmap = base64ToBitmap(avatarStr)
                    if (decodedBitmap != null) {
                        profileBitmap = decodedBitmap
                    }
                }
            }
        } else {
            profile = ProfileDto(
                id = uid ?: "",
                username = email.substringBefore("@").ifBlank { "student" },
                name = "Student"
            )
        }
    }

    val username = profile?.username ?: emailText.substringBefore("@").ifBlank { "student" }
    val name = profile?.name ?: "Student"
    val bio = profile?.bio?.takeIf { it.isNotBlank() } ?: "No bio added yet. Click edit to add one!"

    // Profile photo picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            val inputStream = context.contentResolver.openInputStream(pickedUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                profileBitmap = originalBitmap
                isUploadingPhoto = true
                scope.launch {
                    val base64Photo = bitmapToBase64(originalBitmap)
                    val uid = authRepository.getCurrentUserId() ?: ""
                    val currentProfile = profile ?: ProfileDto(
                        id = uid,
                        username = username,
                        name = name
                    )
                    val updatedProfile = currentProfile.copy(avatarUrl = base64Photo)
                    val res = profileRepository.updateProfile(updatedProfile)
                    isUploadingPhoto = false
                    if (res.isSuccess) {
                        profile = updatedProfile
                    } else {
                        profileError = "Could not save your profile photo. Please try again."
                    }
                }
            }
        }
    }

    ProfileContent(
        profile = profile, email = emailText, name = name, username = username, bio = bio,
        profileBitmap = profileBitmap, uploading = isUploadingPhoto, error = profileError,
        savedCount = savedCount, savedPostIds = savedPostIds, userPosts = userPosts, isLoadingPosts = isLoadingPosts,
        savedQuestions = savedQuestions, userAnswers = userAnswers, isLoadingAnswers = isLoadingAnswers,
        onPhotoClick = { imagePicker.launch("image/*") },
        onBioClick = { editedBioText = profile?.bio.orEmpty(); showEditBioDialog = true },
        onSavedPostsClick = onSavedPostsClick, onQuestionClick = onQuestionClick,
        onUpvoteClick = onUpvoteClick, onDownvoteClick = onDownvoteClick,
        onBookmarkClick = onBookmarkClick, onReportClick = onReportClick,
        onLogOutClick = { scope.launch {
            authRepository.signOut().onSuccess { onLogOutClick() }
                .onFailure { profileError = "Could not log out. Please try again." }
        } }, onNavDestinationSelected = onNavDestinationSelected
    )

    // ---------------- EDIT BIO DIALOG ----------------
    if (showEditBioDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingBio) showEditBioDialog = false },
            title = { Text("Edit Bio", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Tell fellow students a bit about your major, interests, or year.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedBioText,
                        onValueChange = { editedBioText = it },
                        placeholder = { Text("e.g. 2nd Year CSE @ MPSTME • Tech enthusiast") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = profile ?: return@Button
                        isSavingBio = true
                        scope.launch {
                            val updated = current.copy(bio = editedBioText.trim())
                            val result = profileRepository.updateProfile(updated)
                            isSavingBio = false
                            if (result.isSuccess) {
                                profile = updated
                                showEditBioDialog = false
                            } else {
                                profileError = "Could not save your bio. Please try again."
                            }
                        }
                    },
                    enabled = !isSavingBio
                ) {
                    if (isSavingBio) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditBioDialog = false },
                    enabled = !isSavingBio
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileContent(
    profile: ProfileDto?, email: String, name: String, username: String, bio: String,
    profileBitmap: Bitmap?, uploading: Boolean, error: String?, savedCount: Int,
    savedPostIds: Set<String>, userPosts: List<QuestionUiModel>, isLoadingPosts: Boolean,
    onPhotoClick: () -> Unit, onBioClick: () -> Unit, onSavedPostsClick: () -> Unit,
    onQuestionClick: (String) -> Unit, onUpvoteClick: (String) -> Unit, onDownvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit, onReportClick: (String, String) -> Unit,
    onLogOutClick: () -> Unit, onNavDestinationSelected: (BottomNavDestination) -> Unit,
    savedQuestions: List<QuestionUiModel> = emptyList(), userAnswers: List<ProfileAnswerUiModel> = emptyList(),
    isLoadingAnswers: Boolean = false
) {
    var selectedTab by rememberSaveable { mutableStateOf("Saved Questions") }
    var topic by rememberSaveable(selectedTab) { mutableStateOf("All") }
    val posts = if (selectedTab == "Saved Questions") savedQuestions else userPosts
    val activeTopics = if (selectedTab == "Answers") userAnswers.flatMap { it.question.tags } else posts.flatMap { it.tags }
    val categories = activeTopics.distinct()
    val visiblePosts = if (topic == "All") posts else posts.filter { topic in it.tags }
    val visibleAnswers = if (topic == "All") userAnswers else userAnswers.filter { topic in it.question.tags }
    val listState = rememberLazyListState()
    Scaffold(topBar = { AppHeader() }, bottomBar = { BottomNavBar(BottomNavDestination.PROFILE.route, onNavDestinationSelected) }) { padding ->
        LazyColumn(Modifier.padding(padding).consumeWindowInsets(padding).readingWidth(), state = listState,
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = !uploading, onClick = onPhotoClick), Alignment.Center) {
                        if (profileBitmap != null) Image(profileBitmap.asImageBitmap(), "Change profile photo",
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(name.firstOrNull()?.uppercase() ?: "S", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("@" + username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = onBioClick, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onPhotoClick, enabled = !uploading) { Text(if (uploading) "Saving photo…" else "Change photo", style = MaterialTheme.typography.labelSmall) }
                if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            if (error != null) item { ErrorBanner(error) }
            item {
                SecondaryTabRow(selectedTabIndex = listOf("Saved Questions", "My Questions", "Answers").indexOf(selectedTab),
                    containerColor = MaterialTheme.colorScheme.background) {
                    listOf("Saved Questions" to savedCount, "My Questions" to userPosts.size, "Answers" to userAnswers.size).forEach { (label, count) ->
                        Tab(selected = selectedTab == label, onClick = {
                            selectedTab = label
                            if (label == "Saved Questions") onSavedPostsClick()
                        }, text = { Text(label + " " + count, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = topic == "All", onClick = { topic = "All" }, label = {
                    Text("All (" + (if (selectedTab == "Answers") userAnswers.size else posts.size) + ")", style = MaterialTheme.typography.labelSmall)
                })
                categories.forEach { category -> FilterChip(selected = topic == category, onClick = { topic = category },
                    label = { Text(category, style = MaterialTheme.typography.labelSmall) }) }
            } }
            if ((selectedTab == "My Questions" && isLoadingPosts) || (selectedTab == "Answers" && isLoadingAnswers))
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (selectedTab == "Answers") {
                if (!isLoadingAnswers && visibleAnswers.isEmpty()) item { ScreenMessage("No answers yet", "Answers you share will appear here.") }
                items(visibleAnswers, key = { it.answer.id }) { entry -> SectionCard {
                    Text(entry.question.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    TimestampFormatter.format(entry.answer.createdAt)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(entry.answer.body, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onQuestionClick(entry.question.id) }) { Text("View conversation →") }
                } }
            } else {
                if (visiblePosts.isEmpty() && !isLoadingPosts) item { ScreenMessage(
                    if (selectedTab == "Saved Questions") "No saved questions yet" else "No questions yet",
                    if (selectedTab == "Saved Questions") "Bookmark a question and it will appear in this tab." else "Questions you post will appear here.") }
                items(visiblePosts, key = { it.id }) { post -> QuestionCard(post, onQuestionClick, onUpvoteClick,
                    onDownvoteClick = onDownvoteClick, onBookmarkClick = onBookmarkClick, onReportClick = onReportClick,
                    isBookmarked = post.id in savedPostIds) }
            }
            item { OutlinedButton(onClick = onLogOutClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Log out") } }
        }
    }
}

// ---------------- HELPER FUNCTIONS FOR BITMAP <-> BASE64 ----------------
private fun bitmapToBase64(bitmap: Bitmap): String {
    val maxDimension = 300
    val width = bitmap.width
    val height = bitmap.height
    val scaledBitmap = if (width > maxDimension || height > maxDimension) {
        val ratio = width.toFloat() / height.toFloat()
        val (targetW, targetH) = if (ratio > 1f) {
            maxDimension to (maxDimension / ratio).toInt()
        } else {
            (maxDimension * ratio).toInt() to maxDimension
        }
        Bitmap.createScaledBitmap(bitmap, targetW.coerceAtLeast(1), targetH.coerceAtLeast(1), true)
    } else {
        bitmap
    }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    val bytes = outputStream.toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

private fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val cleanBase64 = base64Str.substringAfter(",")
        val bytes = Base64.decode(cleanBase64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

@Preview(name = "Profile · compact", widthDp = 320, heightDp = 700, showBackground = true)
@Preview(name = "Profile · large text", widthDp = 430, heightDp = 932, fontScale = 1.5f, showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CampusQnATheme {
        ProfileContent(null, "student@university.edu", "Campus student", "student", "Learning something new every day.",
            null, false, null, 2, emptySet(), sampleQuestions, false,
            {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, savedQuestions = sampleQuestions)
    }
}

@Composable
fun ProfileInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
