package com.yueliangmanle.danci.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.yueliangmanle.danci.feature.books.BooksRoute
import com.yueliangmanle.danci.feature.home.HomeRoute
import com.yueliangmanle.danci.feature.me.AiSettingsRoute
import com.yueliangmanle.danci.feature.quiz.QuizRoute
import com.yueliangmanle.danci.feature.quiz.quizRoute
import com.yueliangmanle.danci.feature.study.StudyRoute
import com.yueliangmanle.danci.feature.worddetail.WordDetailRoute
import com.yueliangmanle.danci.feature.worddetail.wordDetailRoute

@Composable
fun DanciNavHost(
    modifier: Modifier = Modifier,
    startDestination: TopLevelDestination,
    navController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination.name,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            composable(route = destination.name) {
                when (destination) {
                    TopLevelDestination.HOME -> HomeRoute(
                        onStartNewWordsClick = {
                            navController.navigate(TopLevelDestination.STUDY.name)
                        },
                        onStartReviewClick = {
                            navController.navigate(TopLevelDestination.STUDY.name)
                        },
                        onOpenMistakesClick = {
                            navController.navigate(TopLevelDestination.STUDY.name)
                        },
                    )
                    TopLevelDestination.STUDY -> StudyRoute(
                        onOpenDetailClick = { wordId ->
                            navController.navigate(wordDetailRoute(wordId))
                        },
                    )
                    TopLevelDestination.BOOKS -> BooksRoute()
                    TopLevelDestination.ME -> AiSettingsRoute()
                }
            }
        }
        composable(
            route = "word_detail/{wordId}",
            arguments = listOf(
                navArgument("wordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getLong("wordId") ?: 1L
            WordDetailRoute(
                wordId = wordId,
                onStartQuizClick = {
                    navController.navigate(quizRoute(wordId))
                },
            )
        }
        composable(
            route = "quiz/{wordId}",
            arguments = listOf(
                navArgument("wordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getLong("wordId") ?: 1L
            QuizRoute(
                wordId = wordId,
                onOpenDetailClick = {
                    navController.navigate(wordDetailRoute(wordId))
                },
            )
        }
    }
}
