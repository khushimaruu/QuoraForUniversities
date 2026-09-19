package com.MADproject.quoraforuniversities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onQuestionClick: (String) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {},
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    // Create category names from the tags already used in questions
    val categories = remember(questions) {
        questions
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }

    // Search by question title, body, author or category/tag
    val filteredQuestions = remember(searchQuery, questions) {
        questions.filter { question ->

            question.title.contains(
                searchQuery,
                ignoreCase = true
            ) ||

                    question.body.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||

                    question.authorName.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||

                    question.tags.any { tag ->
                        tag.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                    }
        }
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
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,

                onValueChange = {
                    searchQuery = it
                },

                modifier = Modifier.fillMaxWidth(),

                placeholder = {
                    Text("Search questions, tags or users...")
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

            // ---------------------------------------------------------
            // SHOW CATEGORIES WHEN NOTHING IS BEING SEARCHED
            // ---------------------------------------------------------

            if (searchQuery.isBlank()) {

                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {

                    items(categories) { category ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = category
                                },

                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
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

                // ---------------------------------------------------------
                // SHOW RESULTS ONLY AFTER SEARCH/CATEGORY SELECTION
                // ---------------------------------------------------------

                if (filteredQuestions.isEmpty()) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "No matching questions found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                } else {

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {

                        items(
                            filteredQuestions,
                            key = { it.id }
                        ) { question ->

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
// PREVIEW
// ---------------------------------------------------------------------------

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