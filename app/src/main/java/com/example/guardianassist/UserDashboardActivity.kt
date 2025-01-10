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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
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
    private var isWaitingForTag = false
    private lateinit var loadingDialog: AlertDialog
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private lateinit var sessionManager: SessionManager
    private var errorSound: MediaPlayer? = null
    private var elapsedSeconds = 0
    private val totalDuration = 3600 // 1 hour in seconds
    private val handler = Handler(Looper.getMainLooper())
    private var isHourlyCheckHighlighted = false
    private var mediaPlayer: MediaPlayer? = null
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

        // Initialize hourly check progress
        startHourlyCheckService()
        initializeHourlyCheck()
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
            startWaitingForTag()
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

    private fun startWaitingForTag() {
        isWaitingForTag = true

        // Show loading dialog
        loadingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting to Scan NFC Tag")
            .setMessage("Place your device near an NFC tag...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        setupNfc()

        // Enable NFC foreground dispatch
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)

        // Set a timeout to stop waiting after 5 seconds
        timeoutHandler.postDelayed({
            if (isWaitingForTag) {
                stopWaitingForTag()
                Toast.makeText(this, "NFC scan timed out.", Toast.LENGTH_SHORT).show()
            }
        }, 5000)
    }

    private fun stopWaitingForTag() {
        isWaitingForTag = false

        // Dismiss loading dialog
        if (this::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isWaitingForTag) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isWaitingForTag && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            } else {
                Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        stopWaitingForTag()

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val ndefMessage: NdefMessage? = ndef.ndefMessage
                ndef.close()

                if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                    val record: NdefRecord = ndefMessage.records[0]
                    val payload = String(record.payload, Charsets.UTF_8)
                    val cleanedPayload = payload.substring(3) // Skip the language code prefix

                    if (cleanedPayload.contains("Book On", ignoreCase = true)) {
                        // Extract site name from payload
                        val siteName = cleanedPayload.replace("Book On", "").trim()
                        saveEvent("Book On", siteName)
                        Toast.makeText(this, "Book On successful for site: $siteName", Toast.LENGTH_LONG).show()
                    } else {
                        errorSound?.start()
                        Toast.makeText(this, "This is not a Book On tag", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "NFC tag is empty.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "This tag does not support NDEF.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error reading NFC tag", e)
            Toast.makeText(this, "Error reading NFC tag.", Toast.LENGTH_SHORT).show()
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



    private fun saveEvent(eventType: String, siteName: String) {
        val token = sessionManager.fetchUserToken()
        val realName = sessionManager.fetchRealName()
        val eventTime = Calendar.getInstance().time.toString()

        val eventRequest = SaveEventRequest(eventType, siteName, realName ?: "Unknown", eventTime)
        RetrofitClient.apiService.saveEvent("Bearer $token", eventRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.e("EVENT", "Failed to save event")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("EVENT", "Error saving event: ${t.message}")
            }
        })
    }
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_PERMISSION_CODE
                )
            }
        }
    }

    private fun startHourlyCheckService() {
        val serviceIntent = Intent(this, HourlyCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun initializeHourlyCheck() {
        val progressBar = binding.hourlycheck.findViewById<ProgressBar>(R.id.progressBarHourlyCheck)

        // Start updating the progress bar
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (elapsedSeconds < totalDuration) {
                    elapsedSeconds++
                    progressBar.progress = elapsedSeconds

                    // Update progress bar and card status
                    if (elapsedSeconds == totalDuration) {
                        highlightHourlyCheck()
                    }

                    handler.postDelayed(this, 1000) // Repeat every second
                }
            }
        }, 1000)
    }

    private fun highlightHourlyCheck() {
        if (!isHourlyCheckHighlighted) {
            val hourlyCheckCard = binding.hourlycheck
            hourlyCheckCard.setCardBackgroundColor(Color.parseColor("#FFDD57")) // Highlight in yellow
            isHourlyCheckHighlighted = true

            // Show alert dialog, play sound, and send notification
            showHourlyCheckDialog()
            playReminderSound()
            showNotification()
        }
    }

    private fun showHourlyCheckDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hourly Check Due")
            .setMessage("It's time to perform the hourly check. Please proceed.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .create()
        dialog.show()
    }
    private fun playReminderSound() {
        // Initialize MediaPlayer with the sound resource
        mediaPlayer = MediaPlayer.create(this, R.raw.error)
        mediaPlayer?.start()
    }


    private fun showNotification() {
        val intent = Intent(this, UserDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Hourly Check Reminder")
            .setContentText("It's time to perform your hourly check.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
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



    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
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
