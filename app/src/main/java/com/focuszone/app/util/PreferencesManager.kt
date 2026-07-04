package com.focuszone.app.util

import android.content.Context
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("focuszone_prefs", Context.MODE_PRIVATE)

    var focusMinutes: Int
        get() = prefs.getInt("focus_minutes", 25)
        set(value) = prefs.edit { putInt("focus_minutes", value) }

    var breakMinutes: Int
        get() = prefs.getInt("break_minutes", 5)
        set(value) = prefs.edit { putInt("break_minutes", value) }

    var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean("seen_onboarding", false)
        set(value) = prefs.edit { putBoolean("seen_onboarding", value) }

    var penaltyCheckHour: Int
        get() = prefs.getInt("penalty_check_hour", 20)
        set(value) = prefs.edit { putInt("penalty_check_hour", value) }

    var penaltyCheckMinute: Int
        get() = prefs.getInt("penalty_check_minute", 0)
        set(value) = prefs.edit { putInt("penalty_check_minute", value) }

    var notificationSoundUri: String?
        get() = prefs.getString("notif_sound_uri", null)
        set(value) = prefs.edit { putString("notif_sound_uri", value) }

    var blockedPackages: Set<String>
        get() = prefs.getStringSet("blocked_packages", emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit {
                if (value.isEmpty()) {
                    remove("blocked_packages")
                } else {
                    // Toujours créer une nouvelle copie pour SharedPreferences
                    putStringSet("blocked_packages", value.toSet())
                }
            }
        }
}
