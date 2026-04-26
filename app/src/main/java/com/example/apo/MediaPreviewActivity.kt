package com.example.apo

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MediaPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_preview)

        val filePath = intent.getStringExtra("FILE_PATH") ?: return
        val file = File(filePath)

        val tvMediaName = findViewById<TextView>(R.id.tvMediaName)
        val imageViewer = findViewById<ImageView>(R.id.imageViewer)
        val videoPlayer = findViewById<VideoView>(R.id.videoPlayer)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnDelete = findViewById<Button>(R.id.btnDeletePreview)

        tvMediaName.text = file.name

        // Determine if image or video
        if (file.extension.lowercase() == "jpg" || file.extension.lowercase() == "png") {
            imageViewer.visibility = View.VISIBLE
            videoPlayer.visibility = View.GONE
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageViewer.setImageBitmap(bitmap)
        } else {
            videoPlayer.visibility = View.VISIBLE
            imageViewer.visibility = View.GONE
            videoPlayer.setVideoPath(file.absolutePath)
            videoPlayer.start()
        }

        btnBack.setOnClickListener { finish() }

        btnDelete.setOnClickListener {
            if (file.exists() && file.delete()) {
                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }
}