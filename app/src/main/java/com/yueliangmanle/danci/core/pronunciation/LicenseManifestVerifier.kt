package com.yueliangmanle.danci.core.pronunciation

data class LicenseVerificationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
)

object LicenseManifestVerifier {
    fun verify(manifest: NativeVoicePackManifest): LicenseVerificationResult {
        if (manifest.isEmpty()) {
            return LicenseVerificationResult(isValid = true)
        }

        val licenses = manifest.licenses
            .map(String::trim)
            .filter(String::isNotEmpty)

        if (licenses.isEmpty()) {
            return LicenseVerificationResult(
                isValid = false,
                errorMessage = "原生语音包 manifest 缺少 licenses 声明。",
            )
        }

        return LicenseVerificationResult(isValid = true)
    }

    fun requireValid(manifest: NativeVoicePackManifest) {
        val result = verify(manifest)
        check(result.isValid) { result.errorMessage ?: "语音包许可证校验失败。" }
    }
}
