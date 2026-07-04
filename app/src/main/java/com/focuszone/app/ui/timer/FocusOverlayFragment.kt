package com.focuszone.app.ui.timer

import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.focuszone.app.R
import com.focuszone.app.ui.MainActivity
import com.focuszone.app.viewmodel.TimerMode
import com.focuszone.app.viewmodel.TimerState
import com.focuszone.app.viewmodel.TimerViewModel

class FocusOverlayFragment : Fragment() {

    private val viewModel: TimerViewModel by activityViewModels()
    private var isPenalized = false
    private var isSuccessFinished = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        applyFullScreen(true)
        return inflater.inflate(R.layout.fragment_focus_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvTime = view.findViewById<TextView>(R.id.tvFocusBigTimer)
        val tvPhase = view.findViewById<TextView>(R.id.tvFocusPhase)
        val btnExit = view.findViewById<TextView>(R.id.btnExitFocus)
        val mainContent = view.findViewById<View>(R.id.focusMainContent)
        val congratulationsOverlay = view.findViewById<View>(R.id.congratulationsOverlay)

        viewModel.remainingSeconds.observe(viewLifecycleOwner) { seconds ->
            tvTime.text = viewModel.formatTime(seconds)
        }
        
        viewModel.timerMode.observe(viewLifecycleOwner) { mode ->
            tvPhase.text = if (mode == TimerMode.FOCUS) {
                getString(R.string.overlay_focus_active)
            } else {
                getString(R.string.overlay_pause_active)
            }
            
            if (mode == TimerMode.PAUSE) {
                btnExit.text = getString(R.string.exit_pause_label)
                btnExit.setTextColor(requireContext().getColor(R.color.neon_cyan))
                // Sécurité : si on passe en pause, on ferme l'overlay pour éviter l'écran vide
                if (!isSuccessFinished) {
                    parentFragmentManager.popBackStack()
                }
            } else {
                btnExit.text = getString(R.string.abandon_label)
                btnExit.setTextColor(requireContext().getColor(R.color.neon_red))
            }
        }

        viewModel.sessionCompleted.observe(viewLifecycleOwner) { completed ->
            if (completed == true) {
                viewModel.consumeSessionCompletedEvent()
                isSuccessFinished = true
                showCongratulationsAnimation(mainContent, congratulationsOverlay)
            }
        }

        btnExit.setOnClickListener {
            if (viewModel.timerMode.value == TimerMode.PAUSE) {
                parentFragmentManager.popBackStack()
            } else {
                showExitConfirmation()
            }
        }
    }

    private fun showCongratulationsAnimation(content: View, overlay: View) {
        content.visibility = View.GONE 
        overlay.visibility = View.VISIBLE
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.congratulations_pop)
        overlay.startAnimation(anim)

        overlay.postDelayed({
            if (isAdded) {
                overlay.visibility = View.GONE
                parentFragmentManager.popBackStack()
                // Redirection après succès
                (activity as? MainActivity)?.navigateToTimer()
            }
        }, 5000)
    }

    override fun onStop() {
        super.onStop()
        if (!isPenalized && !isSuccessFinished && 
            viewModel.timerMode.value == TimerMode.FOCUS && 
            viewModel.timerState.value == TimerState.RUNNING) {
            
            isPenalized = true
            viewModel.applyEmergencyPenalty()
            viewModel.resetTimer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPenalized) {
            parentFragmentManager.popBackStack()
            (activity as? MainActivity)?.navigateToTimer()
        } else {
            applyFullScreen(true)
        }
    }

    private fun applyFullScreen(hide: Boolean) {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, !hide)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (hide) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.FocusZoneDialog)
            .setTitle(getString(R.string.exit_focus_title))
            .setMessage(getString(R.string.exit_focus_msg))
            .setPositiveButton(getString(R.string.exit_focus_confirm)) { _, _ ->
                isPenalized = true
                viewModel.applyEmergencyPenalty()
                viewModel.resetTimer()
                parentFragmentManager.popBackStack()
                // Redirection immédiate après abandon
                (activity as? MainActivity)?.navigateToTimer()
            }
            .setNegativeButton(getString(R.string.continue_label), null)
            .show()
    }

    override fun onDestroyView() {
        applyFullScreen(false)
        super.onDestroyView()
    }
}
