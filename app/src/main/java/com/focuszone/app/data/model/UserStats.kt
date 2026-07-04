package com.focuszone.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val todaySessions: Int = 0,
    val todayXp: Int = 0,
    val lastSessionDate: Long = 0L,
    val hasPenalty: Boolean = false,
    val missedSessions: Int = 0
) {
    companion object {
        fun xpRequiredForLevel(level: Int): Int = level * 1000

        fun levelFromXp(xp: Int): Int {
            var level = 1
            var required = 1000
            var accumulated = 0
            while (accumulated + required <= xp) {
                accumulated += required
                level++
                required = level * 1000
            }
            return level
        }

        fun xpForCurrentLevel(xp: Int): Int {
            var level = 1
            var required = 1000
            var accumulated = 0
            while (accumulated + required <= xp) {
                accumulated += required
                level++
                required = level * 1000
            }
            return xp - accumulated
        }

        val LEVEL_TITLES = mapOf(
            1 to "NOVICE",
            2 to "APPRENTI",
            3 to "GUERRIER",
            4 to "CHEVALIER",
            5 to "MAÎTRE",
            6 to "CHAMPION",
            7 to "ÉLITE WARRIOR",
            8 to "LÉGENDE",
            9 to "MYTHIQUE",
            10 to "DIEU DU FOCUS"
        )

        const val XP_PER_SESSION = 40
        const val XP_PER_TASK = 25
        const val XP_STREAK_BONUS = 10
        const val XP_PENALTY_LOSS = 15 // Réduction légère en cas de pénalité
    }
}
