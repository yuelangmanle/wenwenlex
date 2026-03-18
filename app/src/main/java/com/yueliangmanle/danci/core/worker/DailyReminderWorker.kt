package com.yueliangmanle.danci.core.worker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yueliangmanle.danci.MainActivity
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import java.time.Duration
import java.time.ZonedDateTime

const val DAILY_REMINDER_WORK_NAME = "daily_reminder"

private const val DAILY_REMINDER_CHANNEL_ID = "wenwenlex_daily_reminder"
private const val DAILY_REMINDER_CHANNEL_NAME = "文文Lex 学习提醒"
private const val DAILY_REMINDER_NOTIFICATION_ID = 2001

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        showReminderNotification(applicationContext)
        val settings = buildSettingsRepository(applicationContext).getSettings()
        DailyReminderScheduler(applicationContext).sync(
            settings = settings,
            now = ZonedDateTime.now(),
        )
        return Result.success()
    }
}

class DailyReminderScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
) {
    fun sync(
        settings: AppSettings,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): OneTimeWorkRequest? {
        if (!settings.reminderEnabled) {
            cancel()
            return null
        }
        val request = buildReminderWorkRequest(
            hour = settings.reminderHour,
            minute = settings.reminderMinute,
            now = now,
        )
        workManager.enqueueUniqueWork(
            DAILY_REMINDER_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return request
    }

    fun cancel() {
        workManager.cancelUniqueWork(DAILY_REMINDER_WORK_NAME)
    }
}

fun buildReminderWorkRequest(
    hour: Int,
    minute: Int,
    now: ZonedDateTime = ZonedDateTime.now(),
): OneTimeWorkRequest {
    val nextRunTime = nextReminderDateTime(
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        now = now,
    )
    val delay = Duration.between(now, nextRunTime).toMillis().coerceAtLeast(1L)
    return OneTimeWorkRequestBuilder<DailyReminderWorker>()
        .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
        .addTag(DAILY_REMINDER_WORK_NAME)
        .build()
}

internal fun nextReminderDateTime(
    hour: Int,
    minute: Int,
    now: ZonedDateTime,
): ZonedDateTime {
    val candidate = now
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
    return if (candidate.isAfter(now)) {
        candidate
    } else {
        candidate.plusDays(1)
    }
}

private fun showReminderNotification(context: Context) {
    if (!canPostNotifications(context)) {
        return
    }

    ensureReminderChannel(context)
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = Notification.Builder(context, DAILY_REMINDER_CHANNEL_ID)
        .setContentTitle("文文Lex 提醒你")
        .setContentText("今天的单词任务还在等你，先学一组再停。")
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(DAILY_REMINDER_NOTIFICATION_ID, notification)
}

private fun canPostNotifications(context: Context): Boolean =
    android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

private fun ensureReminderChannel(context: Context) {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        DAILY_REMINDER_CHANNEL_ID,
        DAILY_REMINDER_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "每日固定时间提醒开始学习"
    }
    notificationManager.createNotificationChannel(channel)
}
