package com.errorzero.app.review

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.errorzero.app.ErrorZeroApp
import com.errorzero.app.MainActivity
import com.errorzero.app.R
import com.errorzero.app.data.ReminderConfig
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val CHANNEL_ID = "review_reminder"
    private const val WORK_NAME = "daily_review_reminder"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "重做提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "每天提醒需要重做的错题"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** replace = false 时保留已有任务，避免每次启动都重置周期 */
    fun schedule(context: Context, config: ReminderConfig, replace: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!config.enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = ZonedDateTime.now()
        var target = now.withHour(config.hour).withMinute(config.minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, target).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notifyDue(context: Context, count: Int) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_REVIEW, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.cinnabar))
            .setContentTitle("今日待重做")
            .setContentText("有 $count 道错题等待重做")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ErrorZeroApp
        val count = app.repository.countDueToday()
        if (count > 0) Reminders.notifyDue(applicationContext, count)
        return Result.success()
    }
}
