package com.MADproject.quoraforuniversities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// 1. DATA MODEL
// This is what a "Question" looks like on screen. When you wire up Supabase,
// map your Postgres row (or DTO) into this shape before passing it to the UI.
// Keeping this separate from your network model means the UI never needs to
// change when your backend schema does.
// ---------------------------------------------------------------------------
data class QuestionUiModel(
    val id: String,
    val title: String,
    val body: String,
    val authorName: String,      // "Anonymous" if posted anonymously
    val isAnonymous: Boolean,
    val tags: List<String>,
    val voteCount: Int,
    val answerCount: Int
)

// ---------------------------------------------------------------------------
// 2. REUSABLE POST CARD
// This is the piece you'll reuse everywhere a question is listed: Home feed,
// search results, tag filter results, "My Questions" on profile, etc.
// It takes plain data in and emits a click callback out — no ViewModel,
// no Supabase import, no coroutine here. That's what makes it reusable.
// ---------------------------------------------------------------------------
@Composable
fun QuestionCard(
    question: QuestionUiModel,
    onClick: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(question.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (question.isAnonymous)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (question.isAnonymous) "?" else question.authorName.first().toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (question.isAnonymous)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (question.isAnonymous) "Anonymous" else question.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Body preview
            Text(
                text = question.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (question.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: votes + answers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUpvoteClick(question.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Upvote",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${question.voteCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${question.answerCount} answers",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. HOME SCREEN
// Takes a list of questions + loading/error state as parameters — it does
// NOT fetch data itself. A ViewModel (backed by your Supabase repository)
// will own the real data and pass it in. For now, wire it to sample data
// or an empty list.
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    questions: List<QuestionUiModel>,
    isLoading: Boolean = false,
    onQuestionClick: (String) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {},
    onAskQuestionClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Q&A", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAskQuestionClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ask") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                questions.isEmpty() -> {
                    Text(
                        text = "No questions yet. Be the first to ask!",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(questions, key = { it.id }) { question ->
                            QuestionCard(
                                question = question,
                                onClick = onQuestionClick,
                                onUpvoteClick = onUpvoteClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. PREVIEW — sample data only, delete or keep for design iteration.
// Replace this with viewModel.questions.collectAsState() once your
// Supabase repository is ready.
// ---------------------------------------------------------------------------
private val sampleQuestions = listOf(
    QuestionUiModel(
        id = "1",
        title = "How do I add a backlog course to my timetable?",
        body = "I failed one course last semester and need to retake it alongside my current courses. Not sure how registration handles this.",
        authorName = "Anonymous",
        isAnonymous = true,
        tags = listOf("CSE", "Year2", "Registration"),
        voteCount = 12,
        answerCount = 4
    ),
    QuestionUiModel(
        id = "2",
        title = "Best cafes near the north campus for group study?",
        body = "Looking for a place with decent wifi and enough seating for 4-5 people, preferably open till late.",
        authorName = "Riya Sharma",
        isAnonymous = false,
        tags = listOf("CampusLife"),
        voteCount = 27,
        answerCount = 9
    )
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(questions = sampleQuestions)
    }
}