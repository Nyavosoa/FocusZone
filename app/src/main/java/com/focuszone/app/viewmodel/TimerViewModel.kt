package com.focuszone.app.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.focuszone.app.R
import com.focuszone.app.data.model.UserStats
import com.focuszone.app.data.repository.FocusRepository
import com.focuszone.app.service.AppBlockerService
import com.focuszone.app.service.TimerService
import com.focuszone.app.ui.MainActivity
import com.focuszone.app.util.PreferencesManager
import kotlinx.coroutines.launch

enum class TimerMode { FOCUS, PAUSE }
enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FocusRepository(application)
    private val prefs = PreferencesManager(application)

    private val _focusMinutes = MutableLiveData(prefs.focusMinutes)
    val focusMinutes: LiveData<Int> = _focusMinutes

    private val _breakMinutes = MutableLiveData(prefs.breakMinutes)
    val breakMinutes: LiveData<Int> = _breakMinutes

    private val _timerMode = MutableLiveData(TimerMode.FOCUS)
    val timerMode: LiveData<TimerMode> = _timerMode

    private val _timerState = MutableLiveData(TimerState.IDLE)
    val timerState: LiveData<TimerState> = _timerState

    private val _remainingSeconds = MutableLiveData(prefs.focusMinutes * 60)
    val remainingSeconds: LiveData<Int> = _remainingSeconds

    private val _sessionCount = MutableLiveData(0)
    val sessionCount: LiveData<Int> = _sessionCount

    val userStats = repo.userStats

    private val _sessionCompleted = MutableLiveData<Boolean>(false)
    val sessionCompleted: LiveData<Boolean> = _sessionCompleted

    init {
        viewModelScope.launch { repo.ensureStatsExist() }

        TimerService.remainingSeconds.observeForever { secs ->
            _remainingSeconds.value = secs
        }
        TimerService.timerState.observeForever { state ->
            _timerState.value = state
            if (state == TimerState.FINISHED) onTimerFinished()
        }
        TimerService.timerMode.observeForever { mode ->
            _timerMode.value = mode
        }
    }

    fun startTimer() {
        val app = getApplication<Application>()
        val intent = Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS,
                if (_timerMode.value == TimerMode.FOCUS)
                    (_focusMinutes.value ?: 25) * 60
                else
                    (_breakMinutes.value ?: 5) * 60
            )
            putExtra(TimerService.EXTRA_MODE, _timerMode.value?.name)
        }
        app.startForegroundService(intent)
    }

    fun startCustomFocus(minutes: Int) {
        _timerMode.value = TimerMode.FOCUS
        val app = getApplication<Application>()
        val intent = Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS, minutes * 60)
            putExtra(TimerService.EXTRA_MODE, TimerMode.FOCUS.name)
        }
        app.startForegroundService(intent)
    }

    fun applyEmergencyPenalty() {
        viewModelScope.launch {
            repo.setPenalty(true)
            AppBlockerService.isBlocking = true
            showPenaltyNotification()
        }
    }

    private fun showPenaltyNotification() {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, TimerService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_penalty_nav)
            .setContentTitle("⚠️ Focus abandonné !")
            .setContentText("Tu as quitté FocusZone pendant ton focus. Tes jeux sont maintenant bloqués !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        manager.notify(1003, builder.build())
    }

    fun pauseTimer() {
        val app = getApplication<Application>()
        app.startService(Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        })
    }

    fun resumeTimer() {
        val app = getApplication<Application>()
        app.startService(Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        })
    }

    fun resetTimer() {
        val app = getApplication<Application>()
        app.startService(Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        })
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value =
            if (_timerMode.value == TimerMode.FOCUS) (_focusMinutes.value ?: 25) * 60
            else (_breakMinutes.value ?: 5) * 60
    }

    fun togglePlayPause() {
        when (_timerState.value) {
            TimerState.IDLE, TimerState.FINISHED -> startTimer()
            TimerState.RUNNING -> pauseTimer()
            TimerState.PAUSED -> resumeTimer()
            null -> startTimer()
        }
    }

    fun switchMode(mode: TimerMode) {
        if (_timerState.value == TimerState.RUNNING) return
        _timerMode.value = mode
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value =
            if (mode == TimerMode.FOCUS) (_focusMinutes.value ?: 25) * 60
            else (_breakMinutes.value ?: 5) * 60
    }

    fun setFocusMinutes(minutes: Int) {
        val newVal = minutes.coerceIn(1, 240)
        _focusMinutes.value = newVal
        prefs.focusMinutes = newVal
        if (_timerMode.value == TimerMode.FOCUS && _timerState.value != TimerState.RUNNING) {
            _remainingSeconds.value = newVal * 60
        }
    }

    fun setBreakMinutes(minutes: Int) {
        val newVal = minutes.coerceIn(1, 60)
        _breakMinutes.value = newVal
        prefs.breakMinutes = newVal
        if (_timerMode.value == TimerMode.PAUSE && _timerState.value != TimerState.RUNNING) {
            _remainingSeconds.value = newVal * 60
        }
    }

    fun adjustFocusMinutes(delta: Int) {
        setFocusMinutes((_focusMinutes.value ?: 25) + delta)
    }

    fun adjustBreakMinutes(delta: Int) {
        setBreakMinutes((_breakMinutes.value ?: 5) + delta)
    }

    private fun onTimerFinished() {
        if (_timerMode.value == TimerMode.FOCUS) {
            _sessionCount.value = (_sessionCount.value ?: 0) + 1
            val minutes = _focusMinutes.value ?: 25
            viewModelScope.launch {
                repo.onSessionCompleted(minutes)
                // Changement ici : On utilise le thread principal pour signaler la réussite
                _sessionCompleted.postValue(true)
            }
        }
    }

    fun consumeSessionCompletedEvent() {
        // Remet à false pour éviter que l'animation ne se relance à la navigation
        _sessionCompleted.value = false
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    fun getProgress(seconds: Int): Float {
        val total = if (_timerMode.value == TimerMode.FOCUS)
            (_focusMinutes.value ?: 25) * 60f
        else (_breakMinutes.value ?: 5) * 60f
        return if (total <= 0) 1f else seconds / total
    }

    fun getLevelTitle(level: Int): String =
        UserStats.LEVEL_TITLES[level.coerceIn(1, 10)] ?: "FOCUS MASTER"

    fun getXpForCurrentLevel(totalXp: Int): Int = UserStats.xpForCurrentLevel(totalXp)
    fun getXpRequiredForLevel(level: Int): Int = UserStats.xpRequiredForLevel(level)
}
