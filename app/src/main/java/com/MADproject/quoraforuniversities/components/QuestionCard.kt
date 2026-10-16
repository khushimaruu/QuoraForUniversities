package com.MADproject.quoraforuniversities.components

import androidx.compose.foundation.background
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
    val currentUserVote: Int = 0 // 1 for upvote, -1 for downvote, 0 for none
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
@Composable
fun QuestionCard(
    question: QuestionUiModel,
    onClick: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDownvoteClick: ((String) -> Unit)? = null,
    onBookmarkClick: ((String) -> Unit)? = null,
    onReportClick: ((postId: String, reason: String) -> Unit)? = null,
    isBookmarked: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(question.id) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Author row + 3-dots overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val avatarBrush = if (question.isAnonymous) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(avatarBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (question.isAnonymous) "?" else question.authorName.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (question.isAnonymous)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (question.isAnonymous) "Anonymous" else question.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3-dots menu button
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = question.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = question.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (question.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Separate Upvote, Downvote, Answers, Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SEPARATE UPVOTE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onUpvoteClick(question.id) }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (question.currentUserVote == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Upvote",
                            modifier = Modifier.size(16.dp),
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

                    // SEPARATE DOWNVOTE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDownvoteClick?.invoke(question.id) }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (question.currentUserVote == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            contentDescription = "Downvote",
                            modifier = Modifier.size(16.dp),
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

                    // ANSWERS COUNT
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Answers",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${question.answerCount} answers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onBookmarkClick != null) {
                    IconButton(
                        onClick = { onBookmarkClick(question.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Unsave" else "Save",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false },
            onConfirmReport = { selectedReason ->
                showReportDialog = false
                onReportClick?.invoke(question.id, selectedReason)
            }
        )
    }
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
