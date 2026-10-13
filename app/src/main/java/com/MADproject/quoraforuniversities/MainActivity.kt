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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    authRepository: AuthRepository = remember { AuthRepository(context) },
    postRepository: PostRepository = remember { PostRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() }
) {
    val scope = rememberCoroutineScope()
    val networkObserver = remember { NetworkConnectivityObserver(context) }
    val connectivityStatus by networkObserver.observe().collectAsState(initial = ConnectivityStatus.Available)
    val isOffline = connectivityStatus == ConnectivityStatus.Lost || connectivityStatus == ConnectivityStatus.Unavailable

    var currentScreen by rememberSaveable {
        mutableStateOf("splash")
    }

    var selectedQuestion by remember {
        mutableStateOf<QuestionUiModel?>(null)
    }

    var questions by remember {
        mutableStateOf<List<QuestionUiModel>>(emptyList())
    }

    var isFeedLoading by remember {
        mutableStateOf(false)
    }

    var replies by remember {
        mutableStateOf<List<ReplyUiModel>>(emptyList())
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

    LaunchedEffect(Unit) {
        refreshQuestions()
        delay(1200)
        currentScreen = if (authRepository.isLoggedIn()) "home" else "landing"
    }

    // Intercept system back button presses to return to home / landing
    if (currentScreen != "splash" && currentScreen != "home" && currentScreen != "landing") {
        BackHandler {
            currentScreen = if (authRepository.isLoggedIn()) "home" else "landing"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { width -> width / 12 } togetherWith
                fadeOut(animationSpec = tween(180))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {

        "splash" -> {
            SplashScreen()
        }

        "landing" -> {
            LandingPage(
                onLoginClick = {
                    currentScreen = "login"
                },
                onSignUpClick = {
                    currentScreen = "signup"
                }
            )
        }

        "login" -> {
            LoginPage(
                authRepository = authRepository,
                onBackClick = {
                    currentScreen = "landing"
                },
                onLoginSuccess = {
                    refreshQuestions()
                    currentScreen = "home"
                },
                onNavigateToSignUp = {
                    currentScreen = "signup"
                }
            )
        }

        "signup" -> {
            SignUpPage(
                authRepository = authRepository,
                onBackClick = {
                    currentScreen = "landing"
                },
                onSignUpSuccess = {
                    refreshQuestions()
                    currentScreen = "home"
                },
                onNavigateToLogin = {
                    currentScreen = "login"
                }
            )
        }

        "home" -> {
            HomeScreen(
                questions = questions,
                isLoading = isFeedLoading,
                onQuestionClick = { questionId ->
                    selectedQuestion = questions.find { it.id == questionId }
                    refreshReplies(questionId)
                    currentScreen = "post"
                },
                onUpvoteClick = { questionId ->
                    scope.launch {
                        postRepository.votePost(questionId)
                        refreshQuestions()
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

        "search" -> {
            SearchPage(
                questions = questions,
                onQuestionClick = { questionId ->
                    selectedQuestion = questions.find { it.id == questionId }
                    refreshReplies(questionId)
                    currentScreen = "post"
                },
                onUpvoteClick = { questionId ->
                    scope.launch {
                        postRepository.votePost(questionId)
                        refreshQuestions()
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

        "post" -> {
            selectedQuestion?.let { question ->
                PostDetailPage(
                    question = question,
                    replies = replies,
                    onBackClick = {
                        currentScreen = "home"
                    },
                    onUpvoteClick = {
                        scope.launch {
                            postRepository.votePost(question.id)
                            refreshQuestions()
                        }
                    },
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
                onLogOutClick = {
                    currentScreen = "landing"
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

        "add_post" -> {
            AddPostScreen(
                postRepository = postRepository,
                onBack = {
                    currentScreen = "home"
                },
                onPosted = {
                    refreshQuestions()
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
