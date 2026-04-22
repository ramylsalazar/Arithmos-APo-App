package com.example.apo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.apo.databinding.ActivityPlaybackSelectionBinding

class PlaybackSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaybackSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use Binding to prevent ID mismatch crashes
        binding = ActivityPlaybackSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back Button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Navigate to Recordings
        binding.btnRecordings.setOnClickListener {
            val intent = Intent(this, SavedRecordingsActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Snapshots
        binding.btnSnapshots.setOnClickListener {
            val intent = Intent(this, SavedSnapshotsActivity::class.java)
            startActivity(intent)
        }
    }
}