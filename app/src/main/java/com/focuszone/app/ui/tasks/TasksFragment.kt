package com.focuszone.app.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.focuszone.app.R
import com.focuszone.app.data.model.RepeatType
import com.focuszone.app.data.model.Task
import com.focuszone.app.databinding.FragmentTasksBinding
import com.focuszone.app.ui.MainActivity
import com.focuszone.app.viewmodel.TasksViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TasksViewModel by activityViewModels()
    private lateinit var tasksAdapter: TasksAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()

        binding.btnAddTask.setOnClickListener {
            showAddTaskBottomSheet()
        }
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(
            onChecked = { task -> viewModel.toggleTaskCompleted(task) },
            onEdit = { task -> showAddTaskBottomSheet(task) },
            onDelete = { task -> showDeleteConfirmation(task) },
            onStartFocus = { task ->
                // Action: Lancement immédiat du mode plein écran sacré
                val customMins = task.customFocusMinutes ?: 25
                (activity as? MainActivity)?.launchFocusFullscreen(customMins)
            }
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }
    }

    private fun setupObservers() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            tasksAdapter.submitList(tasks)
            val progress = viewModel.getProgressPercent(tasks)
            binding.progressTasks.progress = progress
            binding.tvTaskProgress.text = viewModel.getProgressText(tasks)

            binding.rvTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
            binding.emptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(requireContext(), R.style.FocusZoneDialog)
            .setTitle("Supprimer la mission ?")
            .setMessage("Es-tu sûr de vouloir supprimer \"${task.name}\" ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteTask(task)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAddTaskBottomSheet(taskToEdit: Task? = null) {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_FocusZone_BottomSheet)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_task, null)
        dialog.setContentView(sheetView)

        val tvSheetTitle = sheetView.findViewById<TextView>(R.id.tvSheetTitle)
        val etTaskName = sheetView.findViewById<TextInputEditText>(R.id.etTaskName)
        val etCustomFocus = sheetView.findViewById<TextInputEditText>(R.id.etCustomFocus)
        val chipGroupRepeat = sheetView.findViewById<ChipGroup>(R.id.chipGroupRepeat)
        val layoutTimePicker = sheetView.findViewById<LinearLayout>(R.id.layoutTimePicker)
        val layoutDatePicker = sheetView.findViewById<LinearLayout>(R.id.layoutDatePicker)
        val tvSelectedTime = sheetView.findViewById<TextView>(R.id.tvSelectedTime)
        val tvSelectedDate = sheetView.findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickTime = sheetView.findViewById<TextView>(R.id.btnPickTime)
        val btnPickDate = sheetView.findViewById<TextView>(R.id.btnPickDate)
        val btnSave = sheetView.findViewById<View>(R.id.btnCreateTask)

        var selectedHour = taskToEdit?.reminderHour ?: 9
        var selectedMinute = taskToEdit?.reminderMinute ?: 0
        var selectedDateMillis: Long? = taskToEdit?.specificDateMillis

        if (taskToEdit != null) {
            tvSheetTitle.text = "MODIFIER LA MISSION"
            etTaskName.setText(taskToEdit.name)
            etCustomFocus.setText(taskToEdit.customFocusMinutes?.toString() ?: "")
            tvSelectedTime.text = "%02d:%02d".format(selectedHour, selectedMinute)
            
            val chipId = when (taskToEdit.repeatType) {
                RepeatType.NONE -> R.id.chipRepeatNone
                RepeatType.DAILY -> R.id.chipRepeatDaily
                RepeatType.MONDAY -> R.id.chipMon
                RepeatType.TUESDAY -> R.id.chipTue
                RepeatType.WEDNESDAY -> R.id.chipWed
                RepeatType.THURSDAY -> R.id.chipThu
                RepeatType.FRIDAY -> R.id.chipFri
                RepeatType.SATURDAY -> R.id.chipSat
                RepeatType.SUNDAY -> R.id.chipSun
                RepeatType.SPECIFIC_DATE -> R.id.chipRepeatSpecific
            }
            chipGroupRepeat.check(chipId)
            
            val initialDateSnapshot = selectedDateMillis
            if (taskToEdit.repeatType == RepeatType.SPECIFIC_DATE && initialDateSnapshot != null) {
                val fmt = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH)
                tvSelectedDate.text = fmt.format(Date(initialDateSnapshot))
                tvSelectedDate.setTextColor(requireContext().getColor(R.color.text_primary))
            }
        }

        fun updatePickersVisibility() {
            val checkedId = chipGroupRepeat.checkedChipId
            layoutTimePicker.visibility = if (checkedId == R.id.chipRepeatNone) View.GONE else View.VISIBLE
            layoutDatePicker.visibility = if (checkedId == R.id.chipRepeatSpecific) View.VISIBLE else View.GONE
        }
        updatePickersVisibility()
        chipGroupRepeat.setOnCheckedStateChangeListener { _, _ -> updatePickersVisibility() }

        btnPickTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                selectedHour = h; selectedMinute = m
                tvSelectedTime.text = "%02d:%02d".format(h, m)
            }, selectedHour, selectedMinute, true).show()
        }

        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            selectedDateMillis?.let { cal.timeInMillis = it }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d) 
                selectedDateMillis = cal.timeInMillis
                tvSelectedDate.text = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(cal.time)
                tvSelectedDate.setTextColor(requireContext().getColor(R.color.text_primary))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val name = etTaskName.text?.toString()?.trim()
            if (name.isNullOrBlank()) {
                etTaskName.error = "Donne un nom à ta mission !"
                return@setOnClickListener
            }

            // CORRECTION SMART CAST : On utilise une variable locale immuable pour le traitement
            val dateSnapshot = selectedDateMillis
            val finalDateMillis = dateSnapshot?.let { millis ->
                Calendar.getInstance().apply {
                    timeInMillis = millis
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val repeatType = when (chipGroupRepeat.checkedChipId) {
                R.id.chipRepeatDaily -> RepeatType.DAILY
                R.id.chipMon -> RepeatType.MONDAY
                R.id.chipTue -> RepeatType.TUESDAY
                R.id.chipWed -> RepeatType.WEDNESDAY
                R.id.chipThu -> RepeatType.THURSDAY
                R.id.chipFri -> RepeatType.FRIDAY
                R.id.chipSat -> RepeatType.SATURDAY
                R.id.chipSun -> RepeatType.SUNDAY
                R.id.chipRepeatSpecific -> RepeatType.SPECIFIC_DATE
                else -> RepeatType.NONE
            }

            viewModel.saveTask(
                id = taskToEdit?.id ?: 0L,
                name = name,
                repeatType = repeatType,
                reminderHour = selectedHour,
                reminderMinute = selectedMinute,
                specificDateMillis = finalDateMillis,
                customFocusMinutes = etCustomFocus.text?.toString()?.toIntOrNull()
            )
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
