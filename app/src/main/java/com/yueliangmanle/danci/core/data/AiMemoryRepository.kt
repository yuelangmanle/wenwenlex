package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.ai.PlanContextCompactor
import com.yueliangmanle.danci.core.analytics.AggregatedAnalytics
import com.yueliangmanle.danci.core.analytics.LearningDashboardComposer
import com.yueliangmanle.danci.core.analytics.StudyAnalyticsAggregator
import com.yueliangmanle.danci.core.analytics.SummaryBuilder
import com.yueliangmanle.danci.core.analytics.SummaryContext
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.AnalyticsOverview
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.StudyEvent
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface StudyEventRecorder {
    fun record(event: StudyEvent)
}

fun interface LearningRecordRecorder {
    fun record(record: LearningRecord)
}

object NoOpStudyEventRecorder : StudyEventRecorder {
    override fun record(event: StudyEvent) = Unit
}

object NoOpLearningRecordRecorder : LearningRecordRecorder {
    override fun record(record: LearningRecord) = Unit
}

class AiMemoryRepository(
    private val studyRepository: StudyRepository,
    private val wordRepository: WordRepository,
    private val builtInWordsProvider: () -> List<com.yueliangmanle.danci.core.model.Word>,
    private val analyticsAggregator: StudyAnalyticsAggregator = StudyAnalyticsAggregator(),
    private val learningDashboardComposer: LearningDashboardComposer = LearningDashboardComposer(),
    private val planContextCompactor: PlanContextCompactor = PlanContextCompactor(),
    private val summaryBuilder: SummaryBuilder = SummaryBuilder(),
    private val nowProvider: () -> Instant = { Instant.now() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : StudyEventRecorder, LearningRecordRecorder {
    private val seedMutex = Mutex()

    @Volatile
    private var hasSeededWords = false

    override fun record(event: StudyEvent) {
        scope.launch {
            runCatching {
                ensureSeededWords()
                studyRepository.appendEvent(event)
                refreshMemorySummary(referenceTime = event.happenedAt)
            }
        }
    }

    override fun record(record: LearningRecord) {
        scope.launch {
            runCatching {
                ensureSeededWords()
                studyRepository.upsertLearningRecord(record)
                refreshMemorySummary(referenceTime = record.lastReviewedAt ?: nowProvider())
            }
        }
    }

    suspend fun refreshMemorySummary(referenceTime: Instant = nowProvider()): AiMemorySummary {
        ensureSeededWords()
        val recentEvents = studyRepository.getStudyEventsSince(referenceTime.minus(30, ChronoUnit.DAYS))
        val existingSummary = studyRepository.loadAiMemorySummary()
        if (recentEvents.isEmpty()) {
            val checkpointSummaries = planContextCompactor.compact(
                checkpointSummaries = existingSummary.checkpointSummaries,
                referenceTime = referenceTime,
            )
            val analyticsSnapshot = rebuildIdleAnalyticsSnapshot(existingSummary)
            val longTermInsights = summaryBuilder.buildLongTermInsights(
                analyticsSnapshot = analyticsSnapshot,
                learnerProfile = existingSummary.learnerProfile,
                weeklyTrend = existingSummary.weeklySummaries,
                checkpointSummaries = checkpointSummaries,
                planEffects = analyticsSnapshot.planEffects,
            )
            val compactedSummary = existingSummary.copy(
                checkpointSummaries = checkpointSummaries,
                analyticsSnapshot = analyticsSnapshot,
                longTermInsights = longTermInsights,
            )
            if (compactedSummary != existingSummary) {
                studyRepository.saveAiMemorySummary(compactedSummary)
            }
            return compactedSummary
        }

        val aggregated = analyticsAggregator.aggregate(
            events = recentEvents,
            referenceTime = referenceTime,
        )
        val checkpointSummaries = planContextCompactor.buildCheckpointSummaries(
            events = recentEvents,
            planHistory = existingSummary.planHistory,
            existingSummaries = existingSummary.checkpointSummaries,
            referenceTime = referenceTime,
        )
        val analyticsSnapshot = learningDashboardComposer.compose(
            aggregated = aggregated,
            planHistory = existingSummary.planHistory,
            activeBookTitle = null,
        )
        val longTermInsights = summaryBuilder.buildLongTermInsights(
            analyticsSnapshot = analyticsSnapshot,
            learnerProfile = aggregated.learnerProfile,
            weeklyTrend = aggregated.weeklySummaries.takeLast(4),
            checkpointSummaries = checkpointSummaries,
            planEffects = analyticsSnapshot.planEffects,
        )
        val summary = existingSummary.copy(
            learnerProfile = aggregated.learnerProfile,
            dailySummaries = aggregated.dailySummaries.takeLast(7),
            weeklySummaries = aggregated.weeklySummaries.takeLast(4),
            checkpointSummaries = checkpointSummaries,
            analyticsSnapshot = analyticsSnapshot,
            longTermInsights = longTermInsights,
            confusionEdges = aggregated.confusionEdges,
        )
        studyRepository.saveAiMemorySummary(summary)
        return summary
    }

    suspend fun buildContext(referenceTime: Instant = nowProvider()): SummaryContext {
        val summary = refreshMemorySummary(referenceTime)
        return summaryBuilder.buildContext(
            sevenDay = summary.dailySummaries,
            thirtyDay = summary.weeklySummaries,
            rawEvents = studyRepository.getRecentStudyEvents(limit = 200),
            learnerProfile = summary.learnerProfile,
            confusionEdges = summary.confusionEdges,
            analyticsSnapshot = summary.analyticsSnapshot,
            longTermInsights = summary.longTermInsights,
            planEffects = summary.analyticsSnapshot.planEffects,
            checkpointSummaries = summary.checkpointSummaries,
        )
    }

    private fun rebuildIdleAnalyticsSnapshot(summary: AiMemorySummary): LearningAnalyticsSnapshot {
        val storedSnapshot = summary.analyticsSnapshot
        if (
            summary.dailySummaries.isEmpty() &&
            summary.weeklySummaries.isEmpty() &&
            summary.planHistory.isEmpty() &&
            storedSnapshot == LearningAnalyticsSnapshot()
        ) {
            return storedSnapshot
        }

        val rebuilt = learningDashboardComposer.compose(
            aggregated = AggregatedAnalytics(
                dailySummaries = summary.dailySummaries,
                weeklySummaries = summary.weeklySummaries,
                learnerProfile = summary.learnerProfile ?: LearnerProfile(),
                feedbackBreakdown = storedSnapshot.feedbackBreakdown,
                pronunciationUsage = storedSnapshot.pronunciationUsage,
                performanceEvents = emptyList(),
            ),
            planHistory = summary.planHistory,
            activeBookTitle = null,
        )

        return rebuilt.copy(
            overview = storedSnapshot.overview.takeUnless { it == AnalyticsOverview() } ?: rebuilt.overview,
            dailyTrend = rebuilt.dailyTrend.ifEmpty { storedSnapshot.dailyTrend },
            feedbackBreakdown = rebuilt.feedbackBreakdown.ifEmpty { storedSnapshot.feedbackBreakdown },
            planEffects = rebuilt.planEffects.ifEmpty { storedSnapshot.planEffects },
            pronunciationUsage = storedSnapshot.pronunciationUsage
                .takeUnless { it == PronunciationUsageSnapshot() }
                ?: rebuilt.pronunciationUsage,
        )
    }

    private suspend fun ensureSeededWords() {
        if (hasSeededWords) {
            return
        }
        seedMutex.withLock {
            if (hasSeededWords) {
                return@withLock
            }
            builtInWordsProvider().forEach { word ->
                wordRepository.insertWord(word)
            }
            hasSeededWords = true
        }
    }
}

private object AiMemoryRepositoryHolder {
    @Volatile
    var repository: AiMemoryRepository? = null
}

fun buildAiMemoryRepository(context: Context): AiMemoryRepository {
    val appContext = context.applicationContext
    AiMemoryRepositoryHolder.repository?.let { return it }

    return synchronized(AiMemoryRepositoryHolder) {
        AiMemoryRepositoryHolder.repository ?: run {
            val database: DanciDatabase = buildDanciDatabase(appContext)

            AiMemoryRepository(
                studyRepository = RoomStudyRepository(database.studyDao()),
                wordRepository = RoomWordRepository(database.wordDao()),
                builtInWordsProvider = { loadBuiltInWords(appContext) },
            ).also { repository ->
                AiMemoryRepositoryHolder.repository = repository
            }
        }
    }
}
