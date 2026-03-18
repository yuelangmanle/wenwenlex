package com.yueliangmanle.danci.feature.worddetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.ai.AiWordHelpRequest
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.buildRuntimeSettings
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import kotlinx.coroutines.launch

fun wordDetailRoute(wordId: Long): String = "word_detail/$wordId"

@Composable
fun WordDetailRoute(
    wordId: Long,
    onAiMemoryClick: () -> Unit = {},
    onAiContrastClick: () -> Unit = {},
    onStartQuizClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context, wordId) {
        loadWordDetailViewModel(context, wordId)
    }
    var state by remember(viewModel) {
        mutableStateOf(viewModel.buildUiState())
    }

    WordDetailScreen(
        state = state,
        onAiMemoryClick = {
            viewModel.onAiMemoryClick()
            state = viewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = viewModel.resolveAiHelp(
                    request = AiWordHelpRequest.MNEMONIC,
                    settings = settings,
                    runtimeSettings = buildRuntimeSettings(settings, buildAiCredentialStore(context)),
                    coordinator = buildAiStrategyCoordinator(context),
                )
            }
            onAiMemoryClick()
        },
        onAiContrastClick = {
            viewModel.onAiContrastClick()
            state = viewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = viewModel.resolveAiHelp(
                    request = AiWordHelpRequest.RELATION_DIFFERENCE,
                    settings = settings,
                    runtimeSettings = buildRuntimeSettings(settings, buildAiCredentialStore(context)),
                    coordinator = buildAiStrategyCoordinator(context),
                )
            }
            onAiContrastClick()
        },
        onExplainWordFormsClick = {
            viewModel.onExplainWordFormsClick()
            state = viewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = viewModel.resolveAiHelp(
                    request = AiWordHelpRequest.WORD_FORM_EXPLANATION,
                    settings = settings,
                    runtimeSettings = buildRuntimeSettings(settings, buildAiCredentialStore(context)),
                    coordinator = buildAiStrategyCoordinator(context),
                )
            }
        },
        onExpandExampleClick = {
            viewModel.onExpandExampleClick()
            state = viewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = viewModel.resolveAiHelp(
                    request = AiWordHelpRequest.EXAMPLE_EXPANSION,
                    settings = settings,
                    runtimeSettings = buildRuntimeSettings(settings, buildAiCredentialStore(context)),
                    coordinator = buildAiStrategyCoordinator(context),
                )
            }
        },
        onStartQuizClick = {
            viewModel.onStartQuizClick()
            onStartQuizClick()
        },
    )
}
