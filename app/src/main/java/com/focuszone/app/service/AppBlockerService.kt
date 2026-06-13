package com.focuszone.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focuszone.app.data.db.AppDatabase
import com.focuszone.app.ui.PenaltyBlockActivity
import kotlinx.coroutines.*

class AppBlockerService : AccessibilityService() {

    // List of game package names to block during penalty
    companion object {
        val BLOCKED_GAME_PACKAGES = setOf(
            "com.mobile.legends",
            "com.garena.game.freefire",
            "com.dts.freefiremax",
            "com.ea.gp.fifamobile",
            "com.riotgames.league.wildrift",
            "com.activision.callofduty.shooter",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.mojang.minecraftpe",
            "com.epicgames.fortnite",
            "com.roblox.client",
            "com.miHoYo.GenshinImpact"
        )

        var isBlocking = false
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Initialisation automatique du blocage au démarrage du service
        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val stats = db.userStatsDao().getStatsOnce()
            isBlocking = stats?.hasPenalty == true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName in BLOCKED_GAME_PACKAGES && isBlocking) {
            // Double vérification avec la base de données pour plus de sécurité
            scope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val stats = db.userStatsDao().getStatsOnce()
                if (stats?.hasPenalty == true) {
                    withContext(Dispatchers.Main) {
                        showPenaltyScreen()
                    }
                }
            }
        }
    }

    private fun showPenaltyScreen() {
        val intent = Intent(this, PenaltyBlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
