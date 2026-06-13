package com.focuszone.app.ui.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focuszone.app.R
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.model.Task
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    private val onChecked: (Task) -> Unit,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(DIFF_CALLBACK) {

    fun getTaskAt(position: Int): Task = getItem(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbTask: CheckBox = itemView.findViewById(R.id.cbTask)
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvNotifLabel: TextView = itemView.findViewById(R.id.tvNotifLabel)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditTask)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTask)
        private val tvCustomFocusBadge: TextView = itemView.findViewById(R.id.tvCustomFocusBadge)
        private val dot1: View = itemView.findViewById(R.id.dot1)
        private val dot2: View = itemView.findViewById(R.id.dot2)
        private val dot3: View = itemView.findViewById(R.id.dot3)
        private val dot4: View = itemView.findViewById(R.id.dot4)

        fun bind(task: Task) {
            tvTaskName.text = task.name
            cbTask.isChecked = task.isCompleted

            // Barré si terminé
            tvTaskName.paintFlags = if (task.isCompleted)
                tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            tvTaskName.alpha = if (task.isCompleted) 0.5f else 1f

            // Badge de Focus personnalisé
            if (task.customFocusMinutes != null && task.customFocusMinutes > 0) {
                tvCustomFocusBadge.visibility = View.VISIBLE
                tvCustomFocusBadge.text = "⏱ ${task.customFocusMinutes} min"
            } else {
                tvCustomFocusBadge.visibility = View.GONE
            }

            // Libellé de notification
            tvNotifLabel.text = buildNotifLabel(task)

            // Points Pomodoro
            val dots = listOf(dot1, dot2, dot3, dot4)
            dots.forEachIndexed { index, dot ->
                dot.setBackgroundResource(
                    if (index < task.pomodoroCount) R.drawable.bg_pomo_dot_filled
                    else R.drawable.bg_pomo_dot
                )
            }

            cbTask.setOnClickListener { onChecked(task) }
            btnEdit.setOnClickListener { onEdit(task) }
            btnDelete.setOnClickListener { onDelete(task) }
        }

        private fun buildNotifLabel(task: Task): String {
            val time = "%02d:%02d".format(task.reminderHour, task.reminderMinute)
            return when (task.repeatType) {
                RepeatType.NONE -> "Pas de rappel"
                RepeatType.DAILY -> "Tous les jours • $time"
                RepeatType.MONDAY -> "Lundi • $time"
                RepeatType.TUESDAY -> "Mardi • $time"
                RepeatType.WEDNESDAY -> "Mercredi • $time"
                RepeatType.THURSDAY -> "Jeudi • $time"
                RepeatType.FRIDAY -> "Vendredi • $time"
                RepeatType.SATURDAY -> "Samedi • $time"
                RepeatType.SUNDAY -> "Dimanche • $time"
                RepeatType.SPECIFIC_DATE -> {
                    val millis = task.specificDateMillis ?: return "Date spécifique"
                    val fmt = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
                    fmt.format(Date(millis))
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(old: Task, new: Task) = old.id == new.id
            override fun areContentsTheSame(old: Task, new: Task) = old == new
        }
    }
}
