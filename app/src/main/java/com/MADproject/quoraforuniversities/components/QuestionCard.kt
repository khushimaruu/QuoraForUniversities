package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Plain UI-layer model */
data class QuestionUiModel(
    val id: String,
    val authorId: String = "",
    val title: String,
    val body: String,
    val authorName: String,
    val isAnonymous: Boolean,
    val tags: List<String>,
    val voteCount: Int = 0,
    val answerCount: Int = 0,
    val upvoteCount: Int = voteCount,
    val downvoteCount: Int = 0,
    val currentUserVote: Int = 0, // 1 for upvote, -1 for downvote, 0 for none
    val createdAt: String? = null
)

val CommunityReportReasons = listOf(
    "Harassment or Abusive Language",
    "False or Misleading Information",
    "Spam or Advertisements",
    "Academic Dishonesty / Cheating",
    "Other"
)

/**
 * Reusable post card with separate Like/Dislike, 3-dots overflow menu, and Reporting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionCard(
    question: QuestionUiModel, onClick: (String) -> Unit, onUpvoteClick: (String) -> Unit,
    modifier: Modifier = Modifier, onDownvoteClick: ((String) -> Unit)? = null,
    onBookmarkClick: ((String) -> Unit)? = null, onReportClick: ((String, String) -> Unit)? = null,
    isBookmarked: Boolean = false
) {
    var menu by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    OutlinedCard(onClick = { onClick(question.id) }, modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), Alignment.Center) {
                    Text(if (question.isAnonymous) "?" else question.authorName.firstOrNull()?.uppercase() ?: "S",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                AuthorByline(if (question.isAnonymous) "Anonymous" else question.authorName, question.createdAt, Modifier.weight(1f))
                if (onReportClick != null) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Question options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Report question") }, onClick = { menu = false; report = true })
                    }
                }
            }
            Text(question.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (question.body.isNotBlank()) Text(question.body, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (question.tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                question.tags.forEach { tag -> Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(5.dp)) {
                    Text("#" + tag, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onUpvoteClick(question.id) },
                    colors = ButtonDefaults.textButtonColors(containerColor = if (question.currentUserVote == 1)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Icon(if (question.currentUserVote == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        if (question.currentUserVote == 1) "Remove upvote" else "Upvote", Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp)); Text(question.upvoteCount.toString())
                }
                if (onDownvoteClick != null) TextButton(onClick = { onDownvoteClick(question.id) },
                    contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Icon(if (question.currentUserVote == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        if (question.currentUserVote == -1) "Remove downvote" else "Downvote", Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp)); Text(question.downvoteCount.toString())
                }
                TextButton(onClick = { onClick(question.id) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text(question.answerCount.toString() + " answers", style = MaterialTheme.typography.labelMedium)
                }
                if (onBookmarkClick != null) IconButton(onClick = { onBookmarkClick(question.id) }) {
                    Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        if (isBookmarked) "Unsave question" else "Save question", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    if (report) ReportPostDialog(onDismiss = { report = false }, onConfirmReport = {
        report = false; onReportClick?.invoke(question.id, it)
    })
}

/**
 * Reusable clean community guidelines report dialog
 */
@Composable
fun ReportPostDialog(
    onDismiss: () -> Unit,
    onConfirmReport: (reason: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(CommunityReportReasons.first()) }
    var customDetails by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Report Content",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Help us understand what's wrong with this post. Posts reported by multiple students are automatically hidden.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                CommunityReportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            fontWeight = if (selectedReason == reason) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                if (selectedReason == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDetails,
                        onValueChange = { customDetails = it },
                        placeholder = { Text("Please specify (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Other" && customDetails.isNotBlank()) {
                        "Other: ${customDetails.trim()}"
                    } else {
                        selectedReason
                    }
                    onConfirmReport(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
