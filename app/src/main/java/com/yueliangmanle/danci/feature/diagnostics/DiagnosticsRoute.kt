package com.yueliangmanle.danci.feature.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.diagnostics.buildDiagnosticsRepository
import kotlinx.coroutines.launch

const val DIAGNOSTICS_ROUTE = "diagnostics"

@Composable
fun DiagnosticsRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context) {
        DiagnosticsViewModel(
            diagnosticsRepository = buildDiagnosticsRepository(context),
        )
    }
    var state by remember {
        mutableStateOf(DiagnosticsUiState(isLoading = true))
    }

    fun launchAction(action: suspend DiagnosticsViewModel.() -> DiagnosticsUiState) {
        scope.launch {
            state = state.copy(isWorking = true, statusMessage = null)
            state = runCatching { viewModel.action() }.getOrElse { error ->
                state.copy(
                    isWorking = false,
                    statusMessage = error.message ?: "诊断页加载失败，请稍后重试。",
                )
            }
        }
    }

    LaunchedEffect(viewModel) {
        state = viewModel.loadUiState()
    }

    DiagnosticsScreen(
        state = state,
        onRefreshClick = {
            launchAction { loadUiState() }
        },
        onExportClick = {
            launchAction { exportDiagnostics() }
        },
    )
}
