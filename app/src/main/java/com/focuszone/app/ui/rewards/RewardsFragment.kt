package com.focuszone.app.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.focuszone.app.databinding.FragmentRewardsBinding
import com.focuszone.app.data.model.RewardsList
import com.focuszone.app.data.model.UnlockCondition
import com.focuszone.app.viewmodel.TimerViewModel

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels({ requireActivity() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRewardsGrid()
        observeStats()
    }

    private fun setupRewardsGrid() {
        binding.rvRewards.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvUpcomingRewards.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun observeStats() {
        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe

            val level = stats.level
            val totalXp = stats.totalXp

            // Level display
            binding.tvLevel.text = level.toString()
            binding.tvLevelTitle.text = "⚔ ${viewModel.getLevelTitle(level)} ⚔"

            // XP bar
            val xpCurrent = viewModel.getXpForCurrentLevel(totalXp)
            val xpRequired = viewModel.getXpRequiredForLevel(level)
            val xpPercent = ((xpCurrent.toFloat() / xpRequired) * 100).toInt()
            binding.xpProgressBar.progress = xpPercent
            binding.tvXpProgress.text = "$xpCurrent / $xpRequired XP → Niveau ${level + 1}"

            // Evaluate rewards unlock
            val allRewards = RewardsList.getAllRewards().map { reward ->
                val unlocked = when (val cond = reward.unlockCondition) {
                    is UnlockCondition.Level -> level >= cond.requiredLevel
                    is UnlockCondition.Sessions -> stats.totalSessions >= cond.requiredSessions
                    is UnlockCondition.Streak -> stats.currentStreak >= cond.requiredDays
                    is UnlockCondition.Xp -> totalXp >= cond.requiredXp
                }
                reward.copy(isUnlocked = unlocked)
            }

            val unlocked = allRewards.filter { it.isUnlocked }
            val locked = allRewards.filter { !it.isUnlocked }

            binding.rvRewards.adapter = RewardsAdapter(unlocked)
            binding.rvUpcomingRewards.adapter = RewardsAdapter(locked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
