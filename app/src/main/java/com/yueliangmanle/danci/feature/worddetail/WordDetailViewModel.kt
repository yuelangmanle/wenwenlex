package com.yueliangmanle.danci.feature.worddetail

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiProfileResolver
import com.yueliangmanle.danci.core.ai.AiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.AiWordHelpRequest
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.PhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildPhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.feature.pronunciation.SessionPronunciationSourceUiState
import com.yueliangmanle.danci.feature.pronunciation.buildSessionPronunciationSourceOptions
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_AI
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.hasAnyPhonetic
import com.yueliangmanle.danci.core.model.hasCompletePhonetic
import com.yueliangmanle.danci.core.model.needsPhoneticFill
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import com.yueliangmanle.danci.core.model.withUpdatedPhonetics
import com.yueliangmanle.danci.core.pronunciation.PronunciationSourceRegistry
import java.time.Instant

data class WordDetailUiState(
    val wordId: Long = 0L,
    val word: String = "",
    val phonetic: String? = null,
    val phoneticUk: String? = null,
    val phoneticUs: String? = null,
    val phoneticStatusLabel: String = "空白",
    val phoneticSourceLabel: String = "未补全",
    val meanings: List<String> = emptyList(),
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val similarWords: List<String> = emptyList(),
    val confusingWords: List<String> = emptyList(),
    val wordForms: List<String> = emptyList(),
    val root: String? = null,
    val selectedPronunciationSourceId: String? = null,
    val selectedPronunciationSourceLabel: String = "跟随默认来源",
    val availablePronunciationSources: List<SessionPronunciationSourceUiState> = emptyList(),
    val isAiLoading: Boolean = false,
    val isPhoneticLoading: Boolean = false,
    val aiCards: List<AiInsightCardUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class AiInsightCardUiState(
    val kind: String = "",
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList(),
    val sourceLabel: String? = null,
)

class WordDetailViewModel(
    private val appContext: Context,
    private var word: Word,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val phoneticEnrichmentRepository: PhoneticEnrichmentRepository,
    private val pronunciationSourceRepository: PronunciationSourceRepository? = null,
    private val pronunciationSourceRegistry: PronunciationSourceRegistry? = null,
    private val eventRecorder: StudyEventRecorder = NoOpStudyEventRecorder,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private var isAiLoading = false
    private var isPhoneticLoading = false
    private val aiCards = mutableListOf<AiInsightCardUiState>()
    private var selectedPronunciationSourceId: String? = null
    private var selectedPronunciationSourceLabel: String = "跟随默认来源"
    private var availablePronunciationSources: List<SessionPronunciationSourceUiState> = emptyList()

    init {
        eventRecorder.record(
            StudyEvent(
                wordId = word.id,
                eventType = StudyEventType.DETAIL_OPENED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "detail",
                    "sections" to detailSections().joinToString(","),
                ),
            ),
        )
    }

    fun buildUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): WordDetailUiState =
        WordDetailUiState(
            wordId = word.id,
            word = word.lemma,
            phonetic = word.phonetic,
            phoneticUk = word.phoneticUk,
            phoneticUs = word.phoneticUs,
            phoneticStatusLabel = when {
                word.hasCompletePhonetic() -> "双音标完整"
                word.hasAnyPhonetic() -> "部分音标"
                else -> "空白"
            },
            phoneticSourceLabel = when (word.phoneticSource) {
                "builtin" -> "来源：内置词库"
                "imported" -> "来源：导入文件"
                "ai_generated" -> "来源：AI 补全"
                "legacy" -> "来源：旧版迁移"
                else -> "来源：待补全"
            },
            meanings = word.meanings,
            exampleSentence = word.exampleSentence,
            exampleTranslation = word.exampleTranslation,
            synonyms = word.synonyms,
            antonyms = word.antonyms,
            similarWords = word.similarWords,
            confusingWords = word.confusingWords,
            wordForms = word.wordForms,
            root = word.root,
            selectedPronunciationSourceId = selectedPronunciationSourceId,
            selectedPronunciationSourceLabel = selectedPronunciationSourceLabel,
            availablePronunciationSources = availablePronunciationSources,
            isAiLoading = isAiLoading,
            isPhoneticLoading = isPhoneticLoading,
            aiCards = aiCards.toList(),
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )

    suspend fun refreshPronunciationSourceState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): WordDetailUiState {
        val sources = pronunciationSourceRegistry?.refreshBuiltinSources()
            ?: pronunciationSourceRepository?.getAllSources().orEmpty()
        val sessionPreference = settingsRepository.getPronunciationSessionPreference()
        selectedPronunciationSourceId = sessionPreference.sessionWordPronunciationSourceId
        availablePronunciationSources = buildSessionPronunciationSourceOptions(
            sources = sources,
            selectedSourceId = selectedPronunciationSourceId,
        )
        selectedPronunciationSourceLabel = availablePronunciationSources
            .firstOrNull(SessionPronunciationSourceUiState::isSelected)
            ?.title
            ?: "跟随默认来源"
        return buildUiState(
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun switchSessionPronunciationSource(
        sourceId: String?,
    ): WordDetailUiState {
        pronunciationSourceRegistry?.updateSessionWordSource(sourceId)
            ?: settingsRepository.updateSessionWordPronunciationSourceId(sourceId)
        val refreshed = refreshPronunciationSourceState()
        val message = if (sourceId == null) {
            "当前会话已恢复跟随默认来源。"
        } else {
            "当前会话发音源已切换为${refreshed.selectedPronunciationSourceLabel}。"
        }
        return refreshed.copy(
            statusMessage = message,
            errorMessage = null,
        )
    }

    fun onAiMemoryClick() {
        recordAiAction("memory_helper")
    }

    fun onAiContrastClick() {
        recordAiAction("contrast")
    }

    fun onExplainWordFormsClick() {
        recordAiAction("word_forms")
    }

    fun onExpandExampleClick() {
        recordAiAction("example_expansion")
    }

    fun onStartQuizClick() {
        eventRecorder.record(
            StudyEvent(
                wordId = word.id,
                eventType = StudyEventType.QUIZ_STARTED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "detail",
                    "source" to "word_detail",
                ),
            ),
        )
    }

    fun markAiLoading(): WordDetailUiState {
        isAiLoading = true
        return buildUiState()
    }

    fun markPhoneticLoading(): WordDetailUiState {
        isPhoneticLoading = true
        return buildUiState()
    }

    suspend fun resolveAiHelp(
        request: AiWordHelpRequest,
        settings: com.yueliangmanle.danci.core.data.AppSettings,
        runtimeSettings: com.yueliangmanle.danci.core.ai.AiRuntimeSettings?,
        coordinator: AiStrategyCoordinator,
    ): WordDetailUiState {
        val result = coordinator.requestWordHelp(
            settings = settings,
            runtimeSettings = runtimeSettings,
            word = word,
            request = request,
        )
        val kind = when (request) {
            AiWordHelpRequest.MNEMONIC -> "mnemonic"
            AiWordHelpRequest.RELATION_DIFFERENCE -> "relation_difference"
            AiWordHelpRequest.WORD_FORM_EXPLANATION -> "word_form_explanation"
            AiWordHelpRequest.EXAMPLE_EXPANSION -> "example_expansion"
            AiWordHelpRequest.MISTAKE_EXPLANATION -> "mistake_explanation"
        }
        val card = AiInsightCardUiState(
            kind = kind,
            title = result.title,
            body = result.body,
            bullets = result.bullets,
            sourceLabel = if (result.source == PlanSource.AI) "AI 生成" else "本地兜底",
        )
        aiCards.removeAll { it.kind == kind }
        aiCards.add(0, card)
        isAiLoading = false
        return buildUiState()
    }

    suspend fun fillPhonetic(
        overwrite: Boolean,
        coordinator: AiStrategyCoordinator,
    ): WordDetailUiState {
        val settings = settingsRepository.getSettings()
        val runtimeSettings = resolveRuntimeSettingsForCapability(appContext, AiCapability.PHONETIC_FILL)
        if (runtimeSettings?.enabled != true || runtimeSettings.apiKey.isNullOrBlank()) {
            isPhoneticLoading = false
            return buildUiState(errorMessage = "请先在 AI 设置里配置音标补全 API。")
        }
        if (!overwrite && !word.needsPhoneticFill()) {
            isPhoneticLoading = false
            return buildUiState(statusMessage = "当前这条单词已经有双音标，可以改用“覆盖重拉”。")
        }

        val now = Instant.now()
        val fillMode = if (overwrite) "overwrite_single" else "fill_missing_single"
        val jobId = phoneticEnrichmentRepository.insert(
            PhoneticEnrichmentJob(
                scopeType = "word",
                scopeRef = word.id.toString(),
                profileId = AiProfileResolver().resolveProfileId(settings, AiCapability.PHONETIC_FILL),
                fillMode = fillMode,
                status = "running",
                totalCount = 1,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val result = coordinator.requestPhoneticFill(
            settings = settings,
            runtimeSettings = runtimeSettings,
            word = word,
        )
        val merged = mergePhonetics(result, overwrite)
        val success = merged != word
        if (success) {
            wordRepository.updateWord(merged)
            word = merged
        }
        phoneticEnrichmentRepository.update(
            PhoneticEnrichmentJob(
                id = jobId,
                scopeType = "word",
                scopeRef = word.id.toString(),
                profileId = AiProfileResolver().resolveProfileId(settings, AiCapability.PHONETIC_FILL),
                fillMode = fillMode,
                status = if (success) "completed" else "failed",
                totalCount = 1,
                completedCount = if (success) 1 else 0,
                failedCount = if (success) 0 else 1,
                createdAt = now,
                updatedAt = Instant.now(),
            ),
        )
        isPhoneticLoading = false
        return if (success) {
            buildUiState(statusMessage = "音标已写回本地数据库。")
        } else {
            buildUiState(errorMessage = "这次没有拿到更完整的音标结果。")
        }
    }

    private fun mergePhonetics(
        result: com.yueliangmanle.danci.core.ai.AiPhoneticFillResult,
        overwrite: Boolean,
    ): Word {
        val nextUk = if (overwrite || word.phoneticUk.isNullOrBlank()) {
            result.phoneticUk ?: word.phoneticUk
        } else {
            word.phoneticUk
        }
        val nextUs = if (overwrite || word.phoneticUs.isNullOrBlank()) {
            result.phoneticUs ?: word.phoneticUs
        } else {
            word.phoneticUs
        }
        if (nextUk == word.phoneticUk && nextUs == word.phoneticUs) {
            return word
        }
        return word.withUpdatedPhonetics(
            phoneticUk = nextUk,
            phoneticUs = nextUs,
            source = if (result.source == PlanSource.AI) PHONETIC_SOURCE_AI else word.phoneticSource,
            updatedAt = Instant.now(),
        )
    }

    private fun recordAiAction(action: String) {
        eventRecorder.record(
            StudyEvent(
                wordId = word.id,
                eventType = StudyEventType.AI_ACTION,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "detail",
                    "action" to action,
                ),
            ),
        )
    }

    private fun detailSections(): List<String> =
        buildList {
            if (word.synonyms.isNotEmpty()) add("synonyms")
            if (word.antonyms.isNotEmpty()) add("antonyms")
            if (word.similarWords.isNotEmpty()) add("similar_words")
            if (word.confusingWords.isNotEmpty()) add("confusing_words")
            if (word.wordForms.isNotEmpty()) add("word_forms")
            if (!word.root.isNullOrBlank()) add("root")
        }
}

suspend fun loadWordDetailViewModel(
    context: Context,
    wordId: Long,
): WordDetailViewModel {
    syncBuiltInCatalogToDatabase(context)
    val appContext = context.applicationContext
    val wordRepository = buildWordRepository(appContext)
    val settingsRepository = buildSettingsRepository(appContext)
    val pronunciationSourceRepository = buildPronunciationSourceRepository(appContext)
    val word = requireNotNull(wordRepository.getWord(wordId)) { "Expected word for id=$wordId" }
    return WordDetailViewModel(
        appContext = appContext,
        word = word,
        wordRepository = wordRepository,
        settingsRepository = settingsRepository,
        phoneticEnrichmentRepository = buildPhoneticEnrichmentRepository(appContext),
        pronunciationSourceRepository = pronunciationSourceRepository,
        pronunciationSourceRegistry = PronunciationSourceRegistry(
            sourceRepository = pronunciationSourceRepository,
            voicePackRepository = buildVoicePackRepository(appContext),
            settingsRepository = settingsRepository,
        ),
        eventRecorder = buildAiMemoryRepository(appContext),
    )
}
