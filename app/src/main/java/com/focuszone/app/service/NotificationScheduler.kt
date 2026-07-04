package com.focuszone.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.model.Task
import java.util.*

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskReminder(task: Task) {
        val triggerAt = getNextTriggerMillis(task) ?: return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_TASK_REMINDER
            putExtra(NotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(NotificationReceiver.EXTRA_TASK_NAME, task.name)
            task.customFocusMinutes?.let {
                putExtra(NotificationReceiver.EXTRA_CUSTOM_FOCUS, it)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    private fun getNextTriggerMillis(task: Task): Long? {
        if (task.repeatType == RepeatType.SPECIFIC_DATE) {
            return if (task.specificDateMillis != null && task.specificDateMillis > System.currentTimeMillis()) 
                task.specificDateMillis else null
        }
        if (task.repeatType == RepeatType.NONE) return null

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.reminderHour)
            set(Calendar.MINUTE, task.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (true) {
            val isCorrectDay = when (task.repeatType) {
                RepeatType.DAILY -> true
                RepeatType.MONDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                RepeatType.TUESDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY
                RepeatType.WEDNESDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY
                RepeatType.THURSDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY
                RepeatType.FRIDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                RepeatType.SATURDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                RepeatType.SUNDAY -> target.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                else -> false
            }

            if (isCorrectDay && target.after(now)) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
