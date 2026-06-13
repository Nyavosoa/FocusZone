package com.focuszone.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RepeatType {
    NONE, DAILY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, SPECIFIC_DATE
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false,
    val repeatType: RepeatType = RepeatType.NONE,
    val reminderHour: Int = 9,        // 0-23
    val reminderMinute: Int = 0,      // 0-59
    val specificDateMillis: Long? = null, // for SPECIFIC_DATE
    val customFocusMinutes: Int? = null,  // Custom focus duration for this task
    val pomodoroCount: Int = 0,       // completed pomodoros
    val lastTriggeredDate: Long = 0,  // Last time the reminder was sent (as day-level timestamp)
    val createdAt: Long = System.currentTimeMillis()
)
