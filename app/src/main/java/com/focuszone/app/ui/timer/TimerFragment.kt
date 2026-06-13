package com.focuszone.app.ui.timer

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.focuszone.app.R
import com.focuszone.app.databinding.FragmentTimerBinding
import com.focuszone.app.util.SoundManager
import com.focuszone.app.viewmodel.TimerMode
import com.focuszone.app.viewmodel.TimerState
import com.focuszone.app.viewmodel.TimerViewModel
import com.mikhaellopez.circularprogressbar.CircularProgressBar

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var soundManager: SoundManager
    private var previousLevel = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        soundManager = SoundManager(requireContext())

        setupCircularProgressBar()
        setupListeners()
        observeViewModel()
    }

    private fun setupCircularProgressBar() {
        binding.circularProgressBar.apply {
            progressMax        = 100f
            progress           = 100f
            progressBarWidth          = 6f
            backgroundProgressBarWidth = 6f
            progressBarColor          = requireContext().getColor(R.color.neon_cyan)
            backgroundProgressBarColor = 0x1400F5FF.toInt()
            roundBorder        = true
            startAngle         = 270f
            progressDirection  = CircularProgressBar.ProgressDirection.TO_RIGHT
        }
    }

    private fun setupListeners() {
        // Play / Pause + Auto-launch Fullscreen
        binding.btnPlay.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.btn_scale))
            
            // Si on lance ou reprend une session de FOCUS, on passe en plein écran
            if (viewModel.timerMode.value == TimerMode.FOCUS && viewModel.timerState.value != TimerState.RUNNING) {
                launchFocusOverlay()
            }
            
            viewModel.togglePlayPause()
        }

        // Reset
        binding.btnReset.setOnClickListener {
            viewModel.resetTimer()
        }

        // Fullscreen focus overlay manually
        binding.btnFocusMode.setOnClickListener {
            launchFocusOverlay()
        }

        // Mode tab — FOCUS
        binding.tabFocus.setOnClickListener {
            if (viewModel.timerState.value != TimerState.RUNNING) {
                viewModel.switchMode(TimerMode.FOCUS)
                updateTabsUI(TimerMode.FOCUS)
            }
        }

        // Mode tab — PAUSE
        binding.tabPause.setOnClickListener {
            if (viewModel.timerState.value != TimerState.RUNNING) {
                viewModel.switchMode(TimerMode.PAUSE)
                updateTabsUI(TimerMode.PAUSE)
            }
        }

        // Click on duration labels to specify exact minutes
        binding.tvFocusDuration.setOnClickListener {
            showDurationInputDialog(isFocus = true)
        }
        binding.tvBreakDuration.setOnClickListener {
            showDurationInputDialog(isFocus = false)
        }

        // Focus duration steppers (1min precision)
        binding.btnFocusMinus.setOnClickListener { viewModel.adjustFocusMinutes(-1) }
        binding.btnFocusPlus.setOnClickListener  { viewModel.adjustFocusMinutes(1)  }

        // Break duration steppers
        binding.btnBreakMinus.setOnClickListener { viewModel.adjustBreakMinutes(-1) }
        binding.btnBreakPlus.setOnClickListener  { viewModel.adjustBreakMinutes(1)  }
    }

    private fun launchFocusOverlay() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, FocusOverlayFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun showDurationInputDialog(isFocus: Boolean) {
        val title = if (isFocus) "Durée du Focus" else "Durée de la Pause"
        val currentVal = if (isFocus) viewModel.focusMinutes.value ?: 25 else viewModel.breakMinutes.value ?: 5

        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentVal.toString())
            setSelection(text.length)
        }

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = 60
            rightMargin = 60
            topMargin = 20
            bottomMargin = 20
        }
        container.addView(editText, params)

        AlertDialog.Builder(requireContext(), R.style.FocusZoneDialog)
            .setTitle(title)
            .setMessage("Spécifiez le nombre de minutes :")
            .setView(container)
            .setPositiveButton("Valider") { _, _ ->
                val input = editText.text.toString().toIntOrNull()
                if (input != null && input > 0) {
                    if (isFocus) viewModel.setFocusMinutes(input)
                    else viewModel.setBreakMinutes(input)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun observeViewModel() {

        // Timer countdown
        viewModel.remainingSeconds.observe(viewLifecycleOwner) { seconds ->
            binding.tvTimerDisplay.text = viewModel.formatTime(seconds)
            val progressPercent = viewModel.getProgress(seconds) * 100f
            binding.circularProgressBar.setProgressWithAnimation(progressPercent, 800)
        }

        // Play / Pause button icon
        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            val iconRes = if (state == TimerState.RUNNING) R.drawable.ic_pause
            else R.drawable.ic_play
            binding.btnPlay.setImageResource(iconRes)
        }

        // Phase label + tab highlight
        viewModel.timerMode.observe(viewLifecycleOwner) { mode ->
            updateTabsUI(mode)
            binding.tvPhaseLabel.text = if (mode == TimerMode.FOCUS) "TEMPS DE FOCUS"
            else "TEMPS DE PAUSE"
        }

        // Session counter
        viewModel.sessionCount.observe(viewLifecycleOwner) { count ->
            binding.tvSessionCount.text = "Session ${count + 1} / 4"
        }

        // Durations
        viewModel.focusMinutes.observe(viewLifecycleOwner) { mins ->
            binding.tvFocusDuration.text = "$mins min"
        }
        viewModel.breakMinutes.observe(viewLifecycleOwner) { mins ->
            binding.tvBreakDuration.text = "$mins min"
        }

        // Stats row
        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe

            binding.tvStatSessions.text = stats.todaySessions.toString()
            binding.tvStatXp.text       = "+${stats.todayXp}"

            val h = stats.totalFocusMinutes / 60
            val m = stats.totalFocusMinutes % 60
            binding.tvStatTotal.text = if (h > 0) "${h}h${m}m" else "${m}m"

            // Level-up sound
            if (stats.level > previousLevel) {
                soundManager.playLevelUp()
                previousLevel = stats.level
            }
        }

        // Note: L'animation de session terminée est maintenant gérée EXCLUSIVEMENT 
        // par le FocusOverlayFragment pour éviter les doublons lors de la navigation.
    }

    private fun updateTabsUI(mode: TimerMode) {
        if (mode == TimerMode.FOCUS) {
            binding.tabFocus.setBackgroundResource(R.drawable.bg_mode_tab_active)
            binding.tabFocus.setTextColor(requireContext().getColor(R.color.neon_cyan))
            binding.tabPause.setBackgroundResource(android.R.color.transparent)
            binding.tabPause.setTextColor(requireContext().getColor(R.color.text_muted))
        } else {
            binding.tabPause.setBackgroundResource(R.drawable.bg_mode_tab_active)
            binding.tabPause.setTextColor(requireContext().getColor(R.color.neon_cyan))
            binding.tabFocus.setBackgroundResource(android.R.color.transparent)
            binding.tabFocus.setTextColor(requireContext().getColor(R.color.text_muted))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundManager.release()
        _binding = null
    }
}
