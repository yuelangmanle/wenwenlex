package com.yueliangmanle.danci.feature.worddetail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.ai.AiWordHelpRequest
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.pronunciation.buildPronunciationOrchestrator
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
    val coordinator = remember(context) { buildAiStrategyCoordinator(context) }
    val pronunciationOrchestrator = remember(context) { buildPronunciationOrchestrator(context) }
    var viewModel: WordDetailViewModel? by remember(context, wordId) {
        mutableStateOf(null)
    }
    var state by remember(wordId) {
        mutableStateOf(WordDetailUiState())
    }
    var showOverwriteDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(context, wordId) {
        val loaded = loadWordDetailViewModel(context, wordId)
        viewModel = loaded
        state = loaded.refreshPronunciationSourceState()
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("覆盖现有音标？") },
            text = { Text("会保留本地释义等内容，只重拉这条单词的音标结果。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverwriteDialog = false
                        val currentViewModel = viewModel ?: return@TextButton
                        scope.launch {
                            state = currentViewModel.markPhoneticLoading()
                            state = currentViewModel.fillPhonetic(
                                overwrite = true,
                                coordinator = coordinator,
                            )
                        }
                    },
                ) {
                    Text("继续覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    WordDetailScreen(
        state = state,
        onPlayUkPronunciationClick = {
            viewModel ?: return@WordDetailScreen
            scope.launch {
                val result = pronunciationOrchestrator.playWordById(
                    wordId = state.wordId,
                    accentOverride = PronunciationAccent.UK,
                    contextLabel = "word_detail",
                )
                state = state.copy(
                    statusMessage = result.statusMessage,
                    errorMessage = result.errorMessage,
                )
            }
        },
        onPlayUsPronunciationClick = {
            viewModel ?: return@WordDetailScreen
            scope.launch {
                val result = pronunciationOrchestrator.playWordById(
                    wordId = state.wordId,
                    accentOverride = PronunciationAccent.US,
                    contextLabel = "word_detail",
                )
                state = state.copy(
                    statusMessage = result.statusMessage,
                    errorMessage = result.errorMessage,
                )
            }
        },
        onAiMemoryClick = {
            val currentViewModel = viewModel ?: return@WordDetailScreen
            currentViewModel.onAiMemoryClick()
            state = currentViewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = currentViewModel.resolveAiHelp(
                    request = AiWordHelpRequest.MNEMONIC,
                    settings = settings,
                    runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.WORD_HELP),
                    coordinator = coordinator,
                )
            }
            onAiMemoryClick()
        },
        onAiContrastClick = {
            val currentViewModel = viewModel ?: return@WordDetailScreen
            currentViewModel.onAiContrastClick()
            state = currentViewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = currentViewModel.resolveAiHelp(
                    request = AiWordHelpRequest.RELATION_DIFFERENCE,
                    settings = settings,
                    runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.WORD_HELP),
                    coordinator = coordinator,
                )
            }
            onAiContrastClick()
        },
        onExplainWordFormsClick = {
            val currentViewModel = viewModel ?: return@WordDetailScreen
            currentViewModel.onExplainWordFormsClick()
            state = currentViewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = currentViewModel.resolveAiHelp(
                    request = AiWordHelpRequest.WORD_FORM_EXPLANATION,
                    settings = settings,
                    runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.WORD_HELP),
                    coordinator = coordinator,
                )
            }
        },
        onExpandExampleClick = {
            val currentViewModel = viewModel ?: return@WordDetailScreen
            currentViewModel.onExpandExampleClick()
            state = currentViewModel.markAiLoading()
            scope.launch {
                val settings = buildSettingsRepository(context).getSettings()
                state = currentViewModel.resolveAiHelp(
                    request = AiWordHelpRequest.EXAMPLE_EXPANSION,
                    settings = settings,
                    runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.WORD_HELP),
                    coordinator = coordinator,
                )
            }
        },
        onFillMissingPhoneticClick = {
            val currentViewModel = viewModel ?: return@WordDetailScreen
            scope.launch {
                state = currentViewModel.markPhoneticLoading()
                state = currentViewModel.fillPhonetic(
                    overwrite = false,
                    coordinator = coordinator,
                )
            }
        },
        onOverwritePhoneticClick = {
            showOverwriteDialog = true
        },
        onSwitchPronunciationSource = { sourceId ->
            val currentViewModel = viewModel ?: return@WordDetailScreen
            scope.launch {
                state = currentViewModel.switchSessionPronunciationSource(sourceId)
            }
        },
        onStartQuizClick = {
            viewModel?.onStartQuizClick()
            onStartQuizClick()
        },
    )
}
