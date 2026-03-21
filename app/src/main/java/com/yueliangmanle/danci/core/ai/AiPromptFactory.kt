package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.importer.XlsxSheetData
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
                你的建议必须保守、可执行，并且要明确为什么调整、调整了什么、出现了哪些异常信号，以及最近执行效果。
                你必须重点关注最近一次调整效果、长期统计摘要、发音使用概况，以及这些信号是否支持继续当前节奏。
            """.trimIndent(),
            input = """
                请阅读下面的学习上下文，为下一阶段输出结构化调整建议。
                请特别结合最近一次调整效果、长期统计摘要、发音使用概况，不要只复述字段：
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
                            .put("checkpoint_advice", JSONObject().put("type", "string"))
                            .put("reason_summary", JSONObject().put("type", "string"))
                            .put("change_summary", JSONObject().put("type", "string"))
                            .put(
                                "abnormal_signals",
                                JSONObject()
                                    .put("type", "array")
                                    .put("items", JSONObject().put("type", "string")),
                            )
                            .put("execution_effect", JSONObject().put("type", "string")),
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
                                "reason_summary",
                                "change_summary",
                                "abnormal_signals",
                                "execution_effect",
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

    fun buildImportNormalizationPrompt(
        workbookSheets: List<XlsxSheetData>,
    ): AiStructuredPrompt =
        AiStructuredPrompt(
            instructions = """
                你是文文Lex 的 Excel 词书整理助手。
                只输出合法 JSON，不要输出 Markdown。
                你可以识别真正的数据表、英文列和中文列，也可以补全缺失的中文义和音标。
                但你绝对不能凭空编造不存在的英文单词。
            """.trimIndent(),
            input = """
                请把下面的工作表预览整理成文文Lex 可导入结构：
                ${workbookSheets.toWorkbookPreviewJson()}
            """.trimIndent(),
            responseFormat = AiResponseFormat.JsonSchema(
                name = "import_normalization",
                schema = JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject()
                            .put("sheet_name", JSONObject().put("type", "string"))
                            .put("total_rows", JSONObject().put("type", "integer"))
                            .put("skipped_rows", JSONObject().put("type", "integer"))
                            .put(
                                "rows",
                                JSONObject()
                                    .put("type", "array")
                                    .put(
                                        "items",
                                        JSONObject()
                                            .put("type", "object")
                                            .put(
                                                "properties",
                                                JSONObject()
                                                    .put("word", JSONObject().put("type", "string"))
                                                    .put(
                                                        "meanings",
                                                        JSONObject()
                                                            .put("type", "array")
                                                            .put("items", JSONObject().put("type", "string")),
                                                    )
                                                    .put("phonetic_uk", JSONObject().put("type", "string"))
                                                    .put("phonetic_us", JSONObject().put("type", "string")),
                                            )
                                            .put("required", JSONArray(listOf("word", "meanings", "phonetic_uk", "phonetic_us"))),
                                    ),
                            ),
                    )
                    .put("required", JSONArray(listOf("sheet_name", "total_rows", "skipped_rows", "rows"))),
            ),
        )

    fun buildPhoneticFillPrompt(word: Word): AiStructuredPrompt =
        AiStructuredPrompt(
            instructions = """
                你是文文Lex 的音标补全助手。
                只输出合法 JSON，不要输出 Markdown。
                请优先给出英式和美式两套音标，如果只能确定一套，另一套输出空字符串。
            """.trimIndent(),
            input = """
                单词：${word.lemma}
                释义：${word.meanings.joinToString("；")}
                已有音标：${word.phonetic.orEmpty()}
                请输出 phonetic_uk 和 phonetic_us。
            """.trimIndent(),
            responseFormat = AiResponseFormat.JsonSchema(
                name = "phonetic_fill",
                schema = JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject()
                            .put("phonetic_uk", JSONObject().put("type", "string"))
                            .put("phonetic_us", JSONObject().put("type", "string")),
                    )
                    .put("required", JSONArray(listOf("phonetic_uk", "phonetic_us"))),
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

private fun List<XlsxSheetData>.toWorkbookPreviewJson(): String =
    JSONArray(
        map { sheet ->
            JSONObject()
                .put("name", sheet.name)
                .put(
                    "rows",
                    JSONArray(
                        sheet.rows.take(60).map { row ->
                            JSONArray(row.take(6))
                        },
                    ),
                )
        },
    ).toString()
