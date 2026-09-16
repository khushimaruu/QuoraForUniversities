package com.MADproject.quoraforuniversities

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MADproject.quoraforuniversities.components.AppHeader
import com.MADproject.quoraforuniversities.components.BottomNavBar
import com.MADproject.quoraforuniversities.components.BottomNavDestination

data class UserPost(
    val title: String,
    val description: String,
    val category: String,
    val answers: Int,
    val likes: Int
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onNavDestinationSelected: (BottomNavDestination) -> Unit
) {

    val context = LocalContext.current

    // User information
    var username by remember { mutableStateOf("khushi_maru") }
    var firstName by remember { mutableStateOf("Khushi") }
    var lastName by remember { mutableStateOf("Maru") }
    var university by remember {
        mutableStateOf("Mukesh Patel School of Technology")
    }
    var gender by remember { mutableStateOf("Female") }
    var bio by remember {
        mutableStateOf("Computer Science student • Tech enthusiast")
    }

    // Profile photo
    var profileBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            profileBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
        }
    }

    // Example posts
    val posts = remember {
        listOf(
            UserPost(
                title = "How do I add a backlog course to my timetable?",
                description = "I failed one course last semester and need to retake it alongside my current courses.",
                category = "Registration",
                answers = 4,
                likes = 12
            ),
            UserPost(
                title = "Best cafes near the north campus for group study?",
                description = "Looking for a place with decent wifi and enough seating for 4-5 people.",
                category = "Campus Life",
                answers = 9,
                likes = 27
            )
        )
    }

    Scaffold(
        topBar = {
            Box {
                AppHeader(subtitle = "My Profile")
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 44.dp, start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
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
                .background(Color(0xFFF8F8F8))
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {

                Spacer(modifier = Modifier.height(20.dp))

                // ---------------- PROFILE PHOTO ----------------

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

                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8E8E8)),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(65.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    // Camera button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                            .clickable {
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
                    text = "Change profile photo",
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.clickable {
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
                            color = Color.Gray
                        )

                        Text(
                            text = "@$username",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            username = if (username == "khushi_maru") {
                                "student_user"
                            } else {
                                "khushi_maru"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Username"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                // ---------------- PERSONAL INFORMATION ----------------

                ProfileInfoCard(
                    title = "Personal Information"
                ) {

                    ProfileField(
                        label = "First Name",
                        value = firstName
                    )

                    ProfileField(
                        label = "Last Name",
                        value = lastName
                    )

                    ProfileField(
                        label = "University",
                        value = university
                    )

                    ProfileField(
                        label = "Gender",
                        value = gender
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // ---------------- BIO ----------------

                ProfileInfoCard(
                    title = "Bio"
                ) {

                    Text(
                        text = bio,
                        fontSize = 14.sp,
                        color = Color(0xFF444444),
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Edit Bio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555),
                        modifier = Modifier.clickable {
                            bio = if (
                                bio == "Computer Science student • Tech enthusiast"
                            ) {
                                "Computer Science student"
                            } else {
                                "Computer Science student • Tech enthusiast"
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                // ---------------- POSTS ----------------

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
                        text = "(${posts.size})",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // ---------------- POST CARDS ----------------

            items(posts) { post ->

                PostCard(post)

                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Spacer(modifier = Modifier.height(25.dp))
            }
        }
    }
}


// ============================================================
// PROFILE INFO CARD
// ============================================================

@Composable
fun ProfileInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
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


// ============================================================
// PROFILE FIELD
// ============================================================

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
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 15.sp,
            color = Color(0xFF222222)
        )
    }
}


// ============================================================
// POST CARD
// ============================================================

@Composable
fun PostCard(post: UserPost) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {

            Text(
                text = post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = post.description,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = Color(0xFFF0F0F0)
            ) {

                Text(
                    text = post.category,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "♥ ${post.likes}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(15.dp))

                Text(
                    text = "${post.answers} answers",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}