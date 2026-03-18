package com.yueliangmanle.danci.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yueliangmanle.danci.feature.me.AI_SETTINGS_ROUTE

@Stable
class DanciAppState(
    val navController: NavHostController,
) {
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    @Composable
    fun currentDestination(): NavDestination? {
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        return navBackStackEntry?.destination
    }

    fun navigateToTopLevelDestination(destination: TopLevelDestination) {
        navController.navigate(destination.name) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun isTopLevelDestinationSelected(
        destination: TopLevelDestination,
        currentDestination: NavDestination?,
    ): Boolean {
        val currentRoute = currentDestination?.route ?: return false
        return when (destination) {
            TopLevelDestination.HOME -> currentRoute == TopLevelDestination.HOME.name
            TopLevelDestination.STUDY -> {
                currentDestination.hierarchy.any { it.route == TopLevelDestination.STUDY.name } ||
                    currentRoute.startsWith("word_detail") ||
                    currentRoute.startsWith("quiz")
            }
            TopLevelDestination.BOOKS -> currentRoute == TopLevelDestination.BOOKS.name
            TopLevelDestination.ME -> {
                currentDestination.hierarchy.any { it.route == TopLevelDestination.ME.name } ||
                    currentRoute == AI_SETTINGS_ROUTE
            }
        }
    }
}

@Composable
fun rememberDanciAppState(
    navController: NavHostController = rememberNavController(),
): DanciAppState = remember(navController) {
    DanciAppState(navController = navController)
}
