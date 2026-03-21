package com.yueliangmanle.danci.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.yueliangmanle.danci.feature.aiplan.AiPlanCenterRoute
import com.yueliangmanle.danci.feature.aiplan.AI_PLAN_CENTER_ROUTE
import com.yueliangmanle.danci.feature.aiplan.PlanComparisonRoute
import com.yueliangmanle.danci.feature.aiplan.PlanExplanationRoute
import com.yueliangmanle.danci.feature.aiplan.planComparisonRoute
import com.yueliangmanle.danci.feature.aiplan.planExplanationRoute
import com.yueliangmanle.danci.feature.books.BOOK_IMPORT_ROUTE
import com.yueliangmanle.danci.feature.books.BookDetailRoute
import com.yueliangmanle.danci.feature.books.BookImportRoute
import com.yueliangmanle.danci.feature.books.BooksRoute
import com.yueliangmanle.danci.feature.books.bookDetailRoute
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
                            navController.navigate(TopLevelDestination.STUDY.name)
                        },
                        onStartReviewClick = {
                            navController.navigate(TopLevelDestination.STUDY.name)
                        },
                        onOpenMistakesClick = {
                            navController.navigate(TopLevelDestination.STUDY.name)
                        },
                        onOpenPlanCenterClick = {
                            navController.navigate(AI_PLAN_CENTER_ROUTE)
                        },
                    )
                    TopLevelDestination.STUDY -> StudyRoute(
                        onOpenDetailClick = { wordId ->
                            navController.navigate(wordDetailRoute(wordId))
                        },
                        onOpenPlanCenterClick = {
                            navController.navigate(AI_PLAN_CENTER_ROUTE)
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
                        onOpenAiPlanCenterClick = {
                            navController.navigate(AI_PLAN_CENTER_ROUTE)
                        },
                        onOpenPronunciationSettingsClick = {
                            navController.navigate(PRONUNCIATION_SETTINGS_ROUTE)
                        },
                    )
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
        composable(route = AI_SETTINGS_ROUTE) {
            AiSettingsRoute()
        }
        composable(route = PRONUNCIATION_SETTINGS_ROUTE) {
            PronunciationSettingsRoute()
        }
        composable(route = AI_PLAN_CENTER_ROUTE) {
            AiPlanCenterRoute(
                onOpenComparisonClick = { planVersionId ->
                    navController.navigate(planComparisonRoute(planVersionId))
                },
                onOpenExplanationClick = { planVersionId ->
                    navController.navigate(planExplanationRoute(planVersionId))
                },
            )
        }
        composable(route = BOOK_IMPORT_ROUTE) {
            BookImportRoute(
                onImportedBook = { bookId ->
                    navController.navigate(bookDetailRoute(bookId))
                },
            )
        }
        composable(
            route = "plan_comparison/{planVersionId}",
            arguments = listOf(
                navArgument("planVersionId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val planVersionId = backStackEntry.arguments?.getLong("planVersionId") ?: 0L
            PlanComparisonRoute(planVersionId = planVersionId)
        }
        composable(
            route = "plan_explanation/{planVersionId}",
            arguments = listOf(
                navArgument("planVersionId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val planVersionId = backStackEntry.arguments?.getLong("planVersionId") ?: 0L
            PlanExplanationRoute(planVersionId = planVersionId)
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
