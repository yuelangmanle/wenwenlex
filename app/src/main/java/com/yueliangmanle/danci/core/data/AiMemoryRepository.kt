package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.analytics.StudyAnalyticsAggregator
import com.yueliangmanle.danci.core.analytics.SummaryBuilder
import com.yueliangmanle.danci.core.analytics.SummaryContext
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.AiMemorySummary
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

object NoOpStudyEventRecorder : StudyEventRecorder {
    override fun record(event: StudyEvent) = Unit
}

class AiMemoryRepository(
    private val studyRepository: StudyRepository,
    private val wordRepository: WordRepository,
    private val builtInWordsProvider: () -> List<com.yueliangmanle.danci.core.model.Word>,
    private val analyticsAggregator: StudyAnalyticsAggregator = StudyAnalyticsAggregator(),
    private val summaryBuilder: SummaryBuilder = SummaryBuilder(),
    private val nowProvider: () -> Instant = { Instant.now() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : StudyEventRecorder {
    private val seedMutex = Mutex()

    @Volatile
    private var hasSeededWords = false

    override fun record(event: StudyEvent) {
        scope.launch {
            runCatching {
                ensureSeededWords()
                studyRepository.appendEvent(event)
                refreshMemorySummary()
            }
        }
    }

    suspend fun refreshMemorySummary(referenceTime: Instant = nowProvider()): AiMemorySummary {
        ensureSeededWords()
        val recentEvents = studyRepository.getStudyEventsSince(referenceTime.minus(30, ChronoUnit.DAYS))
        val existingSummary = studyRepository.loadAiMemorySummary()
        if (recentEvents.isEmpty()) {
            return existingSummary
        }

        val aggregated = analyticsAggregator.aggregate(
            events = recentEvents,
            referenceTime = referenceTime,
        )
        val summary = AiMemorySummary(
            learnerProfile = aggregated.learnerProfile,
            dailySummaries = aggregated.dailySummaries.takeLast(7),
            weeklySummaries = aggregated.weeklySummaries.takeLast(4),
            planHistory = existingSummary.planHistory,
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
