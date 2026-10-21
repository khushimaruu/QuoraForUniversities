package com.MADproject.quoraforuniversities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.components.*
import com.MADproject.quoraforuniversities.ui.addpost.availableThemes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPage(
    questions: List<QuestionUiModel>, searchQuery: String = "", onSearchQueryChange: (String) -> Unit = {},
    searchResults: List<QuestionUiModel> = emptyList(), isSearching: Boolean = false, searchError: String? = null,
    savedPostIds: Set<String> = emptySet(), onQuestionClick: (QuestionUiModel) -> Unit = {},
    onUpvoteClick: (String) -> Unit = {}, onDownvoteClick: (String) -> Unit = {},
    onBookmarkClick: ((String) -> Unit)? = null, onReportClick: ((String, String) -> Unit)? = null,
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {}
) {
    val categoriesState = rememberLazyListState()
    val resultsState = rememberSaveable(searchQuery, saver = LazyListState.Saver) { LazyListState() }
    val categories = remember(questions) { (questions.flatMap { it.tags } + availableThemes).distinct().sorted() }
    Scaffold(topBar = { AppHeader(subtitle = "Explore topics") }, bottomBar = { BottomNavBar("search", onNavDestinationSelected) }) { padding ->
        LazyColumn(
            state = if (searchQuery.isBlank()) categoriesState else resultsState,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().readingWidth(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { PageTitle("Find your next answer", "Explore what your campus is talking about.") }
            item { OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search questions or topics") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Outlined.Close, "Clear search") } }) }
            if (searchQuery.isBlank()) {
                item { Text("Browse topics", style = MaterialTheme.typography.titleMedium) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { topic -> SuggestionChip(onClick = { onSearchQueryChange(topic) }, label = { Text(topic) }) }
                } }
            } else {
                if (isSearching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (searchError != null) item { ScreenMessage("Search unavailable", searchError) }
                else if (!isSearching && searchResults.isEmpty()) item { ScreenMessage("No matching questions", "Try another keyword or browse a different topic.") }
                else if (!isSearching) item { Text("${searchResults.size} matching questions", style = MaterialTheme.typography.titleSmall) }
                items(searchResults, key = { it.id }) { question -> QuestionCard(
                    question, { onQuestionClick(question) }, onUpvoteClick,
                    onDownvoteClick = onDownvoteClick, onBookmarkClick = onBookmarkClick,
                    onReportClick = onReportClick, isBookmarked = question.id in savedPostIds) }
            }
        }
    }
}
