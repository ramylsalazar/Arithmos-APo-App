package com.example.apo

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val switchDark = findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val switchEntity = findViewById<SwitchMaterial>(R.id.switchEntityBox)
        val sliderThreshold = findViewById<Slider>(R.id.sliderThreshold)
        val tvThresholdLabel = findViewById<TextView>(R.id.tvThresholdLabel)
        val btnReset = findViewById<TextView>(R.id.btnFactoryReset)
        val btnTerms = findViewById<TextView>(R.id.btnTermsSettings)

        // Threshold Slider Logic
        sliderThreshold.addOnChangeListener { _, value, _ ->
            tvThresholdLabel.text = "Inactivity Threshold: ${value.toInt()} mins"
        }

        // Factory Reset Logic
        btnReset.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Factory Reset")
                .setMessage("This will clear all saved preferences and terms acceptance. Are you sure?")
                .setPositiveButton("Reset") { _, _ ->
                    val prefs = getSharedPreferences("APoPrefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    Toast.makeText(this, "App Reset Successfully", Toast.LENGTH_SHORT).show()
                    finishAffinity() // Closes all activities
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Reuse the Terms Dialog logic we made earlier
        btnTerms.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Terms & Privacy")
                .setMessage("• Data remains local.\n• Unauthorized surveillance is prohibited.\n• Behavioral detection active.")
                .setPositiveButton("Close", null)
                .show()
        }

        // Entity Box Toggle (Communication with Pi would go here)
        switchEntity.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "ON" else "OFF"
            Toast.makeText(this, "Entity Boxes: $status", Toast.LENGTH_SHORT).show()
        }
    }
}