package com.focuszone.app.service

import android.content.Context
import androidx.work.*
import com.focuszone.app.data.db.AppDatabase
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.model.Task
import com.focuszone.app.util.PreferencesManager
import java.util.*
import java.util.concurrent.TimeUnit

class PenaltyTriggerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val stats = db.userStatsDao().getStatsOnce() ?: return Result.success()
        
        val allPendingTasks = db.taskDao().getActiveTasksSync()
        val today = Calendar.getInstance()
        
        val tasksDueToday = allPendingTasks.filter { task ->
            isTaskDueToday(task, today)
        }

        val todaySessions = stats.todaySessions
        
        if ((todaySessions == 0 || tasksDueToday.isNotEmpty()) && !stats.hasPenalty) {
            db.userStatsDao().setPenalty(true)
            AppBlockerService.isBlocking = true
            
            val reason = if (todaySessions == 0) 
                "Aucun focus aujourd'hui." 
            else 
                "Il te reste ${tasksDueToday.size} mission(s) du jour non terminée(s)."
                
            showPenaltyNotification(context, reason)
        }

        // Programmer la prochaine vérification (demain à la même heure)
        scheduleInitialCheck(context) 
        return Result.success()
    }

    private fun isTaskDueToday(task: Task, today: Calendar): Boolean {
        if (task.isCompleted) return false

        return when (task.repeatType) {
            RepeatType.NONE -> false 
            RepeatType.DAILY -> true
            RepeatType.MONDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
            RepeatType.TUESDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY
            RepeatType.WEDNESDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY
            RepeatType.THURSDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY
            RepeatType.FRIDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            RepeatType.SATURDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
            RepeatType.SUNDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            RepeatType.SPECIFIC_DATE -> {
                val taskCal = Calendar.getInstance().apply { timeInMillis = task.specificDateMillis ?: 0 }
                taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                taskCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            }
        }
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

        fun scheduleInitialCheck(context: Context) {
            val delay = getDelayUntilCheckTime(context)
            val request = OneTimeWorkRequestBuilder<PenaltyTriggerWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        private fun getDelayUntilCheckTime(context: Context): Long {
            val prefs = PreferencesManager(context)
            val checkHour = prefs.penaltyCheckHour
            val checkMinute = prefs.penaltyCheckMinute
            
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, checkHour)
                set(Calendar.MINUTE, checkMinute)
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
