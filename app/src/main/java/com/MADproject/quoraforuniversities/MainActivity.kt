package com.MADproject.quoraforuniversities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.data.AuthRepository
import com.MADproject.quoraforuniversities.data.PostRepository
import com.MADproject.quoraforuniversities.data.ProfileRepository
import com.MADproject.quoraforuniversities.ui.addpost.AddPostScreen
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme
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
    authRepository: AuthRepository = remember { AuthRepository() },
    postRepository: PostRepository = remember { PostRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() }
) {
    val scope = rememberCoroutineScope()

    var currentScreen by rememberSaveable {
        mutableStateOf(if (authRepository.isLoggedIn()) "home" else "landing")
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
            isFeedLoading = true
            val result = postRepository.getPosts()
            isFeedLoading = false
            if (result.isSuccess) {
                questions = result.getOrDefault(emptyList())
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
    }

    // Intercept system back button presses to return to home / landing
    if (currentScreen != "home" && currentScreen != "landing") {
        BackHandler {
            currentScreen = if (authRepository.isLoggedIn()) "home" else "landing"
        }
    }

    when (currentScreen) {

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
                    onSubmitReply = { text ->
                        scope.launch {
                            postRepository.createReply(question.id, text)
                            refreshReplies(question.id)
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
