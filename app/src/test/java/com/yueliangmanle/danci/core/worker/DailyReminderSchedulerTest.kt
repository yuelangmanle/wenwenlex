package com.yueliangmanle.danci.core.worker

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyReminderSchedulerTest {
    @Test
    fun schedulesReminderAtUserConfiguredTime() {
        val request = buildReminderWorkRequest(
            hour = 21,
            minute = 30,
            now = ZonedDateTime.of(2026, 3, 18, 20, 0, 0, 0, ZoneId.of("Asia/Shanghai")),
        )

        assertTrue(request.workSpec.initialDelay > 0L)
    }
}
