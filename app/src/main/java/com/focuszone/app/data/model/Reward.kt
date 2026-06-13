package com.focuszone.app.data.model

data class Reward(
    val id: String,
    val icon: String,
    val name: String,
    val description: String,
    val unlockCondition: UnlockCondition,
    val isUnlocked: Boolean = false
)

sealed class UnlockCondition {
    data class Level(val requiredLevel: Int) : UnlockCondition()
    data class Sessions(val requiredSessions: Int) : UnlockCondition()
    data class Streak(val requiredDays: Int) : UnlockCondition()
    data class Xp(val requiredXp: Int) : UnlockCondition()
}

object RewardsList {
    fun getAllRewards(): List<Reward> = listOf(
        Reward("theme_cyber",   "🎨", "THÈME CYBER",    "Skin interface violet néon",           UnlockCondition.Level(2)),
        Reward("badge_elite",   "🏆", "BADGE ELITE",    "10 sessions sans pause",               UnlockCondition.Sessions(10)),
        Reward("sound_victory", "🔊", "SON VICTOIRE",   "Son de fin de focus épique",            UnlockCondition.Level(3)),
        Reward("flash_focus",   "⚡", "FLASH FOCUS",    "Streak de 7 jours requis",             UnlockCondition.Streak(7)),
        Reward("avatar_pro",    "🌟", "AVATAR PRO",     "Débloquer au niveau 8",                UnlockCondition.Level(8)),
        Reward("extra_break",   "🎮", "+5 MIN PAUSE",   "25 sessions complètes",                UnlockCondition.Sessions(25)),
        Reward("leaderboard",   "🏅", "LEADERBOARD",    "Top 10 du classement",                 UnlockCondition.Xp(5000)),
        Reward("boss_slayer",   "🎖", "BOSS SLAYER",    "50 focus totaux",                      UnlockCondition.Sessions(50)),
        Reward("theme_fire",    "🔥", "THÈME FIRE",     "Skin orange/rouge enflammé",           UnlockCondition.Level(5)),
        Reward("crown",         "👑", "COURONNE",       "Atteindre le niveau maximum",          UnlockCondition.Level(10))
    )
}
