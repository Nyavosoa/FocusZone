package com.focuszone.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focuszone.app.data.db.AppDatabase
import com.focuszone.app.ui.PenaltyBlockActivity
import com.focuszone.app.util.PreferencesManager
import kotlinx.coroutines.*

class AppBlockerService : AccessibilityService() {

    companion object {
        var isBlocking = false
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: PreferencesManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(applicationContext)
        
        // Synchronisation initiale de l'état de blocage
        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val stats = db.userStatsDao().getStatsOnce()
            isBlocking = stats?.hasPenalty == true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // On ne bloque QUE les packages sélectionnés par l'utilisateur
        val userBlockedPackages = prefs.blockedPackages
        
        if (isBlocking && userBlockedPackages.isNotEmpty() && packageName in userBlockedPackages) {
            scope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val stats = db.userStatsDao().getStatsOnce()
                
                // Double vérification avec la base de données
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
