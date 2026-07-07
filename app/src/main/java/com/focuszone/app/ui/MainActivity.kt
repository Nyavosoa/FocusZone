package com.focuszone.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.focuszone.app.R
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.model.Task
import com.focuszone.app.databinding.ActivityMainBinding
import com.focuszone.app.service.AppBlockerService
import com.focuszone.app.service.NotificationReceiver
import com.focuszone.app.service.PenaltyTriggerWorker
import com.focuszone.app.ui.timer.FocusOverlayFragment
import com.focuszone.app.util.PreferencesManager
import com.focuszone.app.viewmodel.TimerViewModel
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TimerViewModel by viewModels()
    private var isOverlayPending = false
    private var pendingTaskId: Long = -1L // ID de la mission en cours (notif ou manuel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)

        if (!prefs.hasSeenOnboarding) {
            startTutorial()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupObservers()
        setupListeners()
        handleIntent(intent)
        requestPermissions()
        requestIgnoreBatteryOptimizations()

        PenaltyTriggerWorker.scheduleInitialCheck(this)
    }

    private fun setupListeners() {
        binding.btnHelp.setOnClickListener { startTutorial(isManual = true) }
    }

    private fun startTutorial(isManual: Boolean = false) {
        val intent = Intent(this, OnboardingActivity::class.java)
        if (!isManual) {
            startActivity(intent)
            finish()
        } else {
            startActivity(intent)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(NotificationReceiver.EXTRA_TASK_ID)) {
                pendingTaskId = it.getLongExtra(NotificationReceiver.EXTRA_TASK_ID, -1L)
            }

            if (it.getStringExtra("open_tab") == "tasks") {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.navHostFragment) as NavHostFragment
                navHostFragment.navController.navigate(R.id.tasksFragment)
                binding.bottomNav.selectedItemId = R.id.tasksFragment
            }

            if (it.hasExtra(NotificationReceiver.EXTRA_CUSTOM_FOCUS)) {
                val customMins = it.getIntExtra(NotificationReceiver.EXTRA_CUSTOM_FOCUS, 25)
                launchFocusFullscreen(customMins)
            }
        }
    }

    // Ajout de taskId pour le lancement manuel depuis TasksFragment
    fun launchFocusFullscreen(minutes: Int, taskId: Long = -1L) {
        if (taskId != -1L) pendingTaskId = taskId
        viewModel.startCustomFocus(minutes)
    }

    fun navigateToTimer() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navHostFragment.navController.navigate(R.id.timerFragment)
        binding.bottomNav.selectedItemId = R.id.timerFragment
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun setupObservers() {
        viewModel.userStats.observe(this) { stats ->
            stats?.let {
                binding.tvStreak.text = getString(R.string.streak_format, it.currentStreak)
                val formatter = NumberFormat.getNumberInstance(Locale.US)
                binding.tvXpBadge.text = getString(R.string.xp_badge_format, formatter.format(it.totalXp))
                AppBlockerService.isBlocking = it.hasPenalty
            }
        }

        viewModel.allTasks.observe(this) { tasks ->
            val today = Calendar.getInstance()
            // Logique modifiée : on compte les tâches prévues aujourd'hui OU les tâches sans répétition non finies
            val tasksForToday = tasks.filter { isTaskDueToday(it, today) }
            val completedToday = tasksForToday.count { it.isCompleted }
            binding.tvMissionProgressTop.text = "🎯 $completedToday/${tasksForToday.size}"
        }

        viewModel.autoLaunchFocus.observe(this) { shouldLaunch ->
            if (shouldLaunch == true) {
                viewModel.consumeAutoLaunchFocusEvent()
                showFocusOverlay()
            }
        }

        viewModel.sessionCompleted.observe(this) { completed ->
            if (completed == true && pendingTaskId != -1L) {
                viewModel.markTaskCompleted(pendingTaskId)
                pendingTaskId = -1L
            }
        }
    }

    private fun isTaskDueToday(task: Task, today: Calendar): Boolean {
        // Si c'est une tâche sans répétition, elle est due si elle n'est pas faite
        // ou si elle a été faite aujourd'hui.
        if (task.repeatType == RepeatType.NONE) return !task.isCompleted
        
        return when (task.repeatType) {
            RepeatType.DAILY -> true
            RepeatType.MONDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
            RepeatType.TUESDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY
            RepeatType.WEDNESDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY
            RepeatType.THURSDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY
            RepeatType.FRIDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            RepeatType.SATURDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
            RepeatType.SUNDAY -> today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            RepeatType.SPECIFIC_DATE -> {
                val taskCal = Calendar.getInstance().apply { timeInMillis = task.specificDateMillis ?: 0 }
                taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                taskCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            }
            else -> false
        }
    }

    fun showFocusOverlay() {
        if (supportFragmentManager.isStateSaved) return
        val existing = supportFragmentManager.findFragmentByTag("focus_overlay")
        if (existing == null && !isOverlayPending) {
            isOverlayPending = true
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .add(android.R.id.content, FocusOverlayFragment(), "focus_overlay")
                .commitAllowingStateLoss()
            binding.root.post { isOverlayPending = false }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}
