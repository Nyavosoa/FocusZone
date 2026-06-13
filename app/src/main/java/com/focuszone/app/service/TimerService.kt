package com.focuszone.app.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.focuszone.app.R
import com.focuszone.app.ui.MainActivity
import com.focuszone.app.viewmodel.TimerMode
import com.focuszone.app.viewmodel.TimerState
import com.focuszone.app.util.PreferencesManager
import kotlinx.coroutines.*

class TimerService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_SECONDS = "EXTRA_SECONDS"
        const val EXTRA_MODE = "EXTRA_MODE"
        const val CHANNEL_ID = "TIMER_CHANNEL"
        const val NOTIF_ID = 1001
        const val FINISH_NOTIF_ID = 1002

        val remainingSeconds = MutableLiveData(0)
        val timerState = MutableLiveData(TimerState.IDLE)
        val timerMode = MutableLiveData(TimerMode.FOCUS)
    }

    private var timerJob: Job? = null
    private var currentSeconds = 0
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentSeconds = intent.getIntExtra(EXTRA_SECONDS, 25 * 60)
                val modeStr = intent.getStringExtra(EXTRA_MODE) ?: TimerMode.FOCUS.name
                timerMode.postValue(TimerMode.valueOf(modeStr))
                remainingSeconds.postValue(currentSeconds)
                startCountdown()
                startForeground(NOTIF_ID, buildNotification(currentSeconds))
            }
            ACTION_PAUSE -> {
                timerJob?.cancel()
                timerState.postValue(TimerState.PAUSED)
                updateNotification(currentSeconds)
            }
            ACTION_RESUME -> {
                startCountdown()
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown() {
        timerState.postValue(TimerState.RUNNING)
        timerJob = serviceScope.launch {
            while (currentSeconds > 0) {
                delay(1000L)
                currentSeconds--
                remainingSeconds.postValue(currentSeconds)
                withContext(Dispatchers.Main) {
                    updateNotification(currentSeconds)
                }
            }
            handleTimerFinished()
        }
    }

    private fun handleTimerFinished() {
        val oldMode = timerMode.value
        
        // 1. On signale la fin (déclenche l'animation dans l'UI)
        timerState.postValue(TimerState.FINISHED)
        sendFinishNotification(oldMode)

        serviceScope.launch {
            // 2. SI C'ÉTAIT UN FOCUS : On attend 5 secondes pour l'animation de félicitations
            if (oldMode == TimerMode.FOCUS) {
                delay(5500L) // Un peu plus de 5s pour être sûr que l'UI a fini
            }

            // 3. Basculer sur le mode suivant
            val newMode = if (oldMode == TimerMode.FOCUS) TimerMode.PAUSE else TimerMode.FOCUS
            val prefs = PreferencesManager(this@TimerService)
            val nextDuration = if (newMode == TimerMode.FOCUS) prefs.focusMinutes * 60 else prefs.breakMinutes * 60

            currentSeconds = nextDuration
            timerMode.postValue(newMode)
            remainingSeconds.postValue(currentSeconds)

            // 4. Lancer automatiquement le cycle suivant
            startCountdown()
        }
    }

    private fun sendFinishNotification(finishedMode: TimerMode?) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val title = if (finishedMode == TimerMode.FOCUS) "Focus terminé !" else "Pause terminée !"
        val text = if (finishedMode == TimerMode.FOCUS) "C'est l'heure de la pause ☕" else "Prêt pour une nouvelle session ? ⚔️"
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        manager.notify(FINISH_NOTIF_ID, builder.build())
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerState.postValue(TimerState.IDLE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_timer),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer et alertes de sessions FocusZone"
            enableVibration(true)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(seconds: Int): Notification {
        val mm = seconds / 60
        val ss = seconds % 60
        val timeStr = "%02d:%02d".format(mm, ss)
        val modeLabel = if (timerMode.value == TimerMode.FOCUS) "Focus" else "Pause"
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusZone — $modeLabel")
            .setContentText("$timeStr restantes")
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(seconds: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(seconds))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
}
