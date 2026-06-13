package com.focuszone.app.service

import android.content.Context
import androidx.work.*
import com.focuszone.app.data.db.AppDatabase
import com.focuszone.app.data.model.UserStats
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PenaltyTriggerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val stats = db.userStatsDao().getStatsOnce() ?: return Result.success()
        val pendingTasksCount = db.taskDao().getPendingTasksCount()

        // Vérification : si 0 session aujourd'hui OU s'il reste des missions non terminées -> pénalité
        val todaySessions = stats.todaySessions
        if ((todaySessions == 0 || pendingTasksCount > 0) && !stats.hasPenalty) {
            db.userStatsDao().setPenalty(true)
            AppBlockerService.isBlocking = true
            
            val reason = if (todaySessions == 0) 
                "Aucune session de focus aujourd'hui." 
            else 
                "Tu as encore $pendingTasksCount mission(s) non terminée(s)."
                
            showPenaltyNotification(context, reason)
        }

        // Programmer la prochaine vérification pour demain
        scheduleTomorrowCheck(context)
        return Result.success()
    }

    private fun showPenaltyNotification(context: Context, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                android.app.NotificationManager

        val channel = android.app.NotificationChannel(
            "PENALTY_CHANNEL",
            "Pénalités FocusZone",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val notification = androidx.core.app.NotificationCompat.Builder(context, "PENALTY_CHANNEL")
            .setSmallIcon(com.focuszone.app.R.drawable.ic_penalty_nav)
            .setContentTitle("🚫 Pénalité activée !")
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(9001, notification)
    }

    companion object {
        private const val WORK_NAME = "penalty_daily_check"
        private const val CHECK_HOUR = 20 // 20h00

        fun scheduleInitialCheck(context: Context) {
            val delay = getDelayUntilCheckTime()
            val request = OneTimeWorkRequestBuilder<PenaltyTriggerWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun scheduleTomorrowCheck(context: Context) {
            val delay = getDelayUntilCheckTime() + TimeUnit.HOURS.toMillis(24)
            val request = OneTimeWorkRequestBuilder<PenaltyTriggerWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun getDelayUntilCheckTime(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, CHECK_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (now.after(target)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
