package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.Word
import org.json.JSONArray
import org.json.JSONObject

sealed interface AiWordHelpRequest {
    data object MNEMONIC : AiWordHelpRequest
    data object RELATION_DIFFERENCE : AiWordHelpRequest
    data object WORD_FORM_EXPLANATION : AiWordHelpRequest
    data object EXAMPLE_EXPANSION : AiWordHelpRequest
    data object MISTAKE_EXPLANATION : AiWordHelpRequest
}

data class AiStructuredPrompt(
    val instructions: String,
    val input: String,
    val responseFormat: AiResponseFormat = AiResponseFormat.PlainText,
)

class AiPromptFactory {
    fun buildPlanAdjustmentPrompt(contextJson: String): AiStructuredPrompt =
        AiStructuredPrompt(
            instructions = """
                你是文文Lex 的学习策略教练。
                只输出合法 JSON，不要输出 Markdown。
                你的建议必须保守、可执行，并且只能给出学习重点、题型建议和节奏建议。
            """.trimIndent(),
            input = """
                请阅读下面的学习上下文，为下一阶段输出结构化调整建议：
                $contextJson
            """.trimIndent(),
            responseFormat = AiResponseFormat.JsonSchema(
                name = "plan_adjustment",
                schema = JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject()
                            .put("summary", JSONObject().put("type", "string"))
                            .put(
                                "recommended_focus",
                                JSONObject()
                                    .put("type", "array")
                                    .put("items", JSONObject().put("type", "string")),
                            )
                            .put(
                                "suggested_modes",
                                JSONObject()
                                    .put("type", "array")
                                    .put("items", JSONObject().put("type", "string")),
                            )
                            .put("suggested_pace", JSONObject().put("type", "string"))
                            .put("checkpoint_advice", JSONObject().put("type", "string")),
                    )
                    .put(
                        "required",
                        JSONArray(
                            listOf(
                                "summary",
                                "recommended_focus",
                                "suggested_modes",
                                "suggested_pace",
                                "checkpoint_advice",
                            ),
                        ),
                    ),
            ),
        )

    fun buildWordHelpPrompt(
        word: Word,
        request: AiWordHelpRequest,
        wrongOption: String? = null,
        correctOption: String? = null,
    ): AiStructuredPrompt =
        AiStructuredPrompt(
            instructions = """
                你是文文Lex 的单词学习助手。
                只输出合法 JSON，不要输出 Markdown。
                用简洁中文回答，帮助用户记忆和辨析。
            """.trimIndent(),
            input = wordHelpPrompt(
                word = word,
                request = request,
                wrongOption = wrongOption,
                correctOption = correctOption,
            ),
            responseFormat = AiResponseFormat.JsonSchema(
                name = "word_help",
                schema = JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject()
                            .put("title", JSONObject().put("type", "string"))
                            .put("body", JSONObject().put("type", "string"))
                            .put(
                                "bullets",
                                JSONObject()
                                    .put("type", "array")
                                    .put("items", JSONObject().put("type", "string")),
                            ),
                    )
                    .put("required", JSONArray(listOf("title", "body", "bullets"))),
            ),
        )

    fun wordHelpPrompt(
        word: Word,
        request: AiWordHelpRequest,
        wrongOption: String? = null,
        correctOption: String? = null,
    ): String {
        val base = buildString {
            appendLine("单词：${word.lemma}")
            appendLine("释义：${word.meanings.joinToString("；")}")
            if (word.synonyms.isNotEmpty()) appendLine("近义词：${word.synonyms.joinToString("、")}")
            if (word.antonyms.isNotEmpty()) appendLine("反义词：${word.antonyms.joinToString("、")}")
            if (word.similarWords.isNotEmpty()) appendLine("拼写相近词：${word.similarWords.joinToString("、")}")
            if (word.wordForms.isNotEmpty()) appendLine("单词变形：${word.wordForms.joinToString("、")}")
            if (!word.root.isNullOrBlank()) appendLine("词根词缀：${word.root}")
        }
        return when (request) {
            AiWordHelpRequest.MNEMONIC -> {
                """
                    $base
                    任务：请给出 1 到 2 个中文记忆提示，尽量结合词根词缀或联想画面。
                """.trimIndent()
            }
            AiWordHelpRequest.RELATION_DIFFERENCE -> {
                """
                    $base
                    任务：请重点解释这个词与近义词、反义词、拼写相近词之间的差别，避免混淆。
                """.trimIndent()
            }
            AiWordHelpRequest.WORD_FORM_EXPLANATION -> {
                """
                    $base
                    任务：请解释这个词的单词变形、词性变化和词根词缀之间的关系。
                """.trimIndent()
            }
            AiWordHelpRequest.EXAMPLE_EXPANSION -> {
                """
                    $base
                    任务：请给出 1 到 2 个更贴近日常使用的补充例句，并解释语境。
                """.trimIndent()
            }
            AiWordHelpRequest.MISTAKE_EXPLANATION -> {
                """
                    $base
                    用户刚做错了一道题：
                    错误选项：${wrongOption.orEmpty()}
                    正确选项：${correctOption.orEmpty()}
                    任务：解释为什么会错，指出语义差异，并给出 1 个记忆技巧。
                """.trimIndent()
            }
        }
    }
}
