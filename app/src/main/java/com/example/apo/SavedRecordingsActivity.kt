package com.example.apo

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apo.databinding.ActivitySavedRecordingsBinding
import java.io.File

class SavedRecordingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedRecordingsBinding
    private lateinit var adapter: RecordingAdapter
    private var currentFiles = listOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedRecordingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackRecording.setOnClickListener {
            if (::adapter.isInitialized && adapter.isSelectionMode) {
                exitSelectionMode()
            } else {
                finish()
            }
        }

        binding.btnSelectToggle.setOnClickListener {
            if (::adapter.isInitialized && adapter.isSelectionMode) {
                exitSelectionMode()
            } else {
                enterSelectionMode()
            }
        }

        // BULK DELETE LOGIC
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
            setupRecyclerView() // Refresh the list
        }

        // FEATURE ADD: BULK EXPORT LOGIC
        binding.btnBulkExport.setOnClickListener {
            val selectedIndices = adapter.selectedPositions.toList()
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (!publicDir.exists()) publicDir.mkdirs()

            var exportCount = 0
            for (index in selectedIndices) {
                if (index < currentFiles.size) {
                    val file = currentFiles[index]
                    if (file.exists()) {
                        try {
                            val destFile = File(publicDir, "APO_${file.name}")
                            file.copyTo(destFile, overwrite = true)
                            exportCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            Toast.makeText(this, "Exported $exportCount videos to Gallery", Toast.LENGTH_LONG).show()
            exitSelectionMode()
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recordDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Arithmos/Recordings")

        // Scan for mp4, avi, or mkv
        currentFiles = if (recordDir.exists()) {
            recordDir.listFiles()?.filter { it.extension.lowercase() in listOf("mp4", "avi", "mkv") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }

        if (currentFiles.isEmpty()) {
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.rvRecordings.visibility = View.GONE
            binding.btnSelectToggle.visibility = View.GONE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
            binding.rvRecordings.visibility = View.VISIBLE
            binding.btnSelectToggle.visibility = View.VISIBLE

            adapter = RecordingAdapter(currentFiles,
                onSelectionChanged = { count ->
                    binding.tvSelectionCount.text = "$count selected"
                    binding.layoutActions.visibility = if (count > 0 && adapter.isSelectionMode) View.VISIBLE else View.GONE
                },
                onItemClick = { file ->
                    val intent = Intent(this, MediaPreviewActivity::class.java)
                    val paths = ArrayList(currentFiles.map { it.absolutePath })
                    intent.putStringArrayListExtra("FILE_LIST", paths)
                    intent.putExtra("MEDIA_PATH", file.absolutePath)
                    intent.putExtra("IS_VIDEO", true)
                    startActivity(intent)
                }
            )

            binding.rvRecordings.layoutManager = LinearLayoutManager(this)
            binding.rvRecordings.adapter = adapter
        }
    }

    private fun enterSelectionMode() {
        if (::adapter.isInitialized) {
            adapter.isSelectionMode = true
            binding.btnSelectToggle.text = "Cancel"
            adapter.notifyDataSetChanged()
        }
    }

    private fun exitSelectionMode() {
        if (::adapter.isInitialized) {
            adapter.isSelectionMode = false
            adapter.selectedPositions.clear()
            binding.btnSelectToggle.text = "Select"
            binding.layoutActions.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized && !adapter.isSelectionMode) {
            setupRecyclerView()
        } else if (!::adapter.isInitialized) {
            setupRecyclerView()
        }
    }
}