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
        // 1. SIDE MENU & HEADER LOGIC
        // ==========================================
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<ImageButton>(R.id.btnCloseDrawer).setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, ConnectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // ==========================================
        // 2. DASHBOARD BUTTONS
        // ==========================================
        binding.btnSnapshot.setOnClickListener { takeSnapshot() }
        binding.btnRecord.setOnClickListener { toggleRecording(targetIp) }

        // ==========================================
        // 3. NAVIGATION DRAWER ITEMS
        // ==========================================
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertHistoryActivity::class.java))
                }
                R.id.nav_playback -> {
                    startActivity(Intent(this, PlaybackHistoryActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                // UPDATED: Now launches the professional TermsActivity instead of a dialog
                R.id.nav_terms -> {
                    startActivity(Intent(this, TermsActivity::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        setupWebView(targetIp)
    }

    // --- CAMERA LOGIC ---
    private fun setupWebView(ip: String) {
        binding.webViewCam.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        if (ip.isNotEmpty()) {
            val streamUrl = "http://$ip:5000/video_feed"
            // Revised HTML with #D9D9D9 background to match the new layout
            val html = "<html><body style='margin:0;background:#D9D9D9;'><img src='$streamUrl' style='width:100%;height:100%;object-fit:contain;'/></body></html>"
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
            } catch (e: Exception) {
                Log.e("APO", "Record error: ${e.message}")
            }
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
                        binding.tvEventText.text = "${json.getString("activity")} at ${json.getString("timestamp")}"
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