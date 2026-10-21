package com.MADproject.quoraforuniversities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.components.*
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    questions: List<QuestionUiModel>, isLoading: Boolean = false, savedPostIds: Set<String> = emptySet(),
    onQuestionClick: (String) -> Unit = {}, onUpvoteClick: (String) -> Unit = {},
    onDownvoteClick: (String) -> Unit = {}, onBookmarkClick: ((String) -> Unit)? = null,
    onReportClick: ((postId: String, reason: String) -> Unit)? = null,
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {},
    error: String? = null, onRetry: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    var filter by rememberSaveable { mutableStateOf("All") }
    var showTip by rememberSaveable { mutableStateOf(true) }
    val topics = remember(questions) { questions.flatMap { it.tags }.distinct().take(5) }
    val visible = when (filter) {
        "All" -> questions
        "Trending" -> questions.sortedByDescending { it.upvoteCount }
        else -> questions.filter { filter in it.tags }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { AppHeader() },
        bottomBar = { BottomNavBar("home", onNavDestinationSelected) },
        floatingActionButton = { Button(onClick = { onNavDestinationSelected(BottomNavDestination.ADD_POST) }) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text("Ask question", style = MaterialTheme.typography.labelMedium)
        } }
    ) { padding ->
        LazyColumn(
            state = listState, modifier = Modifier.padding(padding).consumeWindowInsets(padding).readingWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("All", "Trending") + topics).distinct().forEach { topic ->
                    FilterChip(selected = filter == topic, onClick = { filter = topic }, label = { Text(topic) })
                }
            } }
            if (showTip) item { Surface(shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Lightbulb, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("A little campus wisdom", style = MaterialTheme.typography.titleSmall)
                        Text("Share what you know. Someone on campus might need it.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showTip = false }) { Icon(Icons.Outlined.Close, "Dismiss campus tip", Modifier.size(16.dp)) }
                }
            } }
            if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (error != null) item { ScreenMessage("Couldn't load questions", error, "Try again", onRetry) }
            else if (!isLoading && visible.isEmpty()) item { ScreenMessage("Start the conversation",
                "Ask a question and let your campus community help.", "Ask a question",
                { onNavDestinationSelected(BottomNavDestination.ADD_POST) }) }
            items(visible, key = { it.id }) { question -> QuestionCard(
                question = question, onClick = onQuestionClick, onUpvoteClick = onUpvoteClick,
                onDownvoteClick = onDownvoteClick, onBookmarkClick = onBookmarkClick,
                onReportClick = onReportClick, isBookmarked = question.id in savedPostIds) }
        }
    }
}

val sampleQuestions = listOf(
    QuestionUiModel(id = "1", title = "How do I add a backlog course to my timetable?",
        body = "I need to retake a course alongside my current subjects. How does registration work?",
        authorName = "Anonymous", isAnonymous = true, tags = listOf("CSE", "Year2", "Registration"), voteCount = 12, answerCount = 4),
    QuestionUiModel(id = "2", title = "Best study spots on campus with natural light?",
        body = "Looking for a quiet spot with Wi-Fi and room for our study group.",
        authorName = "Aditri Mehta", isAnonymous = false, tags = listOf("CampusLife"), voteCount = 27, answerCount = 9)
)

@Preview(name = "Small phone", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Large phone", widthDp = 430, heightDp = 932, showBackground = true)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Preview(name = "Landscape", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun HomeScreenPreview() { CampusQnATheme { HomeScreen(sampleQuestions) } }
