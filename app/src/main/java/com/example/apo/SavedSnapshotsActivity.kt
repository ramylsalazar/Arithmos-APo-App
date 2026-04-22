package com.example.apo

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.apo.databinding.ActivitySavedSnapshotsBinding
import java.io.File

class SavedSnapshotsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedSnapshotsBinding
    private lateinit var adapter: SnapshotAdapter
    private var currentFiles = listOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedSnapshotsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            if (adapter.isSelectionMode) exitSelectionMode() else finish()
        }

        binding.btnSelectToggle.setOnClickListener {
            if (adapter.isSelectionMode) exitSelectionMode() else enterSelectionMode()
        }

        // FULL BULK DELETE LOGIC
        binding.btnBulkDelete.setOnClickListener {
            val selectedIndices = adapter.selectedPositions.toList()
            for (index in selectedIndices) {
                if (index < currentFiles.size) {
                    val file = currentFiles[index]
                    if (file.exists()) file.delete()
                }
            }
            Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
            exitSelectionMode()
            setupRecyclerView() // Refresh the grid
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val snapDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Arithmos/Snapshots")
        currentFiles = if (snapDir.exists()) snapDir.listFiles()?.filter { it.extension.lowercase() in listOf("jpg", "png") }?.toList() ?: emptyList() else emptyList()

        adapter = SnapshotAdapter(currentFiles,
            onSelectionChanged = { count ->
                binding.tvSelectionCount.text = "$count selected"
                binding.layoutActions.visibility = if (count > 0 && adapter.isSelectionMode) View.VISIBLE else View.GONE
            },
            onImageClick = { file ->
                // BUG 1 FIX: Passes ALL paths to MediaPreviewActivity so it knows what to load
                val intent = Intent(this, MediaPreviewActivity::class.java)
                val paths = ArrayList(currentFiles.map { it.absolutePath })
                intent.putStringArrayListExtra("FILE_LIST", paths)
                intent.putExtra("MEDIA_PATH", file.absolutePath)
                intent.putExtra("IS_VIDEO", false)
                startActivity(intent)
            }
        )

        binding.rvSnapshots.layoutManager = GridLayoutManager(this, 3)
        binding.rvSnapshots.adapter = adapter
    }

    private fun enterSelectionMode() {
        adapter.isSelectionMode = true
        binding.btnSelectToggle.text = "Cancel"
        adapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        adapter.isSelectionMode = false
        adapter.selectedPositions.clear()
        binding.btnSelectToggle.text = "Select"
        binding.layoutActions.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    // Refresh the grid automatically when coming back from preview
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized && !adapter.isSelectionMode) {
            setupRecyclerView()
        }
    }
}