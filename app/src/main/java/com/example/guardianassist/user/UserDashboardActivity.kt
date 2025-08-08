package com.example.guardianassist.user

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.LandingPage
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SaveLogRequest
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.databinding.ActivityUserDashboardBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var sessionManager: SessionManager
    private var errorSound: MediaPlayer? = null


    companion object {
        private const val REQUEST_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Welcome text
        binding.dashboardTitle.text =
            "Welcome ${sessionManager.fetchRealName() ?: "User"}"

        // Immediately fetch today’s session status
        BookOnActivity.fetchSessionStatus(
            this,
            sessionManager
        ) { isOnSite ->
            //binding.bookon.isEnabled = !isOnSite
           // binding.bookoff.isEnabled = isOnSite
        }

        // Patrol button
        binding.patrol.setOnClickListener {
            startActivity(Intent(this, PatrolActivity::class.java))
        }

        // Book On
        binding.bookon.setOnClickListener {
            startActivity(Intent(this, BookOnActivity::class.java))
        }

        // Book Off (Clock Out)
        binding.bookoff.setOnClickListener {
            startActivity(Intent(this, ClockOutActivity::class.java))
        }

        // Hourly check UI
        binding.hourlycheck.setOnClickListener {
            startActivity(Intent(this, HourlyCheckActivity::class.java))

        }

        // Uniform check
        binding.uniformcheck.setOnClickListener {
            startActivity(Intent(this, Uniformcheck::class.java))
        }

        // Incident
        binding.incident.setOnClickListener {
            startActivity(Intent(this, IncidentReportActivity::class.java))
        }

        // Logout
        binding.btnlogout.setOnClickListener {
            sessionManager.fetchUserToken()?.let { token ->
                saveLog("User Logout", token)
            }
            sessionManager.clearSession()
            startActivity(Intent(this, LandingPage::class.java))
            finish()
        }

        // Initialize error sound
        errorSound = MediaPlayer.create(this, R.raw.error)

        // Start hourly countdown

    }

    private fun saveLog(eventType: String, token: String) {
        val req = SaveLogRequest(event_type = eventType)
        RetrofitClient.apiService.saveLog("Bearer $token", req)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, resp: Response<Void>) {
                    if (!resp.isSuccessful) {
                        Log.e("LOG", "SaveLog failed: ${resp.code()}")
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("LOG", "Error saving log", t)
                }
            })
    }

    override fun onRequestPermissionsResult(
        req: Int, perms: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(req, perms, results)
        if (req == REQUEST_PERMISSION_CODE &&
            (results.isEmpty() || results[0] != PackageManager.PERMISSION_GRANTED)
        ) {
            Toast.makeText(
                this,
                "Permission required for NFC & audio",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
