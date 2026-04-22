package com.example.apo

import android.app.DownloadManager
import android.content.Intent
import android.graphics.*
import android.os.*
import android.util.Log
import android.view.Gravity
import android.webkit.WebSettings
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.apo.databinding.ActivityMainBinding
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

        // ==========================================
        // 1. SIDE MENU LOGIC
        // ==========================================
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        val headerView = binding.navView.getHeaderView(0)
        val btnCloseDrawer = headerView.findViewById<ImageButton>(R.id.btnCloseDrawer)
        btnCloseDrawer.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // ==========================================
        // 2. DASHBOARD BUTTONS
        // ==========================================
        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, ConnectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnSnapshot.setOnClickListener {
            takeSnapshot()
        }

        binding.btnRecord.setOnClickListener {
            toggleRecording(targetIp)
        }

        // ==========================================
        // 3. MENU NAVIGATION ITEMS
        // ==========================================
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_alerts -> startActivity(Intent(this, AlertHistoryActivity::class.java))
                R.id.nav_playback -> startActivity(Intent(this, PlaybackSelectionActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // ==========================================
        // 4. WEBVIEW & STATUS
        // ==========================================
        binding.webViewCam.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        if (targetIp.isNotEmpty()) {
            val streamUrl = "http://$targetIp:5000/video_feed"
            val html = "<html><body style='margin:0;background:#000;'><img src='$streamUrl' style='width:100%;height:100%;object-fit:contain;'/></body></html>"
            binding.webViewCam.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            isPolling = true
            startStatusPolling(targetIp)
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
        } catch (e: Exception) {
            Log.e("APO", "Snapshot failed: ${e.message}")
        }
    }

    private fun toggleRecording(ip: String) {
        isRecordingLocal = !isRecordingLocal
        val action = if (isRecordingLocal) "start" else "stop"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conn = URL("http://$ip:5000/record/$action").openConnection() as HttpURLConnection
                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().readText()

                    withContext(Dispatchers.Main) {
                        if (isRecordingLocal) {
                            binding.btnRecord.setColorFilter(Color.parseColor("#FF0000"))
                            binding.btnRecord.alpha = 0.5f
                            Toast.makeText(this@MainActivity, "🔴 Recording Started", Toast.LENGTH_SHORT).show()
                        } else {
                            binding.btnRecord.clearColorFilter()
                            binding.btnRecord.alpha = 1.0f
                            Toast.makeText(this@MainActivity, "✅ Stopped. Downloading via OS...", Toast.LENGTH_SHORT).show()

                            isPolling = false
                            val fetchingHtml = "<html><body style='margin:0;background:#000;display:flex;justify-content:center;align-items:center;height:100%;'><h3 style='color:white;text-align:center;font-family:sans-serif;'>Transferring Video...<br>Check Notifications</h3></body></html>"
                            binding.webViewCam.loadDataWithBaseURL(null, fetchingHtml, "text/html", "UTF-8", null)

                            try {
                                val json = org.json.JSONObject(responseText)
                                if (json.has("file")) {
                                    val filename = json.getString("file")
                                    downloadRecording(ip, filename)
                                } else {
                                    Log.w("APO", "No file attribute found in JSON response")
                                    restoreNetworkFeed(ip)
                                }
                            } catch (e: Exception) {
                                Log.e("APO", "Failed to parse recording response: ${e.message}")
                                restoreNetworkFeed(ip)
                            }
                        }
                    }
                } else {
                    Log.e("APO", "Server returned non-200 code: ${conn.responseCode}")
                    withContext(Dispatchers.Main) { restoreNetworkFeed(ip) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Connection Error", Toast.LENGTH_SHORT).show()
                    restoreNetworkFeed(ip)
                }
            }
        }
    }

    // BUG FIX: Completely offload the file transfer to Android's robust OS DownloadManager
    private fun downloadRecording(ip: String, filename: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Give the Raspberry Pi OS 3 full seconds to flush the video header to its SD card
                delay(3000)

                withContext(Dispatchers.Main) {
                    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    val uri = android.net.Uri.parse("http://$ip:5000/download/$filename")

                    val request = DownloadManager.Request(uri).apply {
                        setTitle(filename)
                        setDescription("Downloading Arithmos recording...")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_MOVIES, "Arithmos/Recordings/$filename")
                        setAllowedOverMetered(true)
                        setAllowedOverRoaming(true)
                    }

                    val downloadId = downloadManager.enqueue(request)

                    // Launch background monitor to wait for completion and trigger Pi cleanup
                    monitorDownload(downloadManager, downloadId, ip, filename)
                }
            } catch (e: Exception) {
                Log.e("APO", "Download setup failed: ${e.message}")
                withContext(Dispatchers.Main) { restoreNetworkFeed(ip) }
            }
        }
    }

    // Monitors the OS DownloadManager so we know when to delete the file off the Pi
    private fun monitorDownload(downloadManager: DownloadManager, downloadId: Long, ip: String, filename: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            var success = false
            var timeoutCounter = 0

            while (downloading && timeoutCounter < 180) { // Max 3 minutes wait
                timeoutCounter++
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex >= 0) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            success = true
                            downloading = false
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                        }
                    }
                }
                cursor?.close()

                if (downloading) {
                    delay(1000)
                }
            }

            if (success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "✅ Download Complete", Toast.LENGTH_SHORT).show()
                }
                try {
                    URL("http://$ip:5000/delete/$filename").openConnection().connect()
                } catch (e: Exception) {
                    Log.w("APO", "Failed to delete remote file")
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Download Failed or Timed Out", Toast.LENGTH_LONG).show()
                }
            }

            withContext(Dispatchers.Main) {
                restoreNetworkFeed(ip)
            }
        }
    }

    private fun restoreNetworkFeed(ip: String) {
        if (isFinishing || isDestroyed) return
        val streamUrl = "http://$ip:5000/video_feed"
        val html = "<html><body style='margin:0;background:#000;'><img src='$streamUrl' style='width:100%;height:100%;object-fit:contain;'/></body></html>"
        binding.webViewCam.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        if (!isPolling) {
            isPolling = true
            startStatusPolling(ip)
        }
    }

    private fun startStatusPolling(ip: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isPolling) {
                try {
                    val conn = URL("http://$ip:5000/status").openConnection() as HttpURLConnection
                    val text = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) {
                            val activity = json.optString("activity", "Unknown")
                            val timestamp = json.optString("timestamp", "--:--")
                            binding.tvEventText.text = "$activity at $timestamp"
                        }
                    }
                } catch (e: Exception) { }
                delay(1000)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }
}