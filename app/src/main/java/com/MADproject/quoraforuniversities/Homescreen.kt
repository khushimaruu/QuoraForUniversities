package com.MADproject.quoraforuniversities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.components.AppHeader
import com.MADproject.quoraforuniversities.components.BottomNavBar
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionCard
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme

@Composable
fun HomeScreen(
    questions: List<QuestionUiModel>,
    isLoading: Boolean = false,
    savedPostIds: Set<String> = emptySet(),
    onQuestionClick: (String) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {},
    onDownvoteClick: (String) -> Unit = {},
    onBookmarkClick: ((String) -> Unit)? = null,
    onReportClick: ((postId: String, reason: String) -> Unit)? = null,
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {},
) {
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader() },
        bottomBar = {
            BottomNavBar(
                currentRoute = BottomNavDestination.HOME.route,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
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
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(questions, key = { it.id }) { question ->
                            QuestionCard(
                                question = question,
                                onClick = onQuestionClick,
                                onUpvoteClick = onUpvoteClick,
                                onDownvoteClick = onDownvoteClick,
                                onBookmarkClick = onBookmarkClick,
                                onReportClick = onReportClick,
                                isBookmarked = savedPostIds.contains(question.id)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PREVIEW — sample data only.
// ---------------------------------------------------------------------------
val sampleQuestions = listOf(
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
        authorName = "aditri mehta",
        isAnonymous = false,
        tags = listOf("CampusLife"),
        voteCount = 27,
        answerCount = 9
    )
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CampusQnATheme {
        HomeScreen(questions = sampleQuestions)
    }
}
