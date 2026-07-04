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
    private val onDelete: (Task) -> Unit,
    private val onStartFocus: (Task) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(DIFF_CALLBACK) {

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
        private val tvFinishedBadge: TextView = itemView.findViewById(R.id.tvFinishedBadge)
        private val taskItemRoot: View = itemView.findViewById(R.id.taskItemRoot)

        fun bind(task: Task) {
            tvTaskName.text = task.name
            cbTask.isChecked = task.isCompleted

            if (task.isCompleted) {
                tvTaskName.paintFlags = tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskItemRoot.alpha = 0.5f
                tvFinishedBadge.visibility = View.VISIBLE
                btnEdit.visibility = View.GONE
            } else {
                tvTaskName.paintFlags = tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskItemRoot.alpha = 1.0f
                tvFinishedBadge.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
            }

            // MODIFICATION : On affiche le badge mais on ne permet plus de cliquer sur la ligne
            // Le focus ne se lance QUE via la notification
            if (task.customFocusMinutes != null && task.customFocusMinutes > 0) {
                tvCustomFocusBadge.visibility = View.VISIBLE
                tvCustomFocusBadge.text = "⏱ ${task.customFocusMinutes} min"
            } else {
                tvCustomFocusBadge.visibility = View.GONE
            }
            itemView.setOnClickListener(null) 

            tvNotifLabel.text = buildNotifLabel(task)

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
