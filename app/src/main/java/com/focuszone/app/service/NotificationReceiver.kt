package com.focuszone.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.focuszone.app.R
import com.focuszone.app.data.db.AppDatabase
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.repository.FocusRepository
import com.focuszone.app.ui.MainActivity
import com.focuszone.app.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.focuszone.TASK_REMINDER"
        const val ACTION_MARK_DONE     = "com.focuszone.MARK_DONE"
        const val EXTRA_TASK_ID        = "task_id"
        const val EXTRA_TASK_NAME      = "task_name"
        const val EXTRA_CUSTOM_FOCUS   = "custom_focus"
        const val CHANNEL_ID_TASKS     = "TASKS_CHANNEL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        
        when (intent.action) {
            ACTION_TASK_REMINDER -> {
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Mission"
                val customFocus = if (intent.hasExtra(EXTRA_CUSTOM_FOCUS)) {
                    intent.getIntExtra(EXTRA_CUSTOM_FOCUS, -1)
                } else null
                
                showTaskReminderNotification(context, taskId, taskName, customFocus)
                
                if (taskId != -1L) {
                    rescheduleNextIfNeeded(context, taskId)
                }
            }
            ACTION_MARK_DONE -> {
                if (taskId != -1L) {
                    markTaskAsDone(context, taskId)
                }
            }
        }
    }

    private fun rescheduleNextIfNeeded(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null && task.repeatType != RepeatType.NONE && task.repeatType != RepeatType.SPECIFIC_DATE) {
                NotificationScheduler(context).scheduleTaskReminder(task)
            }
        }
    }

    private fun markTaskAsDone(context: Context, taskId: Long) {
        val repo = FocusRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            repo.setTaskCompleted(taskId, true)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(taskId.toInt() + 2000)
        }
    }

    private fun showTaskReminderNotification(context: Context, taskId: Long, taskName: String, customFocus: Int?) {
        val prefs = PreferencesManager(context)
        val soundUriStr = prefs.notificationSoundUri
        val soundUri = if (soundUriStr != null) Uri.parse(soundUriStr) 
                       else Settings.System.DEFAULT_NOTIFICATION_URI

        createNotificationChannel(context, soundUri)

        val openIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                putExtra("open_tab", "tasks")
                putExtra(EXTRA_TASK_ID, taskId)
                customFocus?.let { putExtra(EXTRA_CUSTOM_FOCUS, it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 100,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TASKS)
            .setSmallIcon(R.drawable.ic_task_notif)
            .setContentTitle("⚔️ Mission en attente !")
            .setContentText(taskName)
            .setSound(soundUri)
            .setStyle(NotificationCompat.BigTextStyle().bigText("N'oublie pas : $taskName"))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (customFocus == null) {
            builder.addAction(R.drawable.ic_checkbox_checked, "C'EST FAIT ✅", donePendingIntent)
        } else {
            builder.setContentText("$taskName (Focus : $customFocus min)")
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.toInt() + 2000, builder.build())
    }

    private fun createNotificationChannel(context: Context, soundUri: Uri) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(CHANNEL_ID_TASKS)

        val channel = NotificationChannel(CHANNEL_ID_TASKS, "Rappels Missions", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rappels pour tes missions FocusZone"
            setSound(soundUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
