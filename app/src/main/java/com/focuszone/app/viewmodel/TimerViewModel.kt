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
import com.focuszone.app.service.PenaltyTriggerWorker
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

    private val _sessionCount = MutableLiveData(1)
    val sessionCount: LiveData<Int> = _sessionCount

    val userStats = repo.userStats
    val allTasks = repo.allTasks

    private val _sessionCompleted = MutableLiveData(false)
    val sessionCompleted: LiveData<Boolean> = _sessionCompleted

    private val _autoLaunchFocus = MutableLiveData(false)
    val autoLaunchFocus: LiveData<Boolean> = _autoLaunchFocus

    private var isOverlayTriggeredForThisSession = false
    private var currentSessionDuration = 25

    private val remainingSecondsObserver = Observer<Int> { secs ->
        _remainingSeconds.postValue(secs)
    }
    
    private val timerStateObserver = Observer<TimerState> { state ->
        handleStateAndModeChange(state, TimerService.timerMode.value)
    }
    
    private val timerModeObserver = Observer<TimerMode> { mode ->
        handleStateAndModeChange(TimerService.timerState.value, mode)
    }

    private val sessionCountObserver = Observer<Int> { count ->
        _sessionCount.postValue(count)
    }

    init {
        viewModelScope.launch { repo.ensureStatsExist() }

        TimerService.remainingSeconds.observeForever(remainingSecondsObserver)
        TimerService.timerState.observeForever(timerStateObserver)
        TimerService.timerMode.observeForever(timerModeObserver)
        TimerService.sessionCount.observeForever(sessionCountObserver)
    }

    private fun handleStateAndModeChange(state: TimerState?, mode: TimerMode?) {
        val newState = state ?: TimerState.IDLE
        val newMode = mode ?: TimerMode.FOCUS
        
        val oldState = _timerState.value
        val oldMode = _timerMode.value

        if (newState == oldState && newMode == oldMode) return

        _timerState.value = newState
        _timerMode.value = newMode

        if (newState == TimerState.RUNNING && newMode == TimerMode.FOCUS) {
            if (!isOverlayTriggeredForThisSession) {
                isOverlayTriggeredForThisSession = true
                _autoLaunchFocus.value = true
            }
        } else if (newState == TimerState.IDLE || newState == TimerState.FINISHED) {
            isOverlayTriggeredForThisSession = false
        }

        if (newState == TimerState.FINISHED && newMode == TimerMode.FOCUS) {
            if (oldState != TimerState.FINISHED) {
                onTimerFinished()
            }
        }
    }

    fun markTaskCompleted(taskId: Long) {
        viewModelScope.launch {
            repo.setTaskCompleted(taskId, true)
        }
    }

    fun startTimer() {
        currentSessionDuration = if (_timerMode.value == TimerMode.FOCUS) (_focusMinutes.value ?: 25) else (_breakMinutes.value ?: 5)
        val app = getApplication<Application>()
        val intent = Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS, currentSessionDuration * 60)
            putExtra(TimerService.EXTRA_MODE, _timerMode.value?.name)
        }
        app.startForegroundService(intent)
    }

    fun startCustomFocus(minutes: Int) {
        // Force instant state for UI
        currentSessionDuration = minutes
        _timerState.value = TimerState.RUNNING
        _timerMode.value = TimerMode.FOCUS
        _remainingSeconds.value = minutes * 60
        isOverlayTriggeredForThisSession = true
        _autoLaunchFocus.value = true

        val app = getApplication<Application>()
        val intent = Intent(app, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS, minutes * 60)
            putExtra(TimerService.EXTRA_MODE, TimerMode.FOCUS.name)
        }
        app.startForegroundService(intent)
    }

    fun skipPause() {
        if (_timerMode.value == TimerMode.PAUSE) {
            val app = getApplication<Application>()
            app.startService(Intent(app, TimerService::class.java).apply {
                action = TimerService.ACTION_SKIP_PAUSE
            })
        }
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
        val current = _focusMinutes.value ?: 25
        setFocusMinutes(current + delta)
    }

    fun adjustBreakMinutes(delta: Int) {
        val current = _breakMinutes.value ?: 5
        setBreakMinutes(current + delta)
    }

    fun setPenaltyCheckTime(hour: Int, minute: Int) {
        prefs.penaltyCheckHour = hour
        prefs.penaltyCheckMinute = minute
        PenaltyTriggerWorker.scheduleInitialCheck(getApplication())
    }

    fun togglePlayPause() {
        val app = getApplication<Application>()
        when (_timerState.value) {
            TimerState.IDLE, TimerState.FINISHED -> startTimer()
            TimerState.RUNNING -> app.startService(Intent(app, TimerService::class.java).apply { action = TimerService.ACTION_PAUSE })
            TimerState.PAUSED -> app.startService(Intent(app, TimerService::class.java).apply { action = TimerService.ACTION_RESUME })
            else -> startTimer()
        }
    }

    fun resetTimer() {
        val app = getApplication<Application>()
        app.startService(Intent(app, TimerService::class.java).apply { action = TimerService.ACTION_STOP })
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value = if (_timerMode.value == TimerMode.FOCUS) (_focusMinutes.value ?: 25) * 60 else (_breakMinutes.value ?: 5) * 60
        isOverlayTriggeredForThisSession = false
    }

    fun switchMode(mode: TimerMode) {
        if (_timerState.value == TimerState.RUNNING) return
        _timerMode.value = mode
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value = if (mode == TimerMode.FOCUS) (_focusMinutes.value ?: 25) * 60 else (_breakMinutes.value ?: 5) * 60
        isOverlayTriggeredForThisSession = false
    }

    private fun onTimerFinished() {
        viewModelScope.launch {
            repo.onSessionCompleted(currentSessionDuration)
            _sessionCompleted.value = true
        }
    }

    fun consumeSessionCompletedEvent() { _sessionCompleted.value = false }
    fun consumeAutoLaunchFocusEvent() { _autoLaunchFocus.value = false }

    fun formatTime(seconds: Int): String { return "%02d:%02d".format(seconds / 60, seconds % 60) }
    fun getProgress(seconds: Int): Float {
        val total = if (_timerMode.value == TimerMode.FOCUS) (currentSessionDuration) * 60f else (currentSessionDuration) * 60f
        return if (total <= 0) 1f else seconds / total
    }
    fun getLevelTitle(level: Int): String = UserStats.LEVEL_TITLES[level.coerceIn(1, 10)] ?: "FOCUS MASTER"
    fun getXpForCurrentLevel(totalXp: Int): Int = UserStats.xpForCurrentLevel(totalXp)
    fun getXpRequiredForLevel(level: Int): Int = UserStats.xpRequiredForLevel(level)

    fun applyEmergencyPenalty() {
        viewModelScope.launch {
            repo.applyPenaltyWithXpLoss()
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
            .setContentText("Tu as quitté FocusZone. Tes jeux sont bloqués !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        manager.notify(1003, builder.build())
    }

    override fun onCleared() {
        super.onCleared()
        TimerService.remainingSeconds.removeObserver(remainingSecondsObserver)
        TimerService.timerState.removeObserver(timerStateObserver)
        TimerService.timerMode.removeObserver(timerModeObserver)
        TimerService.sessionCount.removeObserver(sessionCountObserver)
    }
}
