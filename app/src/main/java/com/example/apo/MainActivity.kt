package com.example.apo

import android.content.Intent
import android.graphics.*
import android.os.*
import android.util.Log
import android.webkit.WebSettings
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.apo.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isPolling = false
    private var isRecordingLocal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val targetIp = intent.getStringExtra("TARGET_IP") ?: ""

        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }

        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<ImageButton>(R.id.btnCloseDrawer).setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.btnLogout.setOnClickListener {
            startActivity(Intent(this, ConnectionActivity::class.java))
            finish()
        }

        binding.btnSnapshot.setOnClickListener { takeSnapshot() }
        binding.btnRecord.setOnClickListener { toggleRecording(targetIp) }

        // ==========================================
        // NAVIGATION DRAWER (PERMANENT TERMS ADDED)
        // ==========================================
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_alerts -> startActivity(Intent(this, AlertHistoryActivity::class.java))
                R.id.nav_playback -> startActivity(Intent(this, PlaybackHistoryActivity::class.java))

                // PERMANENT TERMS LINK
                R.id.nav_terms -> showPermanentTerms()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        setupWebView(targetIp)
    }

    private fun showPermanentTerms() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Terms & Privacy")
            .setMessage("• Data remains local to your device.\n• No cloud storage is used.\n• Behavioral detection is performed on-device via MobileNet.\n• Unauthorized surveillance is strictly prohibited.")
            .setPositiveButton("Close", null)
            .show()
    }

    // --- EXISTING CAMERA LOGIC ---
    private fun setupWebView(ip: String) {
        binding.webViewCam.settings.javaScriptEnabled = true
        if (ip.isNotEmpty()) {
            val streamUrl = "http://$ip:5000/video_feed"
            val html = "<html><body style='margin:0;background:#000;'><img src='$streamUrl' style='width:100%;height:100%;object-fit:contain;'/></body></html>"
            binding.webViewCam.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            isPolling = true
            startStatusPolling(ip)
        }
    }

    private fun takeSnapshot() {
        try {
            val bitmap = Bitmap.createBitmap(binding.webViewCam.width, binding.webViewCam.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.webViewCam.draw(canvas)
            val snapDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Arithmos/Snapshots")
            if (!snapDir.exists()) snapDir.mkdirs()
            val file = File(snapDir, "SNAP_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            Toast.makeText(this, "Snapshot Saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Log.e("APO", "Snapshot failed") }
    }

    private fun toggleRecording(ip: String) {
        isRecordingLocal = !isRecordingLocal
        val action = if (isRecordingLocal) "start" else "stop"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conn = URL("http://$ip:5000/record/$action").openConnection() as HttpURLConnection
                if (conn.responseCode == 200) {
                    withContext(Dispatchers.Main) {
                        if (isRecordingLocal) {
                            binding.btnRecord.setColorFilter(Color.RED)
                            Toast.makeText(this@MainActivity, "🔴 Recording...", Toast.LENGTH_SHORT).show()
                        } else {
                            binding.btnRecord.clearColorFilter()
                            Toast.makeText(this@MainActivity, "✅ Saved to Pi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun startStatusPolling(ip: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isPolling) {
                try {
                    val conn = URL("http://$ip:5000/status").openConnection() as HttpURLConnection
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    withContext(Dispatchers.Main) {
                        binding.tvEventText.text = "${json.getString("activity")} at ${json.getString("timestamp")}"
                    }
                } catch (e: Exception) { }
                delay(1000)
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); isPolling = false }
}