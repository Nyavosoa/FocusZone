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
import com.focuszone.app.viewmodel.TimerMode
import com.focuszone.app.viewmodel.TimerState
import com.focuszone.app.viewmodel.TimerViewModel

class FocusOverlayFragment : Fragment() {

    private val viewModel: TimerViewModel by activityViewModels()
    private var isPenalized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        applyFullScreen(true)
        return inflater.inflate(R.layout.fragment_focus_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvTime = view.findViewById<TextView>(R.id.tvFocusBigTimer)
        val tvPhase = view.findViewById<TextView>(R.id.tvFocusPhase)
        val btnExit = view.findViewById<TextView>(R.id.btnExitFocus)
        val congratulationsOverlay = view.findViewById<View>(R.id.congratulationsOverlay)

        viewModel.remainingSeconds.observe(viewLifecycleOwner) { seconds ->
            tvTime.text = viewModel.formatTime(seconds)
        }
        
        viewModel.timerMode.observe(viewLifecycleOwner) { mode ->
            tvPhase.text = if (mode == TimerMode.FOCUS) "■ MODE FOCUS ACTIF ■" else "■ PAUSE EN COURS ■"
        }

        viewModel.sessionCompleted.observe(viewLifecycleOwner) { completed ->
            if (completed == true) {
                viewModel.consumeSessionCompletedEvent()
                showCongratulationsAnimation(congratulationsOverlay)
            }
        }

        btnExit.setOnClickListener {
            showExitConfirmation()
        }
    }

    private fun showCongratulationsAnimation(overlay: View) {
        overlay.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.btnExitFocus)?.visibility = View.GONE
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.congratulations_pop)
        overlay.startAnimation(anim)

        overlay.postDelayed({
            if (isAdded) {
                overlay.visibility = View.GONE
                parentFragmentManager.popBackStack()
            }
        }, 5000)
    }

    override fun onStop() {
        super.onStop()
        // Anti-triche : si on quitte l'app pendant un focus
        if (!isPenalized && viewModel.timerMode.value == TimerMode.FOCUS && 
            viewModel.timerState.value == TimerState.RUNNING) {
            
            isPenalized = true
            viewModel.applyEmergencyPenalty()
            viewModel.resetTimer()
            
            // On ne peut pas popBackStack ici (crash StateLoss), 
            // le fragment sera retiré au prochain passage dans onResume ou via le reset.
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPenalized) {
            parentFragmentManager.popBackStack()
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
            .setTitle("Abandonner le focus ?")
            .setMessage("Si tu quittes maintenant, tes jeux seront bloqués !")
            .setPositiveButton("Quitter (Pénalité)") { _, _ ->
                isPenalized = true
                viewModel.applyEmergencyPenalty()
                viewModel.resetTimer()
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Continuer", null)
            .show()
    }

    override fun onDestroyView() {
        applyFullScreen(false)
        super.onDestroyView()
    }
}
