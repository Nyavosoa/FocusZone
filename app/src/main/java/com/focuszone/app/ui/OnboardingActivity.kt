package com.focuszone.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
    private val totalSteps = 5

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
                binding.onbDesc.text = "FocusZone transforme ta productivité en combat.\nFais tes sessions, gagne de l'XP et protège l'accès à tes jeux."
                binding.btnNext.text = "SUIVANT"
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnSkip.text = "Passer"
                binding.permBox.visibility = View.GONE
            }
            1 -> {
                binding.onbIcon.text = "📱"
                binding.onbTitle.text = "Le Focus Absolu"
                binding.onbDesc.text = "Dès que tu lances un focus, l'écran se verrouille.\n⚠️ Attention : quitter l'application pendant un focus actif bloque tes jeux instantanément !"
                binding.btnNext.text = "J'AI COMPRIS"
                binding.btnSkip.visibility = View.VISIBLE
                binding.permBox.visibility = View.GONE
            }
            2 -> {
                binding.onbIcon.text = "⏰"
                binding.onbTitle.text = "La Règle de 20h00"
                binding.onbDesc.text = "Chaque soir à 20h00, FocusZone vérifie ton travail.\nSi tu as 0 session ou des missions non finies : Tes jeux sont bloqués pour la nuit !"
                binding.btnNext.text = "RELEVER LE DÉFI"
                binding.btnSkip.visibility = View.VISIBLE
                binding.permBox.visibility = View.GONE
            }
            3 -> {
                binding.onbIcon.text = "🔔"
                binding.onbTitle.text = "Autorisations"
                binding.onbDesc.text = "Pour te prévenir des missions et des pénalités, nous avons besoin d'envoyer des notifications."
                binding.btnNext.text = "AUTORISER"
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnSkip.text = "Plus tard"
                binding.permBox.visibility = View.VISIBLE
                binding.permTitle.text = "Permissions requises"
                binding.permItems.text = "🔔 Notifications\n⏰ Alarmes exactes"
            }
            4 -> {
                binding.onbIcon.text = "🚫"
                binding.onbTitle.text = "Blocage des Jeux"
                binding.onbDesc.text = "Pour pouvoir bloquer tes jeux en cas d'échec, tu dois activer le service d'accessibilité pour FocusZone."
                binding.btnNext.text = "OUVRIR LES PARAMÈTRES"
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnSkip.text = "Plus tard"
                binding.permBox.visibility = View.VISIBLE
                binding.permTitle.text = "Accessibilité"
                binding.permItems.text = "⚙️ Paramètres → Accessibilité\n🔍 Activer \"FocusZone\""
            }
        }
    }

    private fun handleNextClick() {
        when (currentStep) {
            0, 1, 2 -> goToNextStep()
            3 -> {
                requestNotificationPermission()
                goToNextStep()
            }
            4 -> {
                if (binding.btnNext.text.toString().contains("PARAMÈTRES")) {
                    openAccessibilitySettings()
                    binding.btnNext.text = "TERMINER L'ENTRAÎNEMENT →"
                } else {
                    finishOnboarding()
                }
            }
        }
    }

    private fun goToNextStep() {
        if (currentStep < totalSteps - 1) {
            showStep(currentStep + 1)
        } else {
            finishOnboarding()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {}
        }
    }

    private fun openAccessibilitySettings() {
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
