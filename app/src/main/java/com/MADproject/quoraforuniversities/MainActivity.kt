package com.MADproject.quoraforuniversities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.MADproject.quoraforuniversities.components.BottomNavDestination
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import com.MADproject.quoraforuniversities.ui.addpost.AddPostScreen
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
    val navController = rememberNavController()

    // ---------------------------------------------------------
    // SAMPLE DATA
    // ---------------------------------------------------------
    val questions = remember {
        listOf(
            QuestionUiModel(
                id = "1",
                title = "How do I add a backlog course to my timetable?",
                body = "I failed one course last semester and need to retake it alongside my current courses. Not sure how registration handles this.",
                authorName = "Anonymous",
                isAnonymous = true,
                tags = listOf("CSE", "Year2", "Registration"),
                voteCount = 12,
                answerCount = 4
            ),
            QuestionUiModel(
                id = "2",
                title = "Best cafes near the north campus for group study?",
                body = "Looking for a place with decent wifi and enough seating for 4-5 people, preferably open till late.",
                authorName = "Riya Sharma",
                isAnonymous = false,
                tags = listOf("CampusLife"),
                voteCount = 27,
                answerCount = 9
            )
        )
    }

    val replies = remember {
        listOf(
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
    }

    // ---------------------------------------------------------
    // NAVIGATION HOST
    // ---------------------------------------------------------
    NavHost(
        navController = navController,
        startDestination = "landing"
    ) {
        composable("landing") {
            LandingPage(
                onLoginClick = { navController.navigate(BottomNavDestination.HOME.route) },
                onSignUpClick = { navController.navigate(BottomNavDestination.HOME.route) }
            )
        }

        composable(BottomNavDestination.HOME.route) {
            HomeScreen(
                questions = questions,
                onQuestionClick = { id -> navController.navigate("post_detail/$id") },
                onNavDestinationSelected = { dest ->
                    if (dest.route != BottomNavDestination.HOME.route) {
                        navController.navigate(dest.route)
                    }
                }
            )
        }

        composable(BottomNavDestination.SEARCH.route) {
            SearchPage(
                questions = questions,
                onQuestionClick = { id -> navController.navigate("post_detail/$id") },
                onNavDestinationSelected = { dest ->
                    if (dest.route != BottomNavDestination.SEARCH.route) {
                        navController.navigate(dest.route)
                    }
                }
            )
        }

        composable(BottomNavDestination.PROFILE.route) {
            ProfileScreen(
                onNavDestinationSelected = { dest ->
                    if (dest.route != BottomNavDestination.PROFILE.route) {
                        navController.navigate(dest.route)
                    }
                }
            )
        }

        composable(BottomNavDestination.ADD_POST.route) {
            AddPostScreen(
                onBack = { navController.popBackStack() },
                onPosted = { navController.popBackStack() }
            )
        }

        composable(
            route = "post_detail/{questionId}",
            arguments = listOf(navArgument("questionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId")
            val question = questions.find { it.id == questionId }

            if (question != null) {
                PostDetailPage(
                    question = question,
                    replies = replies,
                    onBackClick = { navController.popBackStack() },
                    onNavDestinationSelected = { dest ->
                        navController.navigate(dest.route)
                    }
                )
            }
        }
    }
}
