package com.yueliangmanle.danci.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yueliangmanle.danci.feature.books.BooksRoute
import com.yueliangmanle.danci.feature.home.HomeRoute

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
                    TopLevelDestination.HOME -> HomeRoute()
                    TopLevelDestination.BOOKS -> BooksRoute()
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = destination.screenTitle)
                    }
                }
            }
        }
    }
}
