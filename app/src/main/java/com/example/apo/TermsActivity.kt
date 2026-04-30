package com.example.apo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.apo.databinding.ActivityTermsBinding

class TermsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsBinding
    private var isAtBottom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("APoPrefs", MODE_PRIVATE)
        val hasAccepted = prefs.getBoolean("terms_accepted", false)

        // If visiting from Sidebar/Settings, change button text for clarity
        if (hasAccepted) {
            binding.btnAccept.text = "Close"
        }

        // Accept and Save logic
        binding.btnAccept.setOnClickListener {
            if (!hasAccepted) {
                // First-time acceptance flow
                prefs.edit().putBoolean("terms_accepted", true).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // Permanent review flow: just go back to the previous screen
                finish()
            }
        }

        // Scroll Button Logic (Scroll to Top/Bottom)
        binding.btnScroll.setOnClickListener {
            if (isAtBottom) {
                binding.termsScroll.fullScroll(View.FOCUS_UP)
            } else {
                binding.termsScroll.fullScroll(View.FOCUS_DOWN)
            }
        }

        // Detect scroll position to update button text dynamically
        binding.termsScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val childHeight = v.getChildAt(0).measuredHeight
                // Check if user has reached the end of the text
                isAtBottom = scrollY + v.measuredHeight >= childHeight - 10

                binding.btnScroll.text = if (isAtBottom) "Scroll to Top" else "Scroll to Bottom"
            }
        )
    }
}