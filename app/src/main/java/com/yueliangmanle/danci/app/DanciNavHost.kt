package com.yueliangmanle.danci.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.yueliangmanle.danci.feature.books.BOOK_IMPORT_ROUTE
import com.yueliangmanle.danci.feature.books.BookDetailRoute
import com.yueliangmanle.danci.feature.books.BookImportRoute
import com.yueliangmanle.danci.feature.books.BooksRoute
import com.yueliangmanle.danci.feature.books.bookDetailRoute
import com.yueliangmanle.danci.core.study.StudyLaunchMode
import com.yueliangmanle.danci.core.study.parseStudyLaunchMode
import com.yueliangmanle.danci.core.study.studyRoute
import com.yueliangmanle.danci.core.study.studyRoutePattern
import com.yueliangmanle.danci.feature.home.HomeRoute
import com.yueliangmanle.danci.feature.me.AiSettingsRoute
import com.yueliangmanle.danci.feature.me.AI_SETTINGS_ROUTE
import com.yueliangmanle.danci.feature.me.MeRoute
import com.yueliangmanle.danci.feature.pronunciation.PRONUNCIATION_SETTINGS_ROUTE
import com.yueliangmanle.danci.feature.pronunciation.PronunciationSettingsRoute
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
                            navController.navigate(studyRoute(StudyLaunchMode.NEW_WORDS))
                        },
                        onStartReviewClick = {
                            navController.navigate(studyRoute(StudyLaunchMode.REVIEW))
                        },
                        onOpenMistakesClick = {
                            navController.navigate(studyRoute(StudyLaunchMode.RECENT_MISTAKES))
                        },
                    )
                    TopLevelDestination.STUDY -> StudyRoute(
                        onOpenDetailClick = { wordId ->
                            navController.navigate(wordDetailRoute(wordId))
                        },
                        onBackHomeClick = {
                            navController.navigate(TopLevelDestination.HOME.name)
                        },
                    )
                    TopLevelDestination.BOOKS -> BooksRoute(
                        onImportClick = {
                            navController.navigate(BOOK_IMPORT_ROUTE)
                        },
                        onBookClick = { bookId ->
                            navController.navigate(bookDetailRoute(bookId))
                        },
                    )
                    TopLevelDestination.ME -> MeRoute(
                        onOpenAiSettingsClick = {
                            navController.navigate(AI_SETTINGS_ROUTE)
                        },
                        onOpenPronunciationSettingsClick = {
                            navController.navigate(PRONUNCIATION_SETTINGS_ROUTE)
                        },
                    )
                }
            }
        }
        composable(
            route = studyRoutePattern(),
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    nullable = false
                },
            ),
        ) { backStackEntry ->
            val launchMode = parseStudyLaunchMode(backStackEntry.arguments?.getString("mode"))
            StudyRoute(
                launchMode = launchMode,
                onOpenDetailClick = { wordId ->
                    navController.navigate(wordDetailRoute(wordId))
                },
                onBackHomeClick = {
                    navController.navigate(TopLevelDestination.HOME.name)
                },
            )
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
        composable(route = AI_SETTINGS_ROUTE) {
            AiSettingsRoute()
        }
        composable(route = PRONUNCIATION_SETTINGS_ROUTE) {
            PronunciationSettingsRoute()
        }
        composable(route = BOOK_IMPORT_ROUTE) {
            BookImportRoute(
                onImportedBook = { bookId ->
                    navController.navigate(bookDetailRoute(bookId))
                },
            )
        }
        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
            BookDetailRoute(
                bookId = bookId,
                onWordClick = { wordId ->
                    navController.navigate(wordDetailRoute(wordId))
                },
            )
        }
    }
}
