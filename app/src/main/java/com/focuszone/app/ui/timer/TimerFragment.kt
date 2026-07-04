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
            progressMax = 100f
            progress = 100f
            progressBarWidth = 6f
            backgroundProgressBarWidth = 6f
            progressBarColor = requireContext().getColor(R.color.neon_cyan)
            backgroundProgressBarColor = 0x1400F5FF
            roundBorder = true
            startAngle = 270f
            progressDirection = CircularProgressBar.ProgressDirection.TO_RIGHT
        }
    }

    private fun setupListeners() {
        binding.btnPlay.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.btn_scale))
            viewModel.togglePlayPause()
        }

        binding.btnReset.setOnClickListener { viewModel.resetTimer() }

        binding.btnSkipPause.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.btn_scale))
            viewModel.skipPause()
        }

        binding.btnFocusMode.setOnClickListener { launchFocusOverlay() }

        binding.tabFocus.setOnClickListener {
            if (viewModel.timerState.value != TimerState.RUNNING) viewModel.switchMode(TimerMode.FOCUS)
        }

        binding.tabPause.setOnClickListener {
            if (viewModel.timerState.value != TimerState.RUNNING) viewModel.switchMode(TimerMode.PAUSE)
        }

        binding.tvFocusDuration.setOnClickListener { showDurationInputDialog(true) }
        binding.tvBreakDuration.setOnClickListener { showDurationInputDialog(false) }

        binding.btnFocusMinus.setOnClickListener { viewModel.adjustFocusMinutes(-1) }
        binding.btnFocusPlus.setOnClickListener { viewModel.adjustFocusMinutes(1) }
        binding.btnBreakMinus.setOnClickListener { viewModel.adjustBreakMinutes(-1) }
        binding.btnBreakPlus.setOnClickListener { viewModel.adjustBreakMinutes(1) }
    }

    private fun launchFocusOverlay() {
        val existing = parentFragmentManager.findFragmentByTag("focus_overlay")
        if (existing == null) {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(android.R.id.content, FocusOverlayFragment(), "focus_overlay")
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showDurationInputDialog(isFocus: Boolean) {
        val title = if (isFocus) getString(R.string.focus_duration) else getString(R.string.pause_duration)
        val currentVal = if (isFocus) viewModel.focusMinutes.value ?: 25 else viewModel.breakMinutes.value ?: 5
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentVal.toString())
            setSelection(text.length)
        }
        val container = android.widget.FrameLayout(requireContext()).apply {
            val p = android.widget.FrameLayout.LayoutParams(-1, -1).apply { setMargins(60, 20, 60, 20) }
            addView(editText, p)
        }
        AlertDialog.Builder(requireContext(), R.style.FocusZoneDialog)
            .setTitle(title).setView(container)
            .setPositiveButton(getString(R.string.reset)) { _, _ ->
                val input = editText.text.toString().toIntOrNull()
                if (input != null && input > 0) {
                    if (isFocus) viewModel.setFocusMinutes(input) else viewModel.setBreakMinutes(input)
                }
            }.setNegativeButton("Annuler", null).show()
    }

    private fun observeViewModel() {
        viewModel.remainingSeconds.observe(viewLifecycleOwner) { seconds ->
            binding.tvTimerDisplay.text = viewModel.formatTime(seconds)
            binding.circularProgressBar.setProgressWithAnimation(viewModel.getProgress(seconds) * 100f, 800)
        }

        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            binding.btnPlay.setImageResource(if (state == TimerState.RUNNING) R.drawable.ic_pause else R.drawable.ic_play)
        }

        viewModel.timerMode.observe(viewLifecycleOwner) { mode ->
            updateTabsUI(mode)
            binding.tvPhaseLabel.text = if (mode == TimerMode.FOCUS) getString(R.string.focus_mode) else getString(R.string.pause_mode)
            binding.btnSkipPause.visibility = if (mode == TimerMode.PAUSE) View.VISIBLE else View.GONE
        }

        viewModel.sessionCount.observe(viewLifecycleOwner) { count ->
            binding.tvSessionCount.text = getString(R.string.session_count, count)
        }

        viewModel.focusMinutes.observe(viewLifecycleOwner) { mins -> 
            binding.tvFocusDuration.text = getString(R.string.duration_min_format, mins) 
        }
        viewModel.breakMinutes.observe(viewLifecycleOwner) { mins -> 
            binding.tvBreakDuration.text = getString(R.string.duration_min_format, mins) 
        }

        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvStatSessions.text = it.todaySessions.toString()
                binding.tvStatXp.text = getString(R.string.xp_gain_format, it.todayXp)
                val h = it.totalFocusMinutes / 60
                val m = it.totalFocusMinutes % 60
                binding.tvStatTotal.text = if (h > 0) "${h}h${m}m" else "${m}m"
                if (it.level > previousLevel) {
                    soundManager.playLevelUp()
                    previousLevel = it.level
                }
            }
        }
    }

    private fun updateTabsUI(mode: TimerMode) {
        val isFocus = mode == TimerMode.FOCUS
        binding.tabFocus.setBackgroundResource(if (isFocus) R.drawable.bg_mode_tab_active else android.R.color.transparent)
        binding.tabFocus.setTextColor(requireContext().getColor(if (isFocus) R.color.neon_cyan else R.color.text_muted))
        binding.tabPause.setBackgroundResource(if (!isFocus) R.drawable.bg_mode_tab_active else android.R.color.transparent)
        binding.tabPause.setTextColor(requireContext().getColor(if (!isFocus) R.color.neon_cyan else R.color.text_muted))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundManager.release()
        _binding = null
    }
}
