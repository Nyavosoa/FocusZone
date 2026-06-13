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

    var isAccessibilityEnabled: Boolean
        get() = prefs.getBoolean("accessibility_enabled", false)
        set(value) = prefs.edit { putBoolean("accessibility_enabled", value) }
}
