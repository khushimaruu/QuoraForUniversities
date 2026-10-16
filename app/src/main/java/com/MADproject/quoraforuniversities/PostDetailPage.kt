package com.MADproject.quoraforuniversities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MADproject.quoraforuniversities.components.AppHeader
import com.MADproject.quoraforuniversities.components.BottomNavBar
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.components.ReportPostDialog
import com.MADproject.quoraforuniversities.data.GeminiService
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import kotlinx.coroutines.launch

data class ReplyUiModel(
    val id: String,
    val authorName: String,
    val body: String,
    val voteCount: Int
)

@Composable
fun PostDetailPage(
    question: QuestionUiModel,
    replies: List<ReplyUiModel>,
    geminiService: GeminiService = remember { GeminiService() },
    isBookmarked: Boolean = false,
    onBackClick: () -> Unit = {},
    onUpvoteClick: () -> Unit = {},
    onDownvoteClick: () -> Unit = {},
    onBookmarkClick: ((String) -> Unit)? = null,
    onReportClick: ((postId: String, reason: String) -> Unit)? = null,
    onSubmitReply: (text: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> },
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {
    var replyText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var replyError by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(subtitle = "Question Details", onBackClick = onBackClick)
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = null,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // FULL QUESTION CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Top Row: Author & 3-dots Menu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (question.isAnonymous) "Anonymous" else question.authorName,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Report Post", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.OutlinedFlag,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showReportDialog = true
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = question.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 29.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = question.body,
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (question.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    question.tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = tag,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            // SEPARATE UPVOTE / DOWNVOTE / ANSWERS / BOOKMARK ROW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // UPVOTE
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onUpvoteClick() }
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (question.currentUserVote == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                            contentDescription = "Upvote",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (question.currentUserVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = question.upvoteCount.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (question.currentUserVote == 1) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (question.currentUserVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // DOWNVOTE
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onDownvoteClick() }
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (question.currentUserVote == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                            contentDescription = "Downvote",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (question.currentUserVote == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = question.downvoteCount.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (question.currentUserVote == -1) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (question.currentUserVote == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "${replies.size} Answers",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (onBookmarkClick != null) {
                                    IconButton(onClick = { onBookmarkClick(question.id) }) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = if (isBookmarked) "Unsave" else "Save",
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ANSWERS HEADER
                item {
                    Text(
                        text = "${replies.size} Answers",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // REPLIES LIST
                items(replies, key = { it.id }) { reply ->
                    ReplyCard(reply = reply)
                }
            }

            // WRITE ANSWER BOX
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    replyError?.let { msg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = {
                                replyText = it
                                replyError = null
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Write an answer...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            enabled = replyText.isNotBlank() && !isSubmitting,
                            onClick = {
                                if (replyText.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    replyError = null
                                    scope.launch {
                                        val moderationResult = geminiService.checkReplyModeration(replyText.trim())
                                        if (moderationResult.isSuccess) {
                                            val mod = moderationResult.getOrNull()
                                            if (mod != null && !mod.isAllowed) {
                                                isSubmitting = false
                                                replyError = mod.reason ?: "Reply flagged by AI moderation."
                                                return@launch
                                            }
                                        } else {
                                            isSubmitting = false
                                            replyError = moderationResult.exceptionOrNull()?.localizedMessage
                                                ?: "AI content moderation check failed."
                                            return@launch
                                        }

                                        onSubmitReply(replyText.trim()) { error ->
                                            isSubmitting = false
                                            if (error != null) {
                                                replyError = error
                                            } else {
                                                replyText = ""
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false },
            onConfirmReport = { reason ->
                showReportDialog = false
                onReportClick?.invoke(question.id, reason)
            }
        )
    }
}

@Composable
fun ReplyCard(reply: ReplyUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Text(
                text = reply.authorName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reply.body,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = "Votes",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = reply.voteCount.toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val sampleReplies = listOf(
    ReplyUiModel(
        id = "1",
        authorName = "Riya Sharma",
        body = "You should be able to add the backlog course during the registration period. Check with your department coordinator if it doesn't appear.",
        voteCount = 8
    ),
    ReplyUiModel(
        id = "2",
        authorName = "Anonymous",
        body = "I had the same issue last semester. You need to select the backlog course separately before submitting your timetable.",
        voteCount = 5
    )
)

@Preview(showBackground = true)
@Composable
private fun PostDetailPagePreview() {
    val question = QuestionUiModel(
        id = "1",
        title = "How do I add a backlog course to my timetable?",
        body = "I failed one course last semester and need to retake it alongside my current courses.",
        authorName = "Anonymous",
        isAnonymous = true,
        tags = listOf("CSE", "Year2", "Registration"),
        voteCount = 12,
        answerCount = 2,
        upvoteCount = 12,
        downvoteCount = 0
    )

    CampusQnATheme {
        PostDetailPage(question = question, replies = sampleReplies)
    }
}
