package com.focuszone.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.model.Task
import com.focuszone.app.data.repository.FocusRepository
import com.focuszone.app.service.NotificationScheduler
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FocusRepository(application)
    private val notificationScheduler = NotificationScheduler(application)

    val tasks = repo.allTasks

    fun saveTask(
        id: Long = 0,
        name: String,
        repeatType: RepeatType,
        reminderHour: Int,
        reminderMinute: Int,
        specificDateMillis: Long?,
        customFocusMinutes: Int?
    ) {
        viewModelScope.launch {
            val task = Task(
                id = id,
                name = name,
                repeatType = repeatType,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                specificDateMillis = specificDateMillis,
                customFocusMinutes = customFocusMinutes
            )
            
            val finalId = if (id == 0L) {
                repo.insertTask(task)
            } else {
                repo.updateTask(task)
                id
            }

            if (repeatType != RepeatType.NONE) {
                notificationScheduler.scheduleTaskReminder(task.copy(id = finalId))
            } else {
                notificationScheduler.cancelTaskReminder(finalId)
            }
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            // Correction du nom de la méthode vers le repository
            repo.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            notificationScheduler.cancelTaskReminder(task.id)
            repo.deleteTask(task)
        }
    }

    fun getProgressPercent(tasks: List<Task>): Int {
        if (tasks.isEmpty()) return 0
        val done = tasks.count { it.isCompleted }
        return ((done.toFloat() / tasks.size) * 100).toInt()
    }

    fun getProgressText(tasks: List<Task>): String {
        val done = tasks.count { it.isCompleted }
        return "$done/${tasks.size}"
    }
}
