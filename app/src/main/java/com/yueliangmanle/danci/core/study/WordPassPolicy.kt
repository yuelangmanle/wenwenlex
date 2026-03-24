package com.yueliangmanle.danci.core.study

enum class WordPassStep {
    MEANING,
    RECALL,
    SPELLING,
    DONE,
}

data class WordPassAdvanceResult(
    val nextStep: WordPassStep,
)

class WordPassPolicy(
    private val spellingEnabled: Boolean,
) {
    fun advance(
        currentStep: WordPassStep,
        passed: Boolean,
    ): WordPassAdvanceResult {
        if (!passed) {
            return WordPassAdvanceResult(nextStep = WordPassStep.MEANING)
        }

        val nextStep = when (currentStep) {
            WordPassStep.MEANING -> WordPassStep.RECALL
            WordPassStep.RECALL -> if (spellingEnabled) WordPassStep.SPELLING else WordPassStep.DONE
            WordPassStep.SPELLING -> WordPassStep.DONE
            WordPassStep.DONE -> WordPassStep.DONE
        }
        return WordPassAdvanceResult(nextStep = nextStep)
    }
}
