package com.yueliangmanle.danci.feature.analytics

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import kotlin.math.roundToInt

class LearningAnalyticsHtmlRenderer {
    fun render(
        snapshot: LearningAnalyticsSnapshot,
        settings: AppSettings = AppSettings(),
        goalProgress: GoalProgressSnapshot = GoalProgressSnapshot(),
    ): String =
        buildString {
            appendLine("<!doctype html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("""<meta charset="utf-8">""")
            appendLine("""<meta name="viewport" content="width=device-width, initial-scale=1">""")
            appendLine("<style>")
            appendLine(
                """
                :root {
                  --paper: #f7f1e6;
                  --card: #fffaf3;
                  --ink: #2f241f;
                  --muted: #7a6a5b;
                  --line: #dfd0c1;
                  --accent: #d8794b;
                  --accent-soft: #f3c8a7;
                  --accent-cool: #6d9dc5;
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  background: linear-gradient(180deg, #fbf7f0 0%, var(--paper) 100%);
                  color: var(--ink);
                  padding: 18px;
                }
                .stack {
                  display: grid;
                  gap: 14px;
                }
                .card {
                  background: rgba(255, 250, 243, 0.96);
                  border: 1px solid var(--line);
                  border-radius: 18px;
                  padding: 16px;
                  box-shadow: 0 10px 24px rgba(71, 46, 28, 0.08);
                }
                h1, h2, p { margin: 0; }
                h1 {
                  font-size: 20px;
                  margin-bottom: 6px;
                }
                h2 {
                  font-size: 15px;
                  margin-bottom: 10px;
                }
                .meta {
                  color: var(--muted);
                  font-size: 12px;
                }
                .bars {
                  display: grid;
                  gap: 10px;
                }
                .bar-row {
                  display: grid;
                  gap: 6px;
                }
                .bar-label {
                  display: flex;
                  justify-content: space-between;
                  font-size: 12px;
                  color: var(--muted);
                }
                .bar-track {
                  height: 10px;
                  border-radius: 999px;
                  background: #efe4d8;
                  overflow: hidden;
                }
                .bar-fill {
                  height: 100%;
                  border-radius: 999px;
                  background: linear-gradient(90deg, var(--accent) 0%, var(--accent-soft) 100%);
                }
                .chips {
                  display: flex;
                  flex-wrap: wrap;
                  gap: 8px;
                }
                .chip {
                  padding: 8px 10px;
                  border-radius: 999px;
                  background: #fff4ea;
                  border: 1px solid #f1d8c5;
                  font-size: 12px;
                }
                .plan {
                  border-left: 4px solid var(--accent-cool);
                  padding-left: 12px;
                  margin-top: 10px;
                }
                ul {
                  margin: 0;
                  padding-left: 18px;
                }
                li + li {
                  margin-top: 8px;
                }
                """.trimIndent(),
            )
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("""<div class="stack">""")
            appendLine("""<section class="card"><h1>学习统计看板</h1><p class="meta">最近趋势、反馈、计划效果和发音使用概况</p></section>""")
            appendLine(renderOverviewSection(snapshot))
            appendLine(renderGoalProgressSection(settings, goalProgress))
            appendLine(renderTrendSection(snapshot))
            appendLine(renderFeedbackSection(snapshot))
            appendLine(renderPlanEffectsSection(snapshot))
            appendLine(renderPronunciationSection(snapshot))
            appendLine("</div>")
            appendLine("</body>")
            appendLine("</html>")
        }

    private fun renderOverviewSection(snapshot: LearningAnalyticsSnapshot): String {
        val overview = snapshot.overview
        val accuracy = overview.accuracyRate?.let(::formatPercent) ?: "暂无"
        return """
        <section class="card">
          <h2>概览</h2>
          <div class="chips">
            <span class="chip">正确率 $accuracy</span>
            <span class="chip">学习天数 ${overview.studiedDays}</span>
            <span class="chip">已掌握 ${overview.masteredCount}</span>
          </div>
        </section>
        """.trimIndent()
    }

    private fun renderGoalProgressSection(
        settings: AppSettings,
        goalProgress: GoalProgressSnapshot,
    ): String {
        val weeklyGoal = settings.weeklyGoal.coerceAtLeast(1)
        val weeklyProgress = (goalProgress.currentWeekCompletedCount.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)
        val weeklyWidth = (weeklyProgress * 100f).roundToInt()
        val phaseTargetWords = goalProgress.phaseTargetWords.takeIf { it > 0 } ?: settings.phaseTargetWords
        val phaseTitle = goalProgress.phaseName ?: settings.phaseName
        val phaseCopy = if (!phaseTitle.isNullOrBlank() && phaseTargetWords > 0) {
            "${phaseTitle} · ${goalProgress.phaseCompletedWords} / $phaseTargetWords"
        } else if (!phaseTitle.isNullOrBlank()) {
            "${phaseTitle} · 已完成 ${goalProgress.phaseCompletedWords} 词"
        } else if (phaseTargetWords > 0) {
            "当前阶段 · ${goalProgress.phaseCompletedWords} / $phaseTargetWords"
        } else {
            "还没有设置阶段目标"
        }
        val phaseWidth = if (phaseTargetWords > 0) {
            ((goalProgress.phaseCompletedWords.toFloat() / phaseTargetWords.toFloat()).coerceIn(0f, 1f) * 100f).roundToInt()
        } else {
            0
        }

        return """
        <section class="card">
          <h2>目标推进</h2>
          <div class="bars">
            <div class="bar-row">
              <div class="bar-label">
                <span>本周目标</span>
                <span>${goalProgress.currentWeekCompletedCount} / $weeklyGoal</span>
              </div>
              <div class="bar-track"><div class="bar-fill" style="width: ${weeklyWidth}%"></div></div>
            </div>
            <div class="bar-row">
              <div class="bar-label">
                <span>连续学习</span>
                <span>${goalProgress.currentStreakDays} 天 · 最佳 ${goalProgress.bestStreakDays} 天</span>
              </div>
              <div class="bar-track"><div class="bar-fill" style="width: ${(goalProgress.currentStreakDays.coerceAtMost(14) / 14f * 100f).roundToInt()}%"></div></div>
            </div>
            <div class="bar-row">
              <div class="bar-label">
                <span>当前阶段</span>
                <span>${escapeHtml(phaseCopy)}</span>
              </div>
              <div class="bar-track"><div class="bar-fill" style="width: ${phaseWidth}%"></div></div>
            </div>
          </div>
        </section>
        """.trimIndent()
    }

    private fun renderTrendSection(snapshot: LearningAnalyticsSnapshot): String {
        val bars = snapshot.dailyTrend.takeLast(7).joinToString(separator = "") { point ->
            val width = point.correctRate.coerceIn(0f, 1f) * 100f
            """
            <div class="bar-row">
              <div class="bar-label">
                <span>${escapeHtml(point.date)}</span>
                <span>${point.studiedCount} 词 / ${formatPercent(point.correctRate)}</span>
              </div>
              <div class="bar-track"><div class="bar-fill" style="width: ${width.roundToInt()}%"></div></div>
            </div>
            """.trimIndent()
        }.ifBlank {
            """<p class="meta">最近还没有足够的日趋势数据。</p>"""
        }

        return """
        <section class="card">
          <h2>日趋势</h2>
          <div class="bars">$bars</div>
        </section>
        """.trimIndent()
    }

    private fun renderFeedbackSection(snapshot: LearningAnalyticsSnapshot): String {
        val chips = snapshot.feedbackBreakdown.joinToString(separator = "") { bucket ->
            """<span class="chip">${escapeHtml(bucket.label)} ${bucket.count} 次 · ${formatPercent(bucket.ratio)}</span>"""
        }.ifBlank {
            """<p class="meta">最近没有可展示的反馈分布。</p>"""
        }

        return """
        <section class="card">
          <h2>反馈分布</h2>
          <div class="chips">$chips</div>
        </section>
        """.trimIndent()
    }

    private fun renderPlanEffectsSection(snapshot: LearningAnalyticsSnapshot): String {
        val plans = snapshot.planEffects.joinToString(separator = "") { effect ->
            val beforeRate = effect.beforeCorrectRate?.let(::formatPercent) ?: "样本不足"
            val afterRate = effect.afterCorrectRate?.let(::formatPercent) ?: "样本不足"
            """
            <div class="plan">
              <strong>${escapeHtml(effect.label.ifBlank { "最近一次调整" })}</strong>
              <p class="meta">调整前：$beforeRate · 调整后：$afterRate</p>
              <p>${escapeHtml(effect.outcomeSummary ?: "效果仍在观察")}</p>
            </div>
            """.trimIndent()
        }.ifBlank {
            """<p class="meta">还没有足够的计划效果样本。</p>"""
        }

        return """
        <section class="card">
          <h2>计划效果</h2>
          $plans
        </section>
        """.trimIndent()
    }

    private fun renderPronunciationSection(snapshot: LearningAnalyticsSnapshot): String {
        val usage = snapshot.pronunciationUsage
        return """
        <section class="card">
          <h2>发音使用</h2>
          <ul>
            <li>播音：${usage.voicePlaybackCount} 次</li>
            <li>跟读：${usage.followReadCount} 次</li>
            <li>shadowing：${usage.shadowingCount} 次</li>
          </ul>
        </section>
        """.trimIndent()
    }

    private fun formatPercent(value: Float): String = "${(value * 100).roundToInt()}%"

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
