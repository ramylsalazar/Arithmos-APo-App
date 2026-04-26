package com.example.apo

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PlaybackHistoryActivity : AppCompatActivity() {

    private lateinit var rvSnapshots: RecyclerView
    private lateinit var rvRecordings: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback_history)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        rvSnapshots = findViewById(R.id.rvSnapshots)
        rvRecordings = findViewById(R.id.rvRecordings)

        rvSnapshots.layoutManager = GridLayoutManager(this, 2)
        rvRecordings.layoutManager = LinearLayoutManager(this)

        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        val snapDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Arithmos/Snapshots")
        val recDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Arithmos/Recordings")

        // Set Snapshot Adapter
        rvSnapshots.adapter = SnapshotAdapter(loadFiles(snapDir)) { file ->
            openPreview(file)
        }

        // Set Recording Adapter
        rvRecordings.adapter = RecordingAdapter(loadFiles(recDir), { /* selection logic */ }) { file ->
            openPreview(file)
        }
    }

    private fun openPreview(file: File) {
        val intent = Intent(this, MediaPreviewActivity::class.java)
        intent.putExtra("FILE_PATH", file.absolutePath)
        startActivity(intent)
    }

    private fun setupTabs() {
        val tabSnapshot = findViewById<LinearLayout>(R.id.tabSnapshot)
        val tabRecording = findViewById<LinearLayout>(R.id.tabRecording)
        val tvTabSnapshot = findViewById<TextView>(R.id.tvTabSnapshot)
        val tvTabRecording = findViewById<TextView>(R.id.tvTabRecording)
        val indSnapshot = findViewById<View>(R.id.indicatorSnapshot)
        val indRecording = findViewById<View>(R.id.indicatorRecording)

        tabSnapshot.setOnClickListener {
            updateTabUI(tvTabSnapshot, indSnapshot, tvTabRecording, indRecording)
            rvSnapshots.visibility = View.VISIBLE
            rvRecordings.visibility = View.GONE
        }

        tabRecording.setOnClickListener {
            updateTabUI(tvTabRecording, indRecording, tvTabSnapshot, indSnapshot)
            rvRecordings.visibility = View.VISIBLE
            rvSnapshots.visibility = View.GONE
        }
    }

    private fun updateTabUI(activeTv: TextView, activeInd: View, inactiveTv: TextView, inactiveInd: View) {
        activeTv.setTextColor(Color.BLACK)
        activeTv.setTypeface(null, Typeface.BOLD)
        activeInd.setBackgroundColor(Color.BLACK)
        inactiveTv.setTextColor(Color.GRAY)
        inactiveTv.setTypeface(null, Typeface.NORMAL)
        inactiveInd.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun loadFiles(directory: File): List<File> {
        if (!directory.exists()) return emptyList()
        return directory.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}