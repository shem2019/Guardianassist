package com.example.guardianassist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.SaveEventRequest
import com.example.guardianassist.appctrl.SaveLogRequest
import com.example.guardianassist.databinding.ActivityUserDashboardBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var loadingDialog: AlertDialog
    private lateinit var sessionManager: SessionManager
    private var errorSound: MediaPlayer? = null
    private val totalDuration = 3600000L
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTimer: TextView
    private var countDownTimer: CountDownTimer? = null
    private var elapsedTime = 0L
    private var isCheckDue = false


    companion object {
        private const val CHANNEL_ID = "HourlyCheckReminder"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Set welcome message
        val realName = sessionManager.fetchRealName()
        binding.dashboardTitle.text = "Welcome ${realName ?: "User"}"

        // button
        binding.btnPatrol.setOnClickListener {
            val intent =Intent(this, MappingActivity::class.java)
            startActivity(intent)
        }
        binding.bookoff.setOnClickListener {
            val intent=Intent(this,ClockOutActivity::class.java)
            startActivity(intent)
        }

        // Initialize hourly check progress
        progressBar = findViewById(R.id.progressBarHourlyCheck)
        tvTimer = findViewById(R.id.tvTimer)

        startHourlyCheckCountdown()


        //create notification channel
        createNotificationChannel()

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Prepare PendingIntent for NFC foreground dispatch
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create a generic intent filter to capture all NFC tags
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Initialize error sound
        errorSound = MediaPlayer.create(this, R.raw.error)

        // Logout button
        binding.btnlogout.setOnClickListener {
            val token = sessionManager.fetchUserToken()
            if (token != null) {
                saveLog("User Logout", token)
            }
            sessionManager.clearSession()
            val intent = Intent(this, LandingPage::class.java)
            startActivity(intent)
            finish()
        }

        // Book On button
        binding.bookon.setOnClickListener {
            val intent = Intent(this, BookOnActivity::class.java)
            startActivity(intent)
        }

        binding.hourlycheck.setOnClickListener {
            val intent= Intent(this,HourlyCheckActivity::class.java)
            startActivity(intent)
            resetHourlyCheckUI()


        }
        //Uniform check button
        binding.uniformcheck.setOnClickListener {
            val intent= Intent(this, Uniformcheck::class.java)
            startActivity(intent)
        }
        binding.incident.setOnClickListener {
            val intent= Intent(this,IncidentReportActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveLog(eventType: String, token: String) {
        val logRequest = SaveLogRequest(event_type = eventType)

        RetrofitClient.apiService.saveLog("Bearer $token", logRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.e("LOG", "Failed to save log. Response code: ${response.code()} - ${response.errorBody()?.string()}")
                } else {
                    Log.i("LOG", "Log saved successfully")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LOG", "Error saving log: ${t.message}")
            }
        })
    }

    /// Hourly check
    private fun startHourlyCheckCountdown() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(totalDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime = totalDuration - millisUntilFinished
                val progress = ((elapsedTime.toFloat() / totalDuration) * 100).toInt()
                progressBar.progress = progress

                val minutesLeft = (millisUntilFinished / 60000).toInt()
                val secondsLeft = (millisUntilFinished % 60000 / 1000).toInt()
                tvTimer.text = String.format("%02d:%02d", minutesLeft, secondsLeft)

                if (minutesLeft == 5 && !isCheckDue) {
                    isCheckDue = true
                    showHourlyCheckAlert()
                }
            }

            override fun onFinish() {
                isCheckDue = false
                progressBar.progress = 100
                tvTimer.text = "00:00"
                resetHourlyCheck()
            }
        }.start()
    }

    private fun resetHourlyCheck() {
        Toast.makeText(this, "Hourly Check is due. Please complete it.", Toast.LENGTH_LONG).show()
        startHourlyCheckCountdown()
    }

    private fun showHourlyCheckAlert() {
        AlertDialog.Builder(this)
            .setTitle("Hourly Check Reminder")
            .setMessage("Your hourly check is due in 5 minutes. Please complete it soon!")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hourly Check Reminder"
            val descriptionText = "Channel for hourly check reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission required for NFC and Audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun resetHourlyCheckUI() {
        val hourlyCheckCard = binding.hourlycheck
        val progressBar = binding.hourlycheck.findViewById<ProgressBar>(R.id.progressBarHourlyCheck)

        hourlyCheckCard.setCardBackgroundColor(Color.WHITE)
        progressBar.progress = 0
    }

}
