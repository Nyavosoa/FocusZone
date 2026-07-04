package com.focuszone.app.ui.penalty

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.focuszone.app.R
import com.focuszone.app.databinding.FragmentPenaltyBinding
import com.focuszone.app.service.AppBlockerService
import com.focuszone.app.viewmodel.TimerViewModel
import com.focuszone.app.util.PreferencesManager

class PenaltyFragment : Fragment() {

    private var _binding: FragmentPenaltyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels({ requireActivity() })
    private lateinit var prefs: PreferencesManager

    private val ringtoneLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            prefs.notificationSoundUri = uri?.toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPenaltyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferencesManager(requireContext())

        setupListeners()
        observeStats()
        updateSettingsUI()
    }

    private fun setupListeners() {
        binding.btnStartFocusPenalty.setOnClickListener {
            findNavController().navigate(R.id.timerFragment)
        }

        binding.btnSelectApps.setOnClickListener {
            startActivity(Intent(requireContext(), AppSelectionActivity::class.java))
        }

        binding.tvCheckHour.setOnClickListener {
            showCheckTimePickerDialog()
        }

        binding.tvDefaultFocus.setOnClickListener {
            showDefaultValDialog("Focus par défaut", true)
        }

        binding.tvDefaultBreak.setOnClickListener {
            showDefaultValDialog("Pause par défaut", false)
        }

        binding.btnPickRingtone.setOnClickListener {
            pickRingtone()
        }
    }

    private fun updateSettingsUI() {
        binding.tvCheckHour.text = "%02d:%02d".format(prefs.penaltyCheckHour, prefs.penaltyCheckMinute)
        binding.tvDefaultFocus.text = "${prefs.focusMinutes} min"
        binding.tvDefaultBreak.text = "${prefs.breakMinutes} min"
    }

    private fun showCheckTimePickerDialog() {
        TimePickerDialog(requireContext(), { _, hour, minute ->
            viewModel.setPenaltyCheckTime(hour, minute)
            updateSettingsUI()
        }, prefs.penaltyCheckHour, prefs.penaltyCheckMinute, true).show()
    }

    private fun showDefaultValDialog(title: String, isFocus: Boolean) {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText((if (isFocus) prefs.focusMinutes else prefs.breakMinutes).toString())
        }

        AlertDialog.Builder(requireContext(), R.style.FocusZoneDialog)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("Enregistrer") { _, _ ->
                val input = editText.text.toString().toIntOrNull()
                if (input != null && input > 0) {
                    if (isFocus) viewModel.setFocusMinutes(input) else viewModel.setBreakMinutes(input)
                    updateSettingsUI()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun pickRingtone() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sonnerie des missions")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, prefs.notificationSoundUri?.let { Uri.parse(it) })
        }
        ringtoneLauncher.launch(intent)
    }

    private fun observeStats() {
        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                val hasPenalty = it.hasPenalty
                binding.penaltyActiveState.visibility = if (hasPenalty) View.VISIBLE else View.GONE
                binding.noPenaltyState.visibility = if (hasPenalty) View.GONE else View.VISIBLE
                AppBlockerService.isBlocking = hasPenalty
            }
            buildBlockedAppsList()
        }
    }

    private fun buildBlockedAppsList() {
        binding.blockedAppsContainer.removeAllViews()
        val blockedPackages = prefs.blockedPackages
        val pm = requireContext().packageManager

        if (blockedPackages.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "Aucun blocage sélectionné."
                setTextColor(requireContext().getColor(R.color.text_hint))
                textSize = 13f
                setPadding(0, 10, 0, 0)
            }
            binding.blockedAppsContainer.addView(emptyView)
            return
        }

        blockedPackages.take(8).forEach { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name = pm.getApplicationLabel(appInfo).toString()
                val itemView = layoutInflater.inflate(R.layout.item_blocked_app, binding.blockedAppsContainer, false)
                itemView.findViewById<TextView>(R.id.tvAppIcon).text = "🎮" 
                itemView.findViewById<TextView>(R.id.tvAppName).text = name
                binding.blockedAppsContainer.addView(itemView)
            } catch (e: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        buildBlockedAppsList()
        updateSettingsUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
