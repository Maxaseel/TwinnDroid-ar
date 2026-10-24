package com.twinndroid

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.twinndroid.data.Settings
import com.twinndroid.install.ShizukuInstaller
import java.util.concurrent.TimeUnit

/**
 * Every few hours: find clones whose original app was updated. With auto-update + Shizuku they are
 * rebuilt and reinstalled silently; otherwise a notification offers a one-tap update in the app.
 */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val outdated = CloneOperations.outdated(ctx)
        if (outdated.isEmpty()) return Result.success()

        val settings = Settings(ctx)
        val remaining = if (settings.autoUpdate && settings.useShizuku && ShizukuInstaller.hasPermission()) {
            outdated.filterNot { c ->
                runCatching { CloneOperations.buildAndInstall(ctx, c, allowPrompt = false) {}.success }.getOrDefault(false)
            }
        } else {
            outdated
        }
        if (remaining.isNotEmpty()) notify(ctx, remaining.map { it.label })
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "updates"
        private const val WORK = "clone-update-check"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun createChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_updates), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        private fun notify(context: Context, labels: List<String>) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.resources.getQuantityString(R.plurals.notification_clone_update, labels.size, labels.size))
                .setContentText(labels.joinToString())
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, n)
        }
    }
}
