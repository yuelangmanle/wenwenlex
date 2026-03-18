package com.yueliangmanle.danci.core.ai

import android.content.Context
import com.yueliangmanle.danci.core.data.AiMemoryRepository
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.security.AiCredentialStore
import com.yueliangmanle.danci.core.security.buildAiCredentialStore

enum class PlanSource {
    AI,
    LOCAL_FALLBACK,
}

data class CurrentPlanSnapshot(
    val settings: AppSettings,
    val runtimeSettings: AiRuntimeSettings? = null,
    val memory: AiMemorySummary = AiMemorySummary(),
    val activeBookTitle: String,
    val headline: String,
    val mistakeCount: Int,
    val anomalyNotes: List<String> = emptyList(),
)

data class AiPlanAdjustmentResult(
    val summary: String,
    val recommendedFocus: List<String> = emptyList(),
    val suggestedModes: List<String> = emptyList(),
    val suggestedPace: String? = null,
    val checkpointAdvice: String? = null,
    val source: PlanSource,
)

data class AiWordHelpResult(
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList(),
    val source: PlanSource,
)

class AiStrategyCoordinator(
    private val client: AiClient = ResponsesApiAiClient(),
    private val promptFactory: AiPromptFactory = AiPromptFactory(),
    private val contextBuilder: AiContextBuilder = AiContextBuilder(),
    private val parser: AiSuggestionParser = AiSuggestionParser(),
) {
    suspend fun adjustPlan(snapshot: CurrentPlanSnapshot): AiPlanAdjustmentResult {
        if (!snapshot.settings.aiEnabled || !snapshot.settings.aiPlanAdjustmentEnabled) {
            return localFallbackPlan(snapshot)
        }
        val runtimeSettings = snapshot.runtimeSettings
        if (runtimeSettings?.enabled != true || runtimeSettings.apiKey.isNullOrBlank()) {
            return localFallbackPlan(snapshot)
        }

        val prompt = promptFactory.buildPlanAdjustmentPrompt(
            contextJson = contextBuilder.buildPlanAdjustmentContext(
                settings = runtimeSettings,
                memory = snapshot.memory,
                activeBookTitle = snapshot.activeBookTitle,
                dailyGoal = snapshot.settings.dailyGoal,
                anomalyNotes = snapshot.anomalyNotes,
                currentHeadline = snapshot.headline,
            ),
        )

        val parsed = runCatching {
            client.generate(
                AiTextRequest(
                    runtimeSettings = runtimeSettings,
                    instructions = prompt.instructions,
                    input = prompt.input,
                    responseFormat = prompt.responseFormat,
                ),
            )
        }.mapCatching { response ->
            parser.parsePlanAdjustment(response.text) ?: error("Invalid plan adjustment payload")
        }.getOrNull()

        return parsed?.let { suggestion ->
            AiPlanAdjustmentResult(
                summary = suggestion.summary,
                recommendedFocus = suggestion.recommendedFocus,
                suggestedModes = suggestion.suggestedModes,
                suggestedPace = suggestion.suggestedPace,
                checkpointAdvice = suggestion.checkpointAdvice,
                source = PlanSource.AI,
            )
        } ?: localFallbackPlan(snapshot)
    }

    suspend fun requestWordHelp(
        settings: AppSettings,
        runtimeSettings: AiRuntimeSettings?,
        word: Word,
        request: AiWordHelpRequest,
        wrongOption: String? = null,
        correctOption: String? = null,
    ): AiWordHelpResult {
        if (!settings.aiEnabled || runtimeSettings?.enabled != true || runtimeSettings.apiKey.isNullOrBlank()) {
            return localFallbackWordHelp(word, request, wrongOption, correctOption)
        }

        val prompt = promptFactory.buildWordHelpPrompt(
            word = word,
            request = request,
            wrongOption = wrongOption,
            correctOption = correctOption,
        )

        val parsed = runCatching {
            client.generate(
                AiTextRequest(
                    runtimeSettings = runtimeSettings,
                    instructions = prompt.instructions,
                    input = prompt.input,
                    responseFormat = prompt.responseFormat,
                ),
            )
        }.mapCatching { response ->
            parser.parseWordHelp(response.text) ?: error("Invalid word help payload")
        }.getOrNull()

        return parsed?.let {
            AiWordHelpResult(
                title = it.title,
                body = it.body,
                bullets = it.bullets,
                source = PlanSource.AI,
            )
        } ?: localFallbackWordHelp(word, request, wrongOption, correctOption)
    }

    fun localFallbackPlan(snapshot: CurrentPlanSnapshot): AiPlanAdjustmentResult {
        val summary = when {
            snapshot.mistakeCount >= 5 -> "先压住错词密度，减少新词推进，把近义词和易混词回拉一轮。"
            snapshot.anomalyNotes.isNotEmpty() -> "本轮先稳节奏，优先处理异常点，再继续推进今天任务。"
            else -> "保持当前节奏，先完成首页任务，再小幅复习薄弱词。"
        }
        return AiPlanAdjustmentResult(
            summary = summary,
            recommendedFocus = snapshot.memory.learnerProfile?.weakSpots?.take(3).orEmpty(),
            suggestedModes = if (snapshot.mistakeCount >= 4) listOf("quiz", "word_detail") else listOf("card", "quiz"),
            suggestedPace = if (snapshot.mistakeCount >= 5) "slow_down" else "steady",
            checkpointAdvice = if (snapshot.anomalyNotes.isNotEmpty()) {
                snapshot.anomalyNotes.joinToString("；")
            } else {
                "继续保持短轮次复习，避免一次塞入过多新词。"
            },
            source = PlanSource.LOCAL_FALLBACK,
        )
    }

    private fun localFallbackWordHelp(
        word: Word,
        request: AiWordHelpRequest,
        wrongOption: String?,
        correctOption: String?,
    ): AiWordHelpResult =
        when (request) {
            AiWordHelpRequest.MNEMONIC -> AiWordHelpResult(
                title = "记忆提示",
                body = buildString {
                    append("先抓住 ${word.lemma} 的核心义“${word.meanings.firstOrNull().orEmpty()}”。")
                    word.root?.takeIf(String::isNotBlank)?.let { append(" 可以把它和词根 $it 绑定记忆。") }
                    if (word.wordForms.isNotEmpty()) append(" 再顺带记住变形：${word.wordForms.joinToString("、")}。")
                },
                bullets = word.synonyms.take(2),
                source = PlanSource.LOCAL_FALLBACK,
            )
            AiWordHelpRequest.RELATION_DIFFERENCE -> AiWordHelpResult(
                title = "关系辨析",
                body = buildString {
                    append("近义词：${word.synonyms.joinToString("、").ifBlank { "暂无" }}。")
                    append(" 反义词：${word.antonyms.joinToString("、").ifBlank { "暂无" }}。")
                    append(" 拼写相近词：${word.similarWords.joinToString("、").ifBlank { "暂无" }}。")
                },
                bullets = listOfNotNull(
                    word.synonyms.firstOrNull()?.let { "遇到 $it 时先比语气和搭配" },
                    word.similarWords.firstOrNull()?.let { "看到 $it 时注意不要按字母形状误判" },
                ),
                source = PlanSource.LOCAL_FALLBACK,
            )
            AiWordHelpRequest.WORD_FORM_EXPLANATION -> AiWordHelpResult(
                title = "词形讲解",
                body = buildString {
                    append("这个词常见变形有：${word.wordForms.joinToString("、").ifBlank { "暂无记录" }}。")
                    word.root?.takeIf(String::isNotBlank)?.let { append(" 词根词缀线索是 $it。") }
                },
                bullets = word.wordForms.take(3),
                source = PlanSource.LOCAL_FALLBACK,
            )
            AiWordHelpRequest.EXAMPLE_EXPANSION -> AiWordHelpResult(
                title = "例句扩展",
                body = word.exampleSentence?.let { sentence ->
                    "先把原句记牢：$sentence。再试着用自己的场景重写一遍。"
                } ?: "先用这个词造一个贴近自己生活的短句，再把中文意思反推回英文。",
                bullets = listOfNotNull(word.exampleTranslation),
                source = PlanSource.LOCAL_FALLBACK,
            )
            AiWordHelpRequest.MISTAKE_EXPLANATION -> AiWordHelpResult(
                title = "错因解释",
                body = buildString {
                    append("你刚才把“${word.lemma}”和“${wrongOption.orEmpty()}”混在一起了。")
                    if (!correctOption.isNullOrBlank()) append(" 正确答案应回到“$correctOption”的语义。")
                    append(" 下次先看核心义，再看搭配和语气。")
                },
                bullets = listOfNotNull(
                    correctOption?.let { "正确：$it" },
                    wrongOption?.let { "错误：$it" },
                ),
                source = PlanSource.LOCAL_FALLBACK,
            )
        }
}

