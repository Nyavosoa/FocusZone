package com.focuszone.app.ui.rewards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.focuszone.app.R
import com.focuszone.app.data.model.Reward
import com.focuszone.app.data.model.UnlockCondition

class RewardsAdapter(private val rewards: List<Reward>) :
    RecyclerView.Adapter<RewardsAdapter.RewardVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward, parent, false)
        return RewardVH(view)
    }

    override fun onBindViewHolder(holder: RewardVH, position: Int) =
        holder.bind(rewards[position])

    override fun getItemCount() = rewards.size

    inner class RewardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvRewardIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvRewardName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvRewardDesc)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvRewardBadge)
        private val cardRoot: View = itemView.findViewById(R.id.rewardCardRoot)

        fun bind(reward: Reward) {
            tvIcon.text = reward.icon
            tvName.text = reward.name
            tvDesc.text = buildDesc(reward)

            if (reward.isUnlocked) {
                tvBadge.text = "✓ OK"
                tvBadge.setBackgroundResource(R.drawable.bg_badge_unlocked)
                tvBadge.setTextColor(itemView.context.getColor(R.color.neon_green))
                tvIcon.alpha = 1f
                cardRoot.alpha = 1f
            } else {
                tvBadge.text = "🔒"
                tvBadge.background = null
                tvIcon.alpha = 0.35f
                cardRoot.alpha = 0.6f
            }
        }

        private fun buildDesc(reward: Reward): String {
            val condition = when (val c = reward.unlockCondition) {
                is UnlockCondition.Level -> "Niveau ${c.requiredLevel} requis"
                is UnlockCondition.Sessions -> "${c.requiredSessions} sessions complètes"
                is UnlockCondition.Streak -> "Streak de ${c.requiredDays} jours"
                is UnlockCondition.Xp -> "${c.requiredXp} XP requis"
            }
            return if (reward.isUnlocked) reward.description else condition
        }
    }
}
