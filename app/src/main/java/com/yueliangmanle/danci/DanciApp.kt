package com.yueliangmanle.danci

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.yueliangmanle.danci.app.DanciNavHost
import com.yueliangmanle.danci.app.TopLevelDestination
import com.yueliangmanle.danci.app.rememberDanciAppState
import com.yueliangmanle.danci.core.designsystem.component.DanciScaffold
import com.yueliangmanle.danci.core.designsystem.theme.DanciTheme

@Composable
fun DanciApp() {
    val appState = rememberDanciAppState()
    val destinations = TopLevelDestination.entries
    val currentDestination = appState.currentDestination()
    DanciTheme {
        DanciScaffold(
            destinations = destinations,
            currentDestinationRoute = currentDestination?.route,
            onDestinationSelected = appState::navigateToTopLevelDestination,
        ) { paddingValues ->
            DanciNavHost(
                modifier = Modifier.padding(paddingValues),
                startDestination = TopLevelDestination.HOME,
                navController = appState.navController,
            )
        }
    }
}
