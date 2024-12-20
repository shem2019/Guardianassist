package com.example.guardianassist

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Set welcome message
        val realName = sessionManager.fetchRealName()
        binding.dashboardTitle.text = "Welcome ${realName ?: "User"}"

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
        //Uniform check button
        binding.uniformcheck.setOnClickListener {
            val intent= Intent(this, Uniformcheck::class.java)
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
}
