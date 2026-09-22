package com.MADproject.quoraforuniversities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
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
import com.MADproject.quoraforuniversities.components.BottomNavBar
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme

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
    onBackClick: () -> Unit = {},
    onUpvoteClick: () -> Unit = {},
    onSubmitReply: (String) -> Unit = {},
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {

    var replyText by remember {
        mutableStateOf("")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(subtitle = "Question Details")
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

            // CONTENT

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // FULL QUESTION

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            // AUTHOR

                            Text(
                                text = if (question.isAnonymous)
                                    "Anonymous"
                                else
                                    question.authorName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            // TITLE

                            Text(
                                text = question.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 29.sp
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            // FULL BODY

                            Text(
                                text = question.body,
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            // TAGS

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                question.tags.forEach { tag ->

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {

                                        Text(
                                            text = tag,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(
                                                horizontal = 9.dp,
                                                vertical = 5.dp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            HorizontalDivider()

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            // VOTE / ANSWER ROW

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                IconButton(
                                    onClick = onUpvoteClick
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = "Upvote"
                                    )
                                }

                                Text(
                                    text = question.voteCount.toString(),
                                    fontSize = 14.sp
                                )

                                Spacer(
                                    modifier = Modifier.width(20.dp)
                                )

                                Text(
                                    text = "${replies.size} Answers",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ANSWERS TITLE

                item {

                    Text(
                        text = "${replies.size} Answers",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // REPLIES

                items(
                    replies,
                    key = { it.id }
                ) { reply ->

                    ReplyCard(
                        reply = reply
                    )
                }
            }

            // WRITE ANSWER

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = {
                            replyText = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Write an answer...")
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    IconButton(
                        onClick = {

                            if (replyText.isNotBlank()) {

                                onSubmitReply(
                                    replyText
                                )

                                replyText = ""
                            }
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}


// ------------------------------------------------------------
// REPLY CARD
// ------------------------------------------------------------

@Composable
fun ReplyCard(
    reply: ReplyUiModel
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = reply.body,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = "Votes",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

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
    ),

    ReplyUiModel(
        id = "3",
        authorName = "Arjun Patel",
        body = "Also make sure that the course doesn't clash with your current timetable.",
        voteCount = 3
    )
)

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PostDetailPagePreview() {
    val question = QuestionUiModel(
        id = "1",
        title = "How do I add a backlog course to my timetable?",
        body = "I failed one course last semester and need to retake it alongside my current courses. Not sure how registration handles this.",
        authorName = "Anonymous",
        isAnonymous = true,
        tags = listOf("CSE", "Year2", "Registration"),
        voteCount = 12,
        answerCount = 4
    )

    CampusQnATheme {

        PostDetailPage(
            question = question,
            replies = sampleReplies
        )
    }
}