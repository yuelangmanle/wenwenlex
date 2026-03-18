package com.yueliangmanle.danci

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.app.DanciNavHost
import com.yueliangmanle.danci.app.TopLevelDestination
import com.yueliangmanle.danci.app.rememberDanciAppState
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.designsystem.component.DanciScaffold
import com.yueliangmanle.danci.core.designsystem.theme.DanciTheme
import com.yueliangmanle.danci.core.worker.AiSummaryRefreshScheduler
import com.yueliangmanle.danci.core.worker.DailyReminderScheduler

@Composable
fun DanciApp() {
    val appState = rememberDanciAppState()
    val destinations = TopLevelDestination.entries
    val currentDestination = appState.currentDestination()
    val context = LocalContext.current

    LaunchedEffect(context) {
        val settings = buildSettingsRepository(context).getSettings()
        DailyReminderScheduler(context).sync(settings)
        AiSummaryRefreshScheduler(context).schedule()
    }

    DanciTheme {
        DanciScaffold(
            destinations = destinations,
            isDestinationSelected = { destination ->
                appState.isTopLevelDestinationSelected(destination, currentDestination)
            },
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
