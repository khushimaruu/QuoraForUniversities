package com.MADproject.quoraforuniversities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.OfflineSystemBanner
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.components.SplashScreen
import com.MADproject.quoraforuniversities.data.AuthRepository
import com.MADproject.quoraforuniversities.data.PostRepository
import com.MADproject.quoraforuniversities.data.ProfileAnswerUiModel
import com.MADproject.quoraforuniversities.data.ProfileRepository
import com.MADproject.quoraforuniversities.ui.addpost.AddPostScreen
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import com.MADproject.quoraforuniversities.utils.ConnectivityStatus
import com.MADproject.quoraforuniversities.utils.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK)
        )
        setContent {
            CampusQnATheme {
                CampusQnAApp()
            }
        }
    }
}

@Composable
fun CampusQnAApp(
    context: android.content.Context = LocalContext.current,
    authRepository: AuthRepository = remember { AuthRepository() },
    postRepository: PostRepository = remember { PostRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() }
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val networkObserver = remember { NetworkConnectivityObserver(context) }
    val connectivityStatus by networkObserver.observe().collectAsState(initial = ConnectivityStatus.Available)
    val isOffline = connectivityStatus == ConnectivityStatus.Lost || connectivityStatus == ConnectivityStatus.Unavailable

    var currentScreen by rememberSaveable { mutableStateOf("splash") }
    var postReturnScreen by rememberSaveable { mutableStateOf("home") }
    val listScreenState = rememberSaveableStateHolder()
    // Search data outlives its AnimatedContent destination; only the query goes in saved state.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isSearching by remember { mutableStateOf(searchQuery.isNotBlank()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRefreshVersion by remember { mutableLongStateOf(0L) }
    var selectedQuestion by remember { mutableStateOf<QuestionUiModel?>(null) }
    var questions by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isFeedLoading by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    var replies by remember { mutableStateOf<List<ReplyUiModel>>(emptyList()) }
    var repliesForPostId by remember { mutableStateOf<String?>(null) }
    var repliesRequestVersion by remember { mutableLongStateOf(0L) }
    var savedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var savedQuestions by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isSavedLoading by remember { mutableStateOf(false) }

    var userCreatedPosts by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isUserPostsLoading by remember { mutableStateOf(false) }
    var userAnswers by remember { mutableStateOf<List<ProfileAnswerUiModel>>(emptyList()) }
    var isUserAnswersLoading by remember { mutableStateOf(false) }
    var postingName by remember { mutableStateOf("Your profile") }

    fun refreshSavedPosts() {
        scope.launch {
            if (savedQuestions.isEmpty()) {
                isSavedLoading = true
            }
            val ids = postRepository.getSavedPostIds()
            savedPostIds = ids
            val res = postRepository.getSavedPosts()
            isSavedLoading = false
            if (res.isSuccess) {
                savedQuestions = res.getOrDefault(emptyList())
            }
        }
    }

    fun refreshUserPosts() {
        scope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            profileRepository.getProfile(uid).onSuccess { postingName = it?.name ?: it?.username ?: "Your profile" }
            isUserPostsLoading = true
            val res = profileRepository.getUserPosts(uid)
            isUserPostsLoading = false
            if (res.isSuccess) {
                userCreatedPosts = res.getOrDefault(emptyList())
            } else {
                snackbarHostState.showSnackbar("Couldn't load your questions. Please try opening your profile again.")
            }
            isUserAnswersLoading = true
            profileRepository.getUserAnswers(uid).onSuccess { userAnswers = it }
                .onFailure { snackbarHostState.showSnackbar("Couldn't load your answers. Please try again.") }
            isUserAnswersLoading = false
        }
    }

    fun toggleBookmark(postId: String) {
        scope.launch {
            val result = if (savedPostIds.contains(postId)) {
                postRepository.unbookmarkPost(postId)
            } else {
                postRepository.bookmarkPost(postId)
            }
            if (result.isSuccess) refreshSavedPosts()
            else snackbarHostState.showSnackbar("Couldn't update your saved questions. Please try again.")
        }
    }

    fun refreshQuestions() {
        scope.launch {
            if (questions.isEmpty()) {
                isFeedLoading = true
            }
            val result = postRepository.getPosts()
            isFeedLoading = false
            if (result.isSuccess) {
                val updatedList = result.getOrDefault(emptyList())
                questions = updatedList
                feedError = null
                // Refresh cached search data after real feed changes, never just on returning.
                searchRefreshVersion++
                selectedQuestion?.let { currentSq ->
                    val updatedSq = updatedList.find { it.id == currentSq.id }
                    if (updatedSq != null) {
                        selectedQuestion = updatedSq
                    }
                }
            } else {
                feedError = "Check your connection and try again."
            }
        }
    }

    fun refreshReplies(postId: String) {
        val requestVersion = ++repliesRequestVersion
        if (repliesForPostId != postId) {
            replies = emptyList()
            repliesForPostId = postId
        }
        scope.launch {
            val result = postRepository.getReplies(postId)
            if (result.isSuccess && selectedQuestion?.id == postId && requestVersion == repliesRequestVersion) {
                replies = result.getOrDefault(emptyList())
            } else if (result.isFailure && selectedQuestion?.id == postId && requestVersion == repliesRequestVersion) {
                snackbarHostState.showSnackbar("Couldn't load answers. Please reopen this question to try again.")
            }
        }
    }

    // INSTANT UPVOTE: Updates UI numbers on all active screens immediately and commits to Supabase
    fun handleUpvote(questionId: String) {
        val updateLogic: (QuestionUiModel) -> QuestionUiModel = { q ->
            if (q.id == questionId) {
                if (q.currentUserVote == 1) {
                    // Toggle OFF
                    val newUp = (q.upvoteCount - 1).coerceAtLeast(0)
                    q.copy(
                        upvoteCount = newUp,
                        voteCount = newUp - q.downvoteCount,
                        currentUserVote = 0
                    )
                } else {
                    val newUp = q.upvoteCount + 1
                    val newDown = if (q.currentUserVote == -1) (q.downvoteCount - 1).coerceAtLeast(0) else q.downvoteCount
                    q.copy(
                        upvoteCount = newUp,
                        downvoteCount = newDown,
                        voteCount = newUp - newDown,
                        currentUserVote = 1
                    )
                }
            } else q
        }

        // Apply immediately to current view state
        questions = questions.map(updateLogic)
        selectedQuestion = selectedQuestion?.let(updateLogic)
        savedQuestions = savedQuestions.map(updateLogic)
        userCreatedPosts = userCreatedPosts.map(updateLogic)

        scope.launch {
            val result = postRepository.votePost(questionId, 1)
            if (result.isSuccess) {
                // Brief stabilization pause to ensure Supabase Postgres commit completes before re-fetching
                delay(400)
                refreshQuestions()
            } else {
                // Rollback to actual DB state on network or DB failure
                refreshQuestions()
            }
        }
    }

    // INSTANT DOWNVOTE: Updates UI numbers on all active screens immediately and commits to Supabase
    fun handleDownvote(questionId: String) {
        val updateLogic: (QuestionUiModel) -> QuestionUiModel = { q ->
            if (q.id == questionId) {
                if (q.currentUserVote == -1) {
                    // Toggle OFF
                    val newDown = (q.downvoteCount - 1).coerceAtLeast(0)
                    q.copy(
                        downvoteCount = newDown,
                        voteCount = q.upvoteCount - newDown,
                        currentUserVote = 0
                    )
                } else {
                    val newDown = q.downvoteCount + 1
                    val newUp = if (q.currentUserVote == 1) (q.upvoteCount - 1).coerceAtLeast(0) else q.upvoteCount
                    q.copy(
                        upvoteCount = newUp,
                        downvoteCount = newDown,
                        voteCount = newUp - newDown,
                        currentUserVote = -1
                    )
                }
            } else q
        }

        // Apply immediately to current view state
        questions = questions.map(updateLogic)
        selectedQuestion = selectedQuestion?.let(updateLogic)
        savedQuestions = savedQuestions.map(updateLogic)
        userCreatedPosts = userCreatedPosts.map(updateLogic)

        scope.launch {
            val result = postRepository.votePost(questionId, -1)
            if (result.isSuccess) {
                delay(400)
                refreshQuestions()
            } else {
                refreshQuestions()
            }
        }
    }

    // COMMUNITY REPORTING: Soft-hides post immediately for reporter and records in DB
    fun handleReportPost(postId: String, reason: String) {
        val currentUserId = authRepository.getCurrentUserId()
        val targetPost = questions.find { it.id == postId }
            ?: savedQuestions.find { it.id == postId }
            ?: userCreatedPosts.find { it.id == postId }

        if (targetPost != null && targetPost.authorId == currentUserId && currentUserId != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "You cannot report your own post.",
                    duration = SnackbarDuration.Short
                )
            }
            return
        }

        // Instantly soft-hide post on client side for the reporting student
        questions = questions.filter { it.id != postId }
        searchResults = searchResults.filter { it.id != postId }
        savedQuestions = savedQuestions.filter { it.id != postId }
        userCreatedPosts = userCreatedPosts.filter { it.id != postId }
        if (selectedQuestion?.id == postId) {
            selectedQuestion = null
            currentScreen = "home"
        }

        scope.launch {
            val result = postRepository.reportPost(postId, reason)
            if (result.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = "Post reported. Thank you for keeping CampusCircle safe.",
                    duration = SnackbarDuration.Short
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to submit report"
                snackbarHostState.showSnackbar(
                    message = if (err.contains("already reported", ignoreCase = true)) {
                        "You have already reported this post."
                    } else {
                        "Notice: $err"
                    },
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(searchQuery, searchRefreshVersion) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            searchError = null
            return@LaunchedEffect
        }

        isSearching = true
        searchError = null
        delay(300)

        val result = postRepository.searchPosts(searchQuery)
        // The repository returns Result, including cancellation; don't apply an obsolete query.
        coroutineContext.ensureActive()
        result.onSuccess {
            searchResults = it
        }.onFailure {
            // Keep the last successful results visible if a background refresh fails.
            searchError = "Search failed. Please try again."
        }
        isSearching = false
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == "profile") { refreshSavedPosts(); refreshUserPosts() }
    }

    LaunchedEffect(Unit) {
        refreshQuestions()
        if (authRepository.isLoggedIn()) {
            refreshSavedPosts()
            refreshUserPosts()
        }
        delay(1200)
        currentScreen = if (authRepository.isLoggedIn()) "home" else "landing"
    }

    if (currentScreen != "splash" && currentScreen != "home" && currentScreen != "landing") {
        BackHandler {
            currentScreen = when {
                !authRepository.isLoggedIn() -> "landing"
                currentScreen == "post" -> postReturnScreen
                else -> "home"
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp)
            )
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { width -> width / 12 } togetherWith
                            fadeOut(animationSpec = tween(180))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    "splash" -> SplashScreen()

                    "landing" -> {
                        LandingPage(
                            onLoginClick = { currentScreen = "login" },
                            onSignUpClick = { currentScreen = "signup" }
                        )
                    }

                    "login" -> {
                        LoginPage(
                            authRepository = authRepository,
                            onBackClick = { currentScreen = "landing" },
                            onLoginSuccess = {
                                refreshQuestions()
                                refreshSavedPosts()
                                refreshUserPosts()
                                currentScreen = "home"
                            },
                            onNavigateToSignUp = { currentScreen = "signup" }
                        )
                    }

                    "signup" -> {
                        SignUpPage(
                            authRepository = authRepository,
                            onBackClick = { currentScreen = "landing" },
                            onSignUpSuccess = {
                                refreshQuestions()
                                refreshSavedPosts()
                                refreshUserPosts()
                                currentScreen = "home"
                            },
                            onNavigateToLogin = { currentScreen = "login" }
                        )
                    }

                    "home" -> listScreenState.SaveableStateProvider("home") {
                        HomeScreen(
                            questions = questions,
                            isLoading = isFeedLoading,
                            error = feedError,
                            onRetry = { refreshQuestions() },
                            savedPostIds = savedPostIds,
                            onQuestionClick = { questionId ->
                                postReturnScreen = "home"
                                selectedQuestion = questions.find { it.id == questionId }
                                refreshReplies(questionId)
                                currentScreen = "post"
                            },
                            onUpvoteClick = { questionId -> handleUpvote(questionId) },
                            onDownvoteClick = { questionId -> handleDownvote(questionId) },
                            onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                            onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                    BottomNavDestination.SAVED -> { refreshSavedPosts(); currentScreen = "saved_posts" }
                                }
                            }
                        )
                    }

                    "search" -> listScreenState.SaveableStateProvider("search") {
                        SearchPage(
                            questions = questions,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            searchResults = searchResults,
                            isSearching = isSearching,
                            searchError = searchError,
                            savedPostIds = savedPostIds,
                            onQuestionClick = { question ->
                                postReturnScreen = "search"
                                selectedQuestion = question
                                refreshReplies(question.id)
                                currentScreen = "post"
                            },
                            onUpvoteClick = { questionId -> handleUpvote(questionId) },
                            onDownvoteClick = { questionId -> handleDownvote(questionId) },
                            onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                            onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                    BottomNavDestination.SAVED -> { refreshSavedPosts(); currentScreen = "saved_posts" }
                                }
                            }
                        )
                    }

                    "post" -> {
                        selectedQuestion?.let { question ->
                            PostDetailPage(
                                question = question,
                                replies = replies,
                                replyAuthorName = postingName,
                                isBookmarked = savedPostIds.contains(question.id),
                                onBackClick = { currentScreen = postReturnScreen },
                                onUpvoteClick = { handleUpvote(question.id) },
                                onDownvoteClick = { handleDownvote(question.id) },
                                onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                                onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                                onSubmitReply = { text, parentReplyId, onResult ->
                                    scope.launch {
                                        val result = postRepository.createReply(question.id, text, parentReplyId)
                                        if (result.isSuccess) {
                                            refreshReplies(question.id)
                                            refreshQuestions()
                                            onResult(null)
                                        } else {
                                            // Supabase exceptions can include request headers.
                                            onResult("Could not post your reply. Please try again.")
                                        }
                                    }
                                },
                                onReplyVote = { replyId, voteType, onResult ->
                                    scope.launch {
                                        val result = postRepository.voteReply(replyId, voteType)
                                        if (result.isSuccess) {
                                            val requestVersion = ++repliesRequestVersion
                                            val refreshed = postRepository.getReplies(question.id)
                                            if (refreshed.isSuccess && selectedQuestion?.id == question.id && requestVersion == repliesRequestVersion) {
                                                replies = refreshed.getOrThrow()
                                            }
                                            onResult(refreshed.exceptionOrNull()?.let {
                                                "Vote saved, but replies could not refresh. Reopen this post."
                                            })
                                        } else {
                                            onResult("Could not save your vote. Please try again.")
                                        }
                                    }
                                },
                                onNavDestinationSelected = { destination ->
                                    when (destination) {
                                        BottomNavDestination.HOME -> currentScreen = "home"
                                        BottomNavDestination.SEARCH -> currentScreen = "search"
                                        BottomNavDestination.PROFILE -> currentScreen = "profile"
                                        BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                    BottomNavDestination.SAVED -> { refreshSavedPosts(); currentScreen = "saved_posts" }
                                    }
                                }
                            )
                        }
                    }

                    "profile" -> listScreenState.SaveableStateProvider("profile") {
                        ProfileScreen(
                            authRepository = authRepository,
                            profileRepository = profileRepository,
                            savedCount = savedPostIds.size,
                            savedPostIds = savedPostIds,
                            userPosts = userCreatedPosts,
                            isLoadingPosts = isUserPostsLoading,
                            savedQuestions = savedQuestions,
                            userAnswers = userAnswers,
                            isLoadingAnswers = isUserAnswersLoading,
                            onQuestionClick = { questionId ->
                                postReturnScreen = "profile"
                                selectedQuestion = questions.find { it.id == questionId }
                                    ?: savedQuestions.find { it.id == questionId }
                                            ?: userCreatedPosts.find { it.id == questionId }
                                            ?: userAnswers.find { it.question.id == questionId }?.question
                                if (selectedQuestion != null) {
                                    refreshReplies(questionId)
                                    currentScreen = "post"
                                }
                            },
                            onUpvoteClick = { handleUpvote(it) },
                            onDownvoteClick = { handleDownvote(it) },
                            onBookmarkClick = { toggleBookmark(it) },
                            onReportClick = { id, reason -> handleReportPost(id, reason) },
                            onSavedPostsClick = { refreshSavedPosts() },
                            onLogOutClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                currentScreen = "landing"
                            },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                    BottomNavDestination.SAVED -> { refreshSavedPosts(); currentScreen = "saved_posts" }
                                }
                            }
                        )
                    }

                    "saved_posts" -> {
                        SavedPostsPage(
                            savedQuestions = savedQuestions,
                            savedPostIds = savedPostIds,
                            isLoading = isSavedLoading,
                            onQuestionClick = { questionId ->
                                postReturnScreen = "saved_posts"
                                selectedQuestion = questions.find { it.id == questionId }
                                    ?: savedQuestions.find { it.id == questionId }
                                            ?: userCreatedPosts.find { it.id == questionId }
                                            ?: userAnswers.find { it.question.id == questionId }?.question
                                if (selectedQuestion != null) {
                                    refreshReplies(questionId)
                                    currentScreen = "post"
                                }
                            },
                            onUpvoteClick = { questionId -> handleUpvote(questionId) },
                            onDownvoteClick = { questionId -> handleDownvote(questionId) },
                            onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                            onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                            onBackClick = { currentScreen = "profile" },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                    BottomNavDestination.SAVED -> { refreshSavedPosts(); currentScreen = "saved_posts" }
                                }
                            }
                        )
                    }

                    "add_post" -> {
                        AddPostScreen(
                            postRepository = postRepository,
                            profileName = postingName,
                            onBack = { currentScreen = "home" },
                            onPosted = {
                                refreshQuestions()
                                refreshUserPosts()
                                currentScreen = "home"
                            }
                        )
                    }
                }
            }

            OfflineSystemBanner(
                isOffline = isOffline,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
