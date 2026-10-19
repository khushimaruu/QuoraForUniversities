package com.MADproject.quoraforuniversities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import com.MADproject.quoraforuniversities.data.ProfileRepository
import com.MADproject.quoraforuniversities.ui.addpost.AddPostScreen
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
import com.MADproject.quoraforuniversities.utils.ConnectivityStatus
import com.MADproject.quoraforuniversities.utils.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    var selectedQuestion by remember { mutableStateOf<QuestionUiModel?>(null) }
    var questions by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isFeedLoading by remember { mutableStateOf(false) }
    var replies by remember { mutableStateOf<List<ReplyUiModel>>(emptyList()) }
    var savedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var savedQuestions by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isSavedLoading by remember { mutableStateOf(false) }

    var userCreatedPosts by remember { mutableStateOf<List<QuestionUiModel>>(emptyList()) }
    var isUserPostsLoading by remember { mutableStateOf(false) }

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
            isUserPostsLoading = true
            val res = profileRepository.getUserPosts(uid)
            isUserPostsLoading = false
            if (res.isSuccess) {
                userCreatedPosts = res.getOrDefault(emptyList())
            }
        }
    }

    fun toggleBookmark(postId: String) {
        scope.launch {
            if (savedPostIds.contains(postId)) {
                postRepository.unbookmarkPost(postId)
            } else {
                postRepository.bookmarkPost(postId)
            }
            refreshSavedPosts()
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
                selectedQuestion?.let { currentSq ->
                    val updatedSq = updatedList.find { it.id == currentSq.id }
                    if (updatedSq != null) {
                        selectedQuestion = updatedSq
                    }
                }
            }
        }
    }

    fun refreshReplies(postId: String) {
        scope.launch {
            val result = postRepository.getReplies(postId)
            if (result.isSuccess) {
                replies = result.getOrDefault(emptyList())
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
            currentScreen = if (authRepository.isLoggedIn()) "home" else "landing"
        }
    }

    Scaffold(
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

                    "home" -> {
                        HomeScreen(
                            questions = questions,
                            isLoading = isFeedLoading,
                            savedPostIds = savedPostIds,
                            onQuestionClick = { questionId ->
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
                                }
                            }
                        )
                    }

                    "search" -> {
                        SearchPage(
                            questions = questions,
                            savedPostIds = savedPostIds,
                            onQuestionClick = { questionId ->
                                selectedQuestion = questions.find { it.id == questionId }
                                refreshReplies(questionId)
                                currentScreen = "post"
                            },
                            onUpvoteClick = { questionId -> handleUpvote(questionId) },
                            onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                            onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                }
                            }
                        )
                    }

                    "post" -> {
                        selectedQuestion?.let { question ->
                            PostDetailPage(
                                question = question,
                                replies = replies,
                                isBookmarked = savedPostIds.contains(question.id),
                                onBackClick = { currentScreen = "home" },
                                onUpvoteClick = { handleUpvote(question.id) },
                                onDownvoteClick = { handleDownvote(question.id) },
                                onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                                onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                                onSubmitReply = { text, onResult ->
                                    scope.launch {
                                        val result = postRepository.createReply(question.id, text)
                                        if (result.isSuccess) {
                                            refreshReplies(question.id)
                                            refreshQuestions()
                                            onResult(null)
                                        } else {
                                            onResult(
                                                result.exceptionOrNull()?.localizedMessage
                                                    ?: "Failed to post answer to database."
                                            )
                                        }
                                    }
                                },
                                onNavDestinationSelected = { destination ->
                                    when (destination) {
                                        BottomNavDestination.HOME -> currentScreen = "home"
                                        BottomNavDestination.SEARCH -> currentScreen = "search"
                                        BottomNavDestination.PROFILE -> currentScreen = "profile"
                                        BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                    }
                                }
                            )
                        }
                    }

                    "profile" -> {
                        ProfileScreen(
                            authRepository = authRepository,
                            profileRepository = profileRepository,
                            savedCount = savedPostIds.size,
                            savedPostIds = savedPostIds,
                            userPosts = userCreatedPosts,
                            isLoadingPosts = isUserPostsLoading,
                            onQuestionClick = { questionId ->
                                selectedQuestion = questions.find { it.id == questionId }
                                    ?: savedQuestions.find { it.id == questionId }
                                            ?: userCreatedPosts.find { it.id == questionId }
                                if (selectedQuestion != null) {
                                    refreshReplies(questionId)
                                    currentScreen = "post"
                                }
                            },
                            onUpvoteClick = { handleUpvote(it) },
                            onDownvoteClick = { handleDownvote(it) },
                            onBookmarkClick = { toggleBookmark(it) },
                            onReportClick = { id, reason -> handleReportPost(id, reason) },
                            onSavedPostsClick = {
                                refreshSavedPosts()
                                currentScreen = "saved_posts"
                            },
                            onLogOutClick = { currentScreen = "landing" },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
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
                                selectedQuestion = questions.find { it.id == questionId }
                                    ?: savedQuestions.find { it.id == questionId }
                                            ?: userCreatedPosts.find { it.id == questionId }
                                if (selectedQuestion != null) {
                                    refreshReplies(questionId)
                                    currentScreen = "post"
                                }
                            },
                            onUpvoteClick = { questionId -> handleUpvote(questionId) },
                            onBookmarkClick = { questionId -> toggleBookmark(questionId) },
                            onReportClick = { postId, reason -> handleReportPost(postId, reason) },
                            onBackClick = { currentScreen = "profile" },
                            onNavDestinationSelected = { destination ->
                                when (destination) {
                                    BottomNavDestination.HOME -> currentScreen = "home"
                                    BottomNavDestination.SEARCH -> currentScreen = "search"
                                    BottomNavDestination.PROFILE -> currentScreen = "profile"
                                    BottomNavDestination.ADD_POST -> currentScreen = "add_post"
                                }
                            }
                        )
                    }

                    "add_post" -> {
                        AddPostScreen(
                            postRepository = postRepository,
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