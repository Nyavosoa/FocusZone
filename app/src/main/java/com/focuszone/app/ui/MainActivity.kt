package com.focuszone.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.focuszone.app.R
import com.focuszone.app.databinding.ActivityMainBinding
import com.focuszone.app.service.AppBlockerService
import com.focuszone.app.service.NotificationReceiver
import com.focuszone.app.service.PenaltyTriggerWorker
import com.focuszone.app.util.PreferencesManager
import com.focuszone.app.viewmodel.TimerViewModel
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)

        // Redirection vers l'onboarding si c'est le premier lancement
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

        // Programmer la vérification quotidienne de la pénalité à 20:00
        PenaltyTriggerWorker.scheduleInitialCheck(this)
    }

    private fun setupListeners() {
        // Bouton d'aide pour relancer le tutoriel
        binding.btnHelp.setOnClickListener {
            startTutorial(isManual = true)
        }
    }

    private fun startTutorial(isManual: Boolean = false) {
        val intent = Intent(this, OnboardingActivity::class.java)
        if (!isManual) {
            startActivity(intent)
            finish()
        } else {
            // Si lancé manuellement, on ne ferme pas la MainActivity
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            // Navigation vers l'onglet des missions si demandé par la notification
            if (it.getStringExtra("open_tab") == "tasks") {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.navHostFragment) as NavHostFragment
                navHostFragment.navController.navigate(R.id.tasksFragment)
            }

            // Lancement d'un focus personnalisé si présent dans l'intent
            if (it.hasExtra(NotificationReceiver.EXTRA_CUSTOM_FOCUS)) {
                val customMins = it.getIntExtra(NotificationReceiver.EXTRA_CUSTOM_FOCUS, 25)
                viewModel.startCustomFocus(customMins)
            }
        }
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
                // Mise à jour du Streak dynamique
                binding.tvStreak.text = getString(R.string.streak_format, it.currentStreak)

                // Mise à jour de l'XP dynamique avec formatage (ex: 3,840 XP)
                val formatter = NumberFormat.getNumberInstance(Locale.US)
                val formattedXp = formatter.format(it.totalXp)
                binding.tvXpBadge.text = getString(R.string.xp_badge_format, formattedXp)

                // S'assure que le service de blocage est synchronisé avec la base de données
                AppBlockerService.isBlocking = it.hasPenalty
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }
}
