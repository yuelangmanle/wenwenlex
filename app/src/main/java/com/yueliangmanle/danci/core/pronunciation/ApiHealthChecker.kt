package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.isMiMoTtsCompatible

class ApiHealthChecker(
    private val miMoTtsProvider: MiMoTtsProvider = MiMoTtsProvider(),
) {
    suspend fun check(
        profile: AiProviderProfile,
        apiKey: String,
        voice: String = "default_en",
        text: String = "Hello, this is a pronunciation health check.",
        style: String? = "Calm",
    ): CloudTtsHealthCheckResult {
        require(apiKey.isNotBlank()) { "当前档案还没有保存 API Key。" }
        return when {
            profile.isMiMoTtsCompatible() -> miMoTtsProvider.healthCheck(
                profile = profile,
                apiKey = apiKey,
                voice = voice,
                text = text,
                style = style,
            )
            else -> CloudTtsHealthCheckResult(
                success = false,
                summary = "当前档案不是受支持的云端 TTS 档案。",
            )
        }
    }
}
