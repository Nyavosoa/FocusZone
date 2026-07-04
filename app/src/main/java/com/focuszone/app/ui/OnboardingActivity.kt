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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.focuszone.app.R
import com.focuszone.app.databinding.ActivityOnboardingBinding
import com.focuszone.app.util.PreferencesManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PreferencesManager
    private var currentStep = 0
    private val totalSteps = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferencesManager(this)

        showStep(0)
        binding.btnNext.setOnClickListener { handleNextClick() }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun showStep(step: Int) {
        currentStep = step
        updateDots(step)

        when (step) {
            0 -> {
                binding.onbIcon.text = "⚔️"
                binding.onbTitle.text = "Bienvenue dans l'Arène"
                binding.onbDesc.text = "FocusZone transforme ta productivité en combat.\nIci, ton temps est ta ressource la plus précieuse."
                binding.btnNext.text = "COMMENCER"
                binding.permBox.visibility = View.GONE
            }
            1 -> {
                binding.onbIcon.text = "📱"
                binding.onbTitle.text = "Le Focus Sacré"
                binding.onbDesc.text = "Dès que tu lances un focus, le mode plein écran s'active.\nL'interface se verrouille pour te garder concentré."
                binding.btnNext.text = "SUIVANT"
            }
            2 -> {
                binding.onbIcon.text = "🚫"
                binding.onbTitle.text = "Anti-Triche & Sanction"
                binding.onbDesc.text = "⚠️ Si tu quittes l'application pendant le chrono, tes jeux sont bloqués immédiatement !\nNe fuis pas le combat."
                binding.btnNext.text = "COMPRIS"
            }
            3 -> {
                binding.onbIcon.text = "🎯"
                binding.onbTitle.text = "Missions de Guerrier"
                binding.onbDesc.text = "Crée tes missions. Si tu spécifies un temps de focus, clique sur la mission pour lancer le chrono sacré tout de suite."
                binding.btnNext.text = "SUIVANT"
            }
            4 -> {
                binding.onbIcon.text = "⭐"
                binding.onbTitle.text = "XP & Niveaux"
                binding.onbDesc.text = "Gagne de l'XP pour chaque session et mission finie.\nMonte de niveau pour débloquer de nouveaux thèmes néons."
                binding.btnNext.text = "SUIVANT"
            }
            5 -> {
                binding.onbIcon.text = "⏰"
                binding.onbTitle.text = "Le Juge de 20h00"
                binding.onbDesc.text = "À l'heure choisie, si tes missions du jour ne sont pas cochées : Tes jeux sont bloqués !\n(Heure réglable dans les paramètres)."
                binding.btnNext.text = "SUIVANT"
            }
            6 -> {
                binding.onbIcon.text = "🔋"
                binding.onbTitle.text = "Performance"
                binding.onbDesc.text = "Pour que les sanctions et rappels fonctionnent, désactive l'économie de batterie pour FocusZone."
                binding.btnNext.text = "OPTIMISER →"
            }
            7 -> {
                binding.onbIcon.text = "⚙️"
                binding.onbTitle.text = "Configuration"
                binding.onbDesc.text = "Autorise les notifications et active le service d'accessibilité pour permettre le blocage des jeux."
                binding.btnNext.text = "OUVRIR LES RÉGLAGES"
            }
        }
    }

    private fun handleNextClick() {
        when (currentStep) {
            0, 1, 2, 3, 4, 5 -> goToNextStep()
            6 -> {
                requestIgnoreBatteryOptimizations()
                goToNextStep()
            }
            7 -> {
                if (binding.btnNext.text.toString().contains("RÉGLAGES")) {
                    requestPermissionsAndSettings()
                    binding.btnNext.text = "TERMINER →"
                } else {
                    finishOnboarding()
                }
            }
        }
    }

    private fun goToNextStep() {
        if (currentStep < totalSteps - 1) showStep(currentStep + 1)
        else finishOnboarding()
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try { startActivity(intent) } catch (e: Exception) {}
            }
        }
    }

    private fun requestPermissionsAndSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun finishOnboarding() {
        prefs.hasSeenOnboarding = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun updateDots(activeStep: Int) {
        binding.dotContainer.removeAllViews()
        for (i in 0 until totalSteps) {
            val dot = View(this)
            val size = (8 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()
            val params = android.widget.LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            dot.layoutParams = params
            dot.setBackgroundResource(if (i == activeStep) R.drawable.bg_onb_dot_active else R.drawable.bg_onb_dot)
            binding.dotContainer.addView(dot)
        }
    }
}
