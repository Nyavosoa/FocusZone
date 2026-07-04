package com.focuszone.app.data.repository

import android.content.Context
import com.focuszone.app.data.db.AppDatabase
import com.focuszone.app.data.model.Task
import com.focuszone.app.data.model.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FocusRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val statsDao = db.userStatsDao()

    // ---- Tasks ----
    val allTasks = taskDao.getAllTasks()
    val activeTasks = taskDao.getActiveTasks()

    suspend fun insertTask(task: Task): Long = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun setTaskCompleted(taskId: Long, done: Boolean) = withContext(Dispatchers.IO) {
        taskDao.setCompleted(taskId, done)
        
        // Gagner de l'XP si la tâche est marquée comme terminée
        if (done) {
            val stats = statsDao.getStatsOnce() ?: return@withContext
            val xpGained = UserStats.XP_PER_TASK
            val newTotalXp = stats.totalXp + xpGained
            val newLevel = UserStats.levelFromXp(newTotalXp)
            
            statsDao.updateStats(stats.copy(
                totalXp = newTotalXp,
                level = newLevel,
                todayXp = stats.todayXp + xpGained,
                hasPenalty = false, // Faire une tâche peut aussi lever la pénalité
                missedSessions = 0
            ))
        }
    }

    suspend fun incrementPomodoro(taskId: Long) = withContext(Dispatchers.IO) {
        taskDao.incrementPomodoro(taskId)
    }

    suspend fun getCompletedCount(): Int = withContext(Dispatchers.IO) {
        taskDao.getCompletedCount()
    }

    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        taskDao.getTotalCount()
    }

    // ---- Stats ----
    val userStats = statsDao.getStats()

    suspend fun getStatsOnce(): UserStats? = withContext(Dispatchers.IO) {
        statsDao.getStatsOnce()
    }

    suspend fun applyPenaltyWithXpLoss() = withContext(Dispatchers.IO) {
        val stats = statsDao.getStatsOnce() ?: return@withContext
        // Réduction légère d'XP définie dans UserStats.kt (15 XP)
        val newTotalXp = (stats.totalXp - UserStats.XP_PENALTY_LOSS).coerceAtLeast(0)
        
        statsDao.updateStats(stats.copy(
            hasPenalty = true,
            totalXp = newTotalXp,
            todayXp = stats.todayXp - UserStats.XP_PENALTY_LOSS
        ))
    }

    suspend fun onSessionCompleted(focusMinutes: Int) = withContext(Dispatchers.IO) {
        val stats = statsDao.getStatsOnce() ?: return@withContext
        val xpGained = UserStats.XP_PER_SESSION + (stats.currentStreak * UserStats.XP_STREAK_BONUS)
        val newTotalXp = stats.totalXp + xpGained
        val newLevel = UserStats.levelFromXp(newTotalXp)

        // Check and update streak
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val lastDay = stats.lastSessionDate / (1000 * 60 * 60 * 24)
        val newStreak = when {
            lastDay == today -> stats.currentStreak
            lastDay == today - 1 -> stats.currentStreak + 1
            else -> 1
        }

        statsDao.updateStats(stats.copy(
            totalXp = newTotalXp,
            level = newLevel,
            currentStreak = newStreak,
            bestStreak = maxOf(stats.bestStreak, newStreak),
            totalSessions = stats.totalSessions + 1,
            todaySessions = stats.todaySessions + 1,
            todayXp = stats.todayXp + xpGained,
            totalFocusMinutes = stats.totalFocusMinutes + focusMinutes,
            lastSessionDate = System.currentTimeMillis(),
            hasPenalty = false,
            missedSessions = 0
        ))
    }

    suspend fun setPenalty(hasPenalty: Boolean) = withContext(Dispatchers.IO) {
        statsDao.setPenalty(hasPenalty)
    }

    suspend fun ensureStatsExist() = withContext(Dispatchers.IO) {
        val existing = statsDao.getStatsOnce()
        if (existing == null) {
            statsDao.insertStats(UserStats())
        }
    }
}
