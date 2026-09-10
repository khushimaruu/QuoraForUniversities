package com.MADproject.quoraforuniversities


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/**
 * Home feed. Header and bottom nav are shared components — reuse them as-is
 * on Search, Ask, and Profile so the shell stays consistent across screens.
 *
 * Data is passed in, not fetched here. Wire `questions` to
 * `viewModel.questions.collectAsState()` once your Supabase repository
 * is ready; nothing else in this file needs to change.
 */
@Composable
fun HomeScreen(
    questions: List<QuestionUiModel>,
    isLoading: Boolean = false,
    onQuestionClick: (String) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {},
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {},
) {
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
// PREVIEW — sample data only.
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
    CampusQnATheme {
        HomeScreen(questions = sampleQuestions)
    }
}