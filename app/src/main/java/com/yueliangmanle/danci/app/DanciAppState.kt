package com.yueliangmanle.danci.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

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
    ): Boolean = currentDestination?.hierarchy?.any { it.route == destination.name } == true
}

@Composable
fun rememberDanciAppState(
    navController: NavHostController = rememberNavController(),
): DanciAppState = DanciAppState(navController = navController)
