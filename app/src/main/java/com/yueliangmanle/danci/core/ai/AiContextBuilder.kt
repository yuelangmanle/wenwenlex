package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.AiMemorySummary
import org.json.JSONArray
import org.json.JSONObject

class AiContextBuilder {
    fun buildPlanAdjustmentContext(
        settings: AiRuntimeSettings,
        memory: AiMemorySummary,
        activeBookTitle: String = "",
        dailyGoal: Int = 20,
        anomalyNotes: List<String> = emptyList(),
        currentHeadline: String? = null,
    ): String =
        JSONObject()
            .put(
                "service_config",
                JSONObject()
                    .put("enabled", settings.enabled)
                    .put("base_url", settings.baseUrl)
                    .put("model", settings.model),
            )
            .put(
                "current_plan",
                JSONObject()
                    .put("active_book_title", activeBookTitle)
                    .put("daily_goal", dailyGoal)
                    .put("headline", currentHeadline.orEmpty())
                    .put("anomaly_notes", JSONArray(anomalyNotes)),
            )
            .put("learner_profile", learnerProfileJson(memory))
            .put("daily_summaries", dailySummariesJson(memory))
            .put("weekly_summaries", weeklySummariesJson(memory))
            .put("plan_history", planHistoryJson(memory))
            .put("confusion_edges", confusionEdgesJson(memory))
            .toString()

    private fun learnerProfileJson(memory: AiMemorySummary): JSONObject =
        memory.learnerProfile?.let { profile ->
            JSONObject()
                .put("vocabulary_level", profile.vocabularyLevel)
                .put("weak_spots", JSONArray(profile.weakSpots))
                .put("preferred_question_types", JSONArray(profile.preferredQuestionTypes))
                .put("common_mistake_patterns", JSONArray(profile.commonMistakePatterns))
        } ?: JSONObject()

    private fun dailySummariesJson(memory: AiMemorySummary): JSONArray =
        JSONArray().apply {
            memory.dailySummaries.forEach { summary ->
                put(
                    JSONObject()
                        .put("date", summary.date)
                        .put("studied_count", summary.studiedCount)
                        .put("review_count", summary.reviewCount)
                        .put("correct_rate", summary.correctRate)
                        .put("fatigue_note", summary.fatigueNote)
                        .put("primary_mistake_reasons", JSONArray(summary.primaryMistakeReasons)),
                )
            }
        }

    private fun weeklySummariesJson(memory: AiMemorySummary): JSONArray =
        JSONArray().apply {
            memory.weeklySummaries.forEach { summary ->
                put(
                    JSONObject()
                        .put("week_start_date", summary.weekStartDate)
                        .put("studied_count", summary.studiedCount)
                        .put("correct_rate", summary.correctRate)
                        .put("trend_summary", summary.trendSummary)
                        .put("persistent_weak_spots", JSONArray(summary.persistentWeakSpots)),
                )
            }
        }

    private fun planHistoryJson(memory: AiMemorySummary): JSONArray =
        JSONArray().apply {
            memory.planHistory.forEach { entry ->
                put(
                    JSONObject()
                        .put("generated_at", entry.generatedAt.toString())
                        .put("summary", entry.summary)
                        .put("recommended_focus", JSONArray(entry.recommendedFocus))
                        .put("suggested_pace", entry.suggestedPace)
                        .put("execution_effect", entry.executionEffect),
                )
            }
        }

    private fun confusionEdgesJson(memory: AiMemorySummary): JSONArray =
        JSONArray().apply {
            memory.confusionEdges.forEach { edge ->
                put(
                    JSONObject()
                        .put("source_word_id", edge.sourceWordId)
                        .put("target_word_id", edge.targetWordId)
                        .put("relation_type", edge.relationType)
                        .put("mistake_count", edge.mistakeCount)
                        .put("weight", edge.weight),
                )
            }
        }
}
