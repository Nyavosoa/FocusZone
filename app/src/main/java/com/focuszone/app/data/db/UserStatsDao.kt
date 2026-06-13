package com.focuszone.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.focuszone.app.data.model.UserStats

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getStats(): LiveData<UserStats>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getStatsOnce(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: UserStats)

    @Update
    suspend fun updateStats(stats: UserStats)

    @Query("UPDATE user_stats SET totalXp = totalXp + :xp, todayXp = todayXp + :xp WHERE id = 1")
    suspend fun addXp(xp: Int)

    @Query("UPDATE user_stats SET totalSessions = totalSessions + 1, todaySessions = todaySessions + 1, totalFocusMinutes = totalFocusMinutes + :minutes WHERE id = 1")
    suspend fun incrementSession(minutes: Int)

    @Query("UPDATE user_stats SET hasPenalty = :hasPenalty WHERE id = 1")
    suspend fun setPenalty(hasPenalty: Boolean)

    @Query("UPDATE user_stats SET currentStreak = :streak WHERE id = 1")
    suspend fun updateStreak(streak: Int)
}
