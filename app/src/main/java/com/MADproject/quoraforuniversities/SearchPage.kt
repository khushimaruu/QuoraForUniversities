package com.MADproject.quoraforuniversities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onAuthorClick: ((String) -> Unit)? = null,
    onNavDestinationSelected: (BottomNavDestination) -> Unit = {},
    onDeletePost: ((String) -> Unit)? = null
) {
    val categoriesState = rememberLazyListState()
    val resultsState = rememberSaveable(searchQuery, saver = LazyListState.Saver) { LazyListState() }
    var showAllTopics by rememberSaveable { mutableStateOf(false) }
    val topicCounts = remember(questions) { questions.flatMap { it.tags }.groupingBy { it }.eachCount() }
    val categories = remember(topicCounts) {
        (availableThemes + topicCounts.keys).distinct().sortedWith(
            compareByDescending<String> { topicCounts[it] ?: 0 }.thenBy { it })
    }
    Scaffold(topBar = { AppHeader() }, bottomBar = { BottomNavBar("search", onNavDestinationSelected) }) { padding ->
        LazyColumn(
            state = if (searchQuery.isBlank()) categoriesState else resultsState,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().readingWidth(),
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Find an answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item { OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Search questions or topics") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Outlined.Close, "Clear search") } }) }
            if (searchQuery.isBlank()) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Browse topics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (categories.size > 8) TextButton(onClick = { showAllTopics = !showAllTopics },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Text(if (showAllTopics) "Show less" else "See all", style = MaterialTheme.typography.labelMedium)
                    }
                } }
                val visibleTopics = if (showAllTopics) categories else categories.take(8)
                items(visibleTopics.chunked(2)) { rowTopics ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTopics.forEach { topic -> TopicTile(topic, topicCounts[topic] ?: 0,
                            onClick = { onSearchQueryChange(topic) }, modifier = Modifier.weight(1f)) }
                        if (rowTopics.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                if (isSearching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (searchError != null) item { ScreenMessage("Search unavailable", searchError) }
                else if (!isSearching && searchResults.isEmpty()) item { ScreenMessage("No matching questions", "Try another keyword or browse a different topic.") }
                else if (!isSearching) item { Text("${searchResults.size} matching questions", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold) }
                items(searchResults, key = { it.id }) { question -> QuestionCard(
                    question, { onQuestionClick(question) }, onUpvoteClick,
                    onDownvoteClick = onDownvoteClick, onBookmarkClick = onBookmarkClick,
                    onReportClick = onReportClick, onAuthorClick = onAuthorClick, onDeleteClick = onDeletePost,
                    isBookmarked = question.id in savedPostIds) }
            }
        }
    }
}

@Composable
private fun TopicTile(topic: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(topic, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (count == 1) "1 question" else "$count questions",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
