package com.example.apo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.apo.databinding.ActivityMediaPreviewBinding
import java.io.File

class MediaPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaPreviewBinding
    private var mediaFiles = mutableListOf<File>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paths = intent.getStringArrayListExtra("FILE_LIST")
        val startPath = intent.getStringExtra("MEDIA_PATH")

        // Populate the list and find where we started
        if (paths != null && startPath != null) {
            mediaFiles = paths.map { File(it) }.toMutableList()
            currentIndex = mediaFiles.indexOfFirst { it.absolutePath == startPath }
            if (currentIndex == -1) currentIndex = 0
            if (mediaFiles.isNotEmpty()) showMedia()
        } else {
            Toast.makeText(this, "Failed to load media", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnNext.setOnClickListener {
            if (currentIndex < mediaFiles.size - 1) {
                currentIndex++
                showMedia()
            }
        }

        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                showMedia()
            }
        }

        // FULL DELETE LOGIC
        binding.btnDeletePreview.setOnClickListener {
            if (mediaFiles.isNotEmpty()) {
                val file = mediaFiles[currentIndex]
                if (file.exists() && file.delete()) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    mediaFiles.removeAt(currentIndex)

                    if (mediaFiles.isEmpty()) {
                        finish() // Exit if no more files left
                    } else {
                        // Adjust index and show next available picture
                        if (currentIndex >= mediaFiles.size) currentIndex = mediaFiles.size - 1
                        showMedia()
                    }
                }
            }
        }
    }

    private fun showMedia() {
        if (mediaFiles.isEmpty()) return

        val file = mediaFiles[currentIndex]
        binding.tvMediaName.text = file.name

        if (binding.videoPlayer.isPlaying) {
            binding.videoPlayer.stopPlayback()
        }
        binding.videoPlayer.suspend()

        if (file.extension.lowercase() in listOf("avi", "mp4", "mkv")) {
            binding.imageViewer.visibility = View.GONE
            binding.videoPlayer.visibility = View.VISIBLE

            // BUG FIX: Gracefully catch MediaPlayer errors to prevent the app from crashing
            binding.videoPlayer.setOnErrorListener { _, what, extra ->
                Log.e("APO", "Video Player Error: $what, $extra")
                Toast.makeText(this@MediaPreviewActivity, "Cannot play video: File may be corrupted or incomplete.", Toast.LENGTH_LONG).show()
                true // Returning true tells Android we handled the error and prevents the fatal crash
            }

            binding.videoPlayer.setVideoPath(file.absolutePath)
            binding.videoPlayer.setOnPreparedListener { it.isLooping = true }
            binding.videoPlayer.start()
        } else {
            binding.videoPlayer.visibility = View.GONE
            binding.imageViewer.visibility = View.VISIBLE
            Glide.with(this).load(file).into(binding.imageViewer)
        }
    }
}
