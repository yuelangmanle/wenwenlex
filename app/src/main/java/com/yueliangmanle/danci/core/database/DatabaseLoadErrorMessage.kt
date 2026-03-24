package com.yueliangmanle.danci.core.database

fun Throwable.toUserFacingLoadMessage(pageLabel: String): String {
    val rawMessage = message.orEmpty()
    return when {
        rawMessage.contains("migration from", ignoreCase = true) ||
            rawMessage.contains("Migration path", ignoreCase = true) -> {
            "${pageLabel}检测到本地数据版本不一致，更新到最新修复包后会自动迁移。"
        }

        rawMessage.contains("no such table", ignoreCase = true) -> {
            "${pageLabel}读取本地数据失败，请稍后重试。"
        }

        else -> "${pageLabel}加载失败，请稍后重试。"
    }
}
