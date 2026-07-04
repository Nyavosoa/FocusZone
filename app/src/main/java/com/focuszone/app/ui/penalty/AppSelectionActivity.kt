package com.focuszone.app.ui.penalty

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.focuszone.app.databinding.ActivityAppSelectionBinding
import com.focuszone.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    var isSelected: Boolean
)

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var prefs: PreferencesManager
    private val appList = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferencesManager(this)
        setupRecyclerView()
        loadInstalledApps()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveApps.setOnClickListener { saveSelection() }
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(appList)
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun loadInstalledApps() {
        binding.loadingProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val installedApps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val blockedSet = prefs.blockedPackages
                
                packages.filter { 
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName == "com.android.chrome"
                }.map { 
                    AppInfo(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.packageName,
                        icon = it.loadIcon(pm),
                        isSelected = it.packageName in blockedSet
                    )
                }.sortedBy { it.name }
            }

            appList.clear()
            appList.addAll(installedApps)
            adapter.notifyDataSetChanged()
            binding.loadingProgress.visibility = View.GONE
        }
    }

    private fun saveSelection() {
        val selectedPackages = appList.filter { it.isSelected }.map { it.packageName }.toSet()
        prefs.blockedPackages = selectedPackages
        finish()
    }
}
