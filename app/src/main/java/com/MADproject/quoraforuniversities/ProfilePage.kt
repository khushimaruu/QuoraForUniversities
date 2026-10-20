package com.MADproject.quoraforuniversities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    LaunchedEffect(Unit) {
        val uid = authRepository.getCurrentUserId()
        val email = authRepository.getCurrentUserEmail() ?: ""
        emailText = email
        
        // Fetch Profile from Supabase
        val fetchedResult = profileRepository.getProfile(uid)
        val fetched = fetchedResult.getOrNull()
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

    // Initial letter for the avatar
    val initialLetter = (if (name.isNotBlank()) name else username).firstOrNull()?.uppercase() ?: "U"

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
                        Log.e("ProfileScreen", "Failed to upload photo: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(subtitle = "My Profile")
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = BottomNavDestination.PROFILE.route,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                Spacer(modifier = Modifier.height(20.dp))

                // ---------------- PROFILE PHOTO / DEFAULT INITIAL AVATAR ----------------
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap!!.asImageBitmap(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Default initial letter avatar
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialLetter,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (isUploadingPhoto) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    // Camera button to change photo
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                            .clickable(enabled = !isUploadingPhoto) {
                                imagePicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isUploadingPhoto) "Saving photo to cloud..." else "Change profile photo",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = !isUploadingPhoto) {
                        imagePicker.launch("image/*")
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ---------------- USERNAME ----------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Username",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "@$username",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                // ---------------- PERSONAL INFORMATION ----------------
                ProfileInfoCard(
                    title = "Personal Information"
                ) {
                    ProfileField(
                        label = "Name",
                        value = name
                    )
                    if (emailText.isNotBlank()) {
                        ProfileField(
                            label = "Email",
                            value = emailText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                // ---------------- BIO CARD WITH EDIT BUTTON ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    editedBioText = profile?.bio.orEmpty()
                                    showEditBioDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Bio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bio,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                // ---------------- SAVED POSTS SHORTCUT ----------------
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSavedPostsClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Saved Posts",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Saved Posts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "$savedCount saved",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ---------------- LOG OUT BUTTON ----------------
                Button(
                    onClick = {
                        scope.launch {
                            authRepository.signOut()
                            onLogOutClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(25.dp))

                // ---------------- POSTS HEADER ----------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Posts Created",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${userPosts.size})",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                if (isLoadingPosts) {
                    CircularProgressIndicator(modifier = Modifier.padding(20.dp))
                } else if (userPosts.isEmpty()) {
                    Text(
                        text = "You haven't posted anything yet.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------------- USER POST CARDS ----------------
            items(userPosts, key = { it.id }) { post ->
                QuestionCard(
                    question = post,
                    onClick = onQuestionClick,
                    onUpvoteClick = onUpvoteClick,
                    onDownvoteClick = onDownvoteClick,
                    onBookmarkClick = onBookmarkClick,
                    onReportClick = onReportClick,
                    isBookmarked = savedPostIds.contains(post.id)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Spacer(modifier = Modifier.height(25.dp))
            }
        }
    }

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

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CampusQnATheme {
        ProfileScreen()
    }
}

@Composable
fun ProfileInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
