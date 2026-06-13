package com.focuszone.app.ui.penalty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.focuszone.app.R
import com.focuszone.app.databinding.FragmentPenaltyBinding
import com.focuszone.app.service.AppBlockerService
import com.focuszone.app.viewmodel.TimerViewModel

class PenaltyFragment : Fragment() {

    private var _binding: FragmentPenaltyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels({ requireActivity() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPenaltyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartFocus.setOnClickListener {
            findNavController().navigate(R.id.timerFragment)
        }

        observeStats()
    }

    private fun observeStats() {
        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe

            val hasPenalty = stats.hasPenalty
            AppBlockerService.isBlocking = hasPenalty

            if (!hasPenalty) {
                // No penalty state
                binding.blockedAppsContainer.visibility = View.GONE
                // Show/hide penalty banner based on state
                // (Use visibility logic for the full penalty layout vs no-penalty)
                return@observe
            }

            // Build blocked apps list
            buildBlockedAppsList()
        }
    }

    private fun buildBlockedAppsList() {
        binding.blockedAppsContainer.removeAllViews()
        val context = requireContext()
        val blockedApps = listOf(
            Pair("🎮", "Mobile Legends"),
            Pair("⚡", "Free Fire"),
            Pair("🔫", "PUBG Mobile"),
            Pair("⚽", "FIFA Mobile"),
            Pair("🏆", "Wild Rift"),
            Pair("💥", "Call of Duty Mobile")
        )

        blockedApps.forEach { (icon, name) ->
            val itemView = layoutInflater.inflate(R.layout.item_blocked_app, binding.blockedAppsContainer, false)
            itemView.findViewById<TextView>(R.id.tvAppIcon).text = icon
            itemView.findViewById<TextView>(R.id.tvAppName).text = name
            binding.blockedAppsContainer.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
