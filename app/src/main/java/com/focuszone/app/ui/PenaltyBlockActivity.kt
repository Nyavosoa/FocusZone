package com.focuszone.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.focuszone.app.R
import com.focuszone.app.data.repository.FocusRepository
import kotlinx.coroutines.launch

class PenaltyBlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_penalty_block)

        findViewById<TextView>(R.id.btnGoFocus).setOnClickListener {
            // Open MainActivity on timer tab
            val intent = android.content.Intent(this, MainActivity::class.java).apply {
                putExtra("open_tab", "timer")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                         android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            finish()
        }
    }

    // Disable back button — penalty cannot be dismissed without doing a focus
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — block is enforced
    }
}
