package com.focuszone.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.focuszone.app.data.model.Task

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveTasks(): LiveData<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    suspend fun getPendingTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :done WHERE id = :taskId")
    suspend fun setCompleted(taskId: Long, done: Boolean)

    @Query("UPDATE tasks SET pomodoroCount = pomodoroCount + 1 WHERE id = :taskId")
    suspend fun incrementPomodoro(taskId: Long)
}
