package com.MADproject.quoraforuniversities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
fun SearchPage(
    questions: List<QuestionUiModel>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    searchResults: List<QuestionUiModel> = emptyList(),
    isSearching: Boolean = false,
    searchError: String? = null,
    savedPostIds: Set<String> = emptySet(),
    onQuestionClick: (QuestionUiModel) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {},
    onBookmarkClick: ((String) -> Unit)? = null,
    onReportClick: ((postId: String, reason: String) -> Unit)? = null,
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {

    // Keep states alive while results are temporarily replaced by a loading UI.
    val categoriesListState = rememberLazyListState()
    val resultsListState = rememberSaveable(searchQuery, saver = LazyListState.Saver) {
        LazyListState()
    }

    // CREATE CATEGORIES FROM EXISTING QUESTION TAGS
    val categories = remember(questions) {
        questions
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader()
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = BottomNavDestination.SEARCH.route,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search questions or categories...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SHOW CATEGORIES WHEN SEARCH IS EMPTY
            if (searchQuery.isBlank()) {

                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = categoriesListState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {

                    items(categories, key = { it }) { category ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSearchQueryChange(category)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 3.dp
                            )
                        ) {

                            Text(
                                text = category,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

            } else {

                // SHOW LOADING, ERRORS OR SEARCH RESULTS
                when {

                    isSearching && searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    searchError != null && searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = searchError ?: "Search failed",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching questions found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = resultsListState,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    items = searchResults,
                                    key = { it.id }
                                ) { question ->
                                    QuestionCard(
                                        question = question,
                                        onClick = { onQuestionClick(question) },
                                        onUpvoteClick = onUpvoteClick,
                                        onBookmarkClick = onBookmarkClick,
                                        onReportClick = onReportClick,
                                        isBookmarked = savedPostIds.contains(question.id)
                                    )
                                }
                            }
                            if (isSearching) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// PREVIEW
@Preview(showBackground = true)
@Composable
fun SearchPagePreview() {

    val previewQuestions = listOf(
        QuestionUiModel(
            id = "1",
            title = "How do I add a backlog course to my timetable?",
            body = "I failed one course last semester and need to retake it alongside my current courses.",
            authorName = "Anonymous",
            isAnonymous = true,
            tags = listOf(
                "CSE",
                "Year2",
                "Registration"
            ),
            voteCount = 12,
            answerCount = 4
        ),
        QuestionUiModel(
            id = "2",
            title = "Best cafes near the north campus for group study?",
            body = "Looking for a place with decent wifi and enough seating.",
            authorName = "Riya Sharma",
            isAnonymous = false,
            tags = listOf(
                "CampusLife"
            ),
            voteCount = 27,
            answerCount = 9
        )
    )

    CampusQnATheme {
        SearchPage(
            questions = previewQuestions
        )
    }
}
