package com.focuszone.app.ui.penalty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.focuszone.app.R

class AppAdapter(private val appList: List<AppInfo>) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_select, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = appList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        private val cbApp: CheckBox = itemView.findViewById(R.id.cbApp)

        fun bind(app: AppInfo) {
            ivIcon.setImageDrawable(app.icon)
            tvName.text = app.name
            
            // On désactive le listener avant de changer l'état pour éviter les boucles
            cbApp.setOnCheckedChangeListener(null)
            cbApp.isChecked = app.isSelected

            itemView.setOnClickListener {
                app.isSelected = !app.isSelected
                cbApp.isChecked = app.isSelected
            }
            
            cbApp.setOnCheckedChangeListener { _, isChecked ->
                app.isSelected = isChecked
            }
        }
    }
}