private object AiStrategyCoordinatorHolder {
    @Volatile
    var instance: AiStrategyCoordinator? = null
}

fun buildAiStrategyCoordinator(context: Context): AiStrategyCoordinator {
    AiStrategyCoordinatorHolder.instance?.let { return it }
    return synchronized(AiStrategyCoordinatorHolder) {
        AiStrategyCoordinatorHolder.instance ?: AiStrategyCoordinator().also { coordinator ->
            AiStrategyCoordinatorHolder.instance = coordinator
        }
    }
}

suspend fun buildRuntimeSettings(
    settings: AppSettings,
    credentialStore: AiCredentialStore,
): AiRuntimeSettings =
    AiRuntimeSettings(
        enabled = settings.aiEnabled,
        baseUrl = settings.aiBaseUrl,
        apiKey = credentialStore.readApiKey(),
        model = settings.aiModel,
    )

suspend fun loadCurrentPlanSnapshot(
    context: Context,
    activeBookTitle: String,
    headline: String,
    mistakeCount: Int,
    anomalyNotes: List<String> = emptyList(),
): CurrentPlanSnapshot {
    val settingsRepository = buildSettingsRepository(context)
    val settings = settingsRepository.getSettings()
    val memoryRepository: AiMemoryRepository = buildAiMemoryRepository(context)
    val memory = memoryRepository.refreshMemorySummary()
    return CurrentPlanSnapshot(
        settings = settings,
        runtimeSettings = buildRuntimeSettings(settings, buildAiCredentialStore(context)),
        memory = memory,
        activeBookTitle = activeBookTitle,
        headline = headline,
        mistakeCount = mistakeCount,
        anomalyNotes = anomalyNotes,
    )
}
