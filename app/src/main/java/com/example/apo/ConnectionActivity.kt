package com.example.apo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class ConnectionActivity : AppCompatActivity() {

    private lateinit var imgLogo: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var cardDeviceList: ScrollView

    private lateinit var btnDevice1: LinearLayout
    private lateinit var btnDevice2: LinearLayout
    private lateinit var btnDevice3: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        imgLogo = findViewById(R.id.imgLogo)
        tvStatus = findViewById(R.id.tvStatus)
        cardDeviceList = findViewById(R.id.cardDeviceList)

        btnDevice1 = findViewById(R.id.btnDevice1)
        btnDevice2 = findViewById(R.id.btnDevice2)
        btnDevice3 = findViewById(R.id.btnDevice3)

        simulateDeviceScan()
    }

    private fun simulateDeviceScan() {
        tvStatus.visibility = View.VISIBLE
        cardDeviceList.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            delay(2500)

            imgLogo.visibility = View.GONE
            tvStatus.visibility = View.GONE
            cardDeviceList.visibility = View.VISIBLE

            setupDeviceButtons()
        }
    }

    private fun setupDeviceButtons() {
        val emulatorHostIp = "10.0.2.2"

        val clickListener = View.OnClickListener { view ->
            val deviceName = when (view.id) {
                R.id.btnDevice1 -> "APo-LivingRoom-A1"
                R.id.btnDevice2 -> "APo-Bedroom-B3"
                R.id.btnDevice3 -> "APo-Kitchen-C2"
                else -> "APo Device"
            }

            Toast.makeText(this@ConnectionActivity, "Connecting to $deviceName...", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@ConnectionActivity, MainActivity::class.java)
            intent.putExtra("TARGET_IP", emulatorHostIp)
            startActivity(intent)
            finish()
        }

        btnDevice1.setOnClickListener(clickListener)
        btnDevice2.setOnClickListener(clickListener)
        btnDevice3.setOnClickListener(clickListener)
    }
}