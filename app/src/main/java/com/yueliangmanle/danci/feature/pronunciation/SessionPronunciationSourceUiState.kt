package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType

data class SessionPronunciationSourceUiState(
    val id: String? = null,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
)

internal fun buildSessionPronunciationSourceOptions(
    sources: List<PronunciationSource>,
    selectedSourceId: String?,
): List<SessionPronunciationSourceUiState> =
    buildList {
        add(
            SessionPronunciationSourceUiState(
                id = null,
                title = "跟随默认来源",
                subtitle = "使用当前全局默认的单词发音来源",
                isSelected = selectedSourceId == null,
            ),
        )
        sources
            .asSequence()
            .filter(PronunciationSource::enabled)
            .sortedWith(
                compareByDescending<PronunciationSource> { it.isDefaultForWord }
                    .thenBy { it.sortOrder }
                    .thenBy { it.id },
            )
            .map { source ->
                SessionPronunciationSourceUiState(
                    id = source.id,
                    title = source.name,
                    subtitle = buildSessionSourceSubtitle(source),
                    isSelected = source.id == selectedSourceId,
                )
            }
            .forEach(::add)
    }

private fun buildSessionSourceSubtitle(
    source: PronunciationSource,
): String {
    val accentLabel = PronunciationAccent.fromStorageValue(source.accent).label
    val typeLabel = when (PronunciationSourceType.fromStorageValue(source.sourceType)) {
        PronunciationSourceType.DICTIONARY -> "词典音频"
        PronunciationSourceType.LOCAL_NATIVE -> "本地原生"
        PronunciationSourceType.LOCAL_BRIDGE -> "本地桥接"
        PronunciationSourceType.CLOUD_TTS -> "云端 TTS"
        null -> "未知来源"
    }
    return buildList {
        add(typeLabel)
        add(accentLabel)
        if (source.isDefaultForWord) {
            add("全局单词默认")
        }
        if (source.isDefaultForLongText) {
            add("全局长文本默认")
        }
    }.joinToString(" · ")
}
