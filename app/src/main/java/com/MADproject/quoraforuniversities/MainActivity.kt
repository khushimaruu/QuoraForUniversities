package com.MADproject.quoraforuniversities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme

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
fun CampusQnAApp() {

    var currentScreen by remember {
        mutableStateOf("home")
    }

    var selectedQuestion by remember {
        mutableStateOf<QuestionUiModel?>(null)
    }

    // ---------------------------------------------------------
    // SAMPLE QUESTIONS
    // ---------------------------------------------------------

    val questions = listOf(

        QuestionUiModel(
            id = "1",
            title = "How do I add a backlog course to my timetable?",
            body = "I failed one course last semester and need to retake it alongside my current courses. Not sure how registration handles this.",
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
            body = "Looking for a place with decent wifi and enough seating for 4-5 people, preferably open till late.",
            authorName = "Riya Sharma",
            isAnonymous = false,
            tags = listOf(
                "CampusLife"
            ),
            voteCount = 27,
            answerCount = 9
        )
    )

    // ---------------------------------------------------------
    // SAMPLE REPLIES
    // ---------------------------------------------------------

    val replies = listOf(

        ReplyUiModel(
            id = "1",
            authorName = "Aarav",
            body = "You can usually add the backlog course during the registration period.",
            voteCount = 0
        ),

        ReplyUiModel(
            id = "2",
            authorName = "Anonymous",
            body = "I had the same issue last semester. Check with your department office.",
            voteCount = 0
        )
    )

    // ---------------------------------------------------------
    // SCREEN NAVIGATION
    // ---------------------------------------------------------

    when (currentScreen) {

        // =====================================================
        // HOME
        // =====================================================

        "home" -> {

            HomeScreen(

                questions = questions,

                onQuestionClick = { questionId ->

                    selectedQuestion = questions.find { question ->
                        question.id == questionId
                    }

                    currentScreen = "post"
                },

                onUpvoteClick = {
                    // Add upvote logic later
                },

                onNavDestinationSelected = { destination ->

                    when (destination) {

                        BottomNavDestination.HOME -> {
                            currentScreen = "home"
                        }

                        BottomNavDestination.PROFILE -> {
                            currentScreen = "profile"
                        }

                        else -> {
                            // Other destinations can be added later
                        }
                    }
                }
            )
        }

        // =====================================================
        // POST DETAIL
        // =====================================================

        "post" -> {

            selectedQuestion?.let { question ->

                PostDetailPage(

                    question = question,

                    replies = replies,

                    onBackClick = {
                        currentScreen = "home"
                    },

                    onUpvoteClick = {
                        // Add upvote logic later
                    },

                    onSubmitReply = {
                        // Add reply logic later
                    }
                )
            }
        }

        // =====================================================
        // PROFILE
        // =====================================================

        "profile" -> {

            ProfileScreen(

                onBackClick = {
                    currentScreen = "home"
                },

                onNavDestinationSelected = {

                    val it = null
                    when (it) {

                        BottomNavDestination.HOME -> {
                            currentScreen = "home"
                        }

                        BottomNavDestination.PROFILE -> {
                            currentScreen = "profile"
                        }

                        else -> {
                            // Other destinations can be added later
                        }
                    }
                }
            )
        }
    }
}