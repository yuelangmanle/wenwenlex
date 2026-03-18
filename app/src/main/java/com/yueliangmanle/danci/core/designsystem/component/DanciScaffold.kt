package com.yueliangmanle.danci.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yueliangmanle.danci.app.TopLevelDestination

@Composable
fun DanciScaffold(
    destinations: List<TopLevelDestination>,
    isDestinationSelected: (TopLevelDestination) -> Boolean,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = isDestinationSelected(destination),
                        onClick = { onDestinationSelected(destination) },
                        icon = { SpacerIcon },
                        label = { Text(text = destination.label) },
                    )
                }
            }
        },
        content = content,
    )
}

private val SpacerIcon: @Composable () -> Unit = {}
