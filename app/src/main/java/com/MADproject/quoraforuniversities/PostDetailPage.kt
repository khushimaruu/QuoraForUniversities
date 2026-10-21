package com.MADproject.quoraforuniversities

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.MADproject.quoraforuniversities.utils.ReplyOrder
import com.MADproject.quoraforuniversities.utils.orderReplyThreads
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import com.MADproject.quoraforuniversities.components.readingWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.MADproject.quoraforuniversities.components.AppHeader
import com.MADproject.quoraforuniversities.components.AuthorByline
import com.MADproject.quoraforuniversities.components.AuthorAvatar
import com.MADproject.quoraforuniversities.components.BottomNavBar
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.components.ReportPostDialog
import com.MADproject.quoraforuniversities.data.GeminiService
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import com.MADproject.quoraforuniversities.utils.ReplyMentions
import kotlinx.coroutines.launch

data class ReplyUiModel(
    val id: String,
    val authorName: String,
    val body: String,
    val voteCount: Int,
    val createdAt: String? = null,
    val authorUsername: String? = null,
    val parentReplyId: String? = null,
    val upvoteCount: Int = voteCount.coerceAtLeast(0),
    val downvoteCount: Int = 0,
    val currentUserVote: Int = 0,
    val authorAvatar: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostDetailPage(
    question: QuestionUiModel,
    replies: List<ReplyUiModel>,
    geminiService: GeminiService = remember { GeminiService() },
    isBookmarked: Boolean = false,
    replyAuthorName: String = "Your profile",
    onBackClick: () -> Unit = {},
    onUpvoteClick: () -> Unit = {},
    onDownvoteClick: () -> Unit = {},
    onBookmarkClick: ((String) -> Unit)? = null,
    onReportClick: ((postId: String, reason: String) -> Unit)? = null,
    onSubmitReply: (text: String, parentReplyId: String?, onResult: (String?) -> Unit) -> Unit = { _, _, _ -> },
    onReplyVote: (replyId: String, voteType: Int, onResult: (String?) -> Unit) -> Unit = { _, _, onResult -> onResult(null) },
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {
    var replyText by rememberSaveable(question.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val replyFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var replyOrder by rememberSaveable(question.id) { mutableStateOf(ReplyOrder.TOP) }
    val replyRows = remember(replies, replyOrder) { orderReplyThreads(replies, replyOrder) }
    var selectedParentId by rememberSaveable(question.id) { mutableStateOf<String?>(null) }
    var selectedParentUsername by rememberSaveable(question.id) { mutableStateOf<String?>(null) }
    var pendingVotes by remember(question.id) { mutableStateOf(emptySet<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var replyError by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
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
                .consumeWindowInsets(innerPadding)
                .readingWidth()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FULL QUESTION CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Top Row: Author & 3-dots Menu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AuthorAvatar(if (question.isAnonymous) null else question.authorAvatar,
                                    if (question.isAnonymous) "Anonymous" else question.authorName, 34.dp)
                                Spacer(Modifier.width(8.dp))
                                AuthorByline(
                                    authorName = if (question.isAnonymous) "Anonymous" else question.authorName,
                                    createdAt = question.createdAt,
                                    modifier = Modifier.weight(1f)
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

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = question.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 26.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = question.body,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (question.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Keep every action in the same wrapping row on narrow screens.
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                    // UPVOTE
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onUpvoteClick() }
                                            .heightIn(min = 44.dp)
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
                                            .heightIn(min = 44.dp)
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
                                        modifier = Modifier.padding(vertical = 14.dp),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

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

                // Sort controls stay local to this question and preserve each reply thread.
                item {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(replies.size.toString() + " Answers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            listOf(ReplyOrder.TOP to "Top", ReplyOrder.NEWEST to "Newest").forEach { (order, label) ->
                                TextButton(onClick = { replyOrder = order },
                                    modifier = Modifier.semantics { selected = replyOrder == order },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.textButtonColors(containerColor = if (replyOrder == order)
                                        MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) {
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // REPLIES LIST
                items(replyRows, key = { it.first.id }) { (reply, isChild) ->
                    ReplyCard(
                        reply = reply,
                        modifier = Modifier.padding(start = if (isChild) 24.dp else 0.dp),
                        replyEnabled = !isSubmitting,
                        voteEnabled = reply.id !in pendingVotes,
                        onVoteClick = { voteType ->
                            if (reply.id !in pendingVotes) {
                                pendingVotes = pendingVotes + reply.id
                                onReplyVote(reply.id, voteType) { error ->
                                    pendingVotes = pendingVotes - reply.id
                                    replyError = error
                                }
                            }
                        }
                    ) {
                        reply.authorUsername?.takeIf { it.isNotBlank() }?.let { username ->
                            val text = ReplyMentions.prepend(replyText.text, username)
                            val mentionEnd = username.trim().removePrefix("@").length + 2
                            replyText = TextFieldValue(text, TextRange(mentionEnd.coerceAtMost(text.length)))
                            selectedParentId = reply.id
                            selectedParentUsername = username
                            replyError = null
                            replyFocusRequester.requestFocus()
                            keyboard?.show()
                        }
                    }
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
                    selectedParentId?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Replying to @${selectedParentUsername.orEmpty()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(enabled = !isSubmitting, onClick = {
                                selectedParentId = null
                                selectedParentUsername = null
                            }) { Text("Cancel", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
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
                                if (it.text.isBlank()) {
                                    selectedParentId = null
                                    selectedParentUsername = null
                                }
                                replyError = null
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).focusRequester(replyFocusRequester),
                            placeholder = { Text("Write an answer...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            enabled = replyText.text.isNotBlank() && !isSubmitting,
                            onClick = {
                                if (replyText.text.isNotBlank() && !isSubmitting) {
                                    val submittedText = replyText.text.trim()
                                    val submittedParentId = selectedParentId
                                    isSubmitting = true
                                    replyError = null
                                    scope.launch {
                                        val moderationResult = geminiService.checkReplyModeration(submittedText)
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

                                        onSubmitReply(submittedText, submittedParentId) { error ->
                                            isSubmitting = false
                                            if (error != null) {
                                                replyError = error
                                            } else {
                                                replyText = TextFieldValue()
                                                selectedParentId = null
                                                selectedParentUsername = null
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                    Text("●  Posting as " + replyAuthorName, Modifier.padding(top = 6.dp, start = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReplyCard(
    reply: ReplyUiModel,
    replyEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    voteEnabled: Boolean = true,
    onVoteClick: (Int) -> Unit = {},
    onReplyClick: () -> Unit = {}
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthorAvatar(reply.authorAvatar, reply.authorName, 30.dp)
                AuthorByline(authorName = reply.authorName, createdAt = reply.createdAt)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    append(reply.body)
                    ReplyMentions.pattern.findAll(reply.body).forEach { match ->
                        addStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                },
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onVoteClick(1) }, enabled = voteEnabled,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    val tint = if (reply.currentUserVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(
                        imageVector = if (reply.currentUserVote == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like reply", modifier = Modifier.size(17.dp), tint = tint
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(reply.upvoteCount.toString(), fontSize = 12.sp, color = tint)
                }
                TextButton(
                    onClick = { onVoteClick(-1) }, enabled = voteEnabled,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    val tint = if (reply.currentUserVote == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(
                        imageVector = if (reply.currentUserVote == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike reply", modifier = Modifier.size(17.dp), tint = tint
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(reply.downvoteCount.toString(), fontSize = 12.sp, color = tint)
                }
                TextButton(
                    onClick = onReplyClick,
                    enabled = replyEnabled && !reply.authorUsername.isNullOrBlank(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Reply", style = MaterialTheme.typography.labelSmall)
                }
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
