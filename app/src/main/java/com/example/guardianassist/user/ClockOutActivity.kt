package com.example.guardianassist.user

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.ClockOutRequest
import com.example.guardianassist.appctrl.ClockOutResponse
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset

class ClockOutActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var sessionManager: SessionManager
    private lateinit var tvClockOutStatus: TextView
    private lateinit var ivClockOutStatus: ImageView
    private lateinit var tvOrganizationName: TextView
    private lateinit var tvSiteName: TextView
    private lateinit var tvClockInTime: TextView
    private lateinit var tvRealName: TextView
    private lateinit var loadingDialog: AlertDialog
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var isWaitingForTag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock_out)

        sessionManager = SessionManager(this)

        // UI Elements
        tvClockOutStatus = findViewById(R.id.tvClockOutStatus)
        ivClockOutStatus = findViewById(R.id.ivClockOutStatus)
        tvOrganizationName = findViewById(R.id.tvOrganizationName)
        tvSiteName = findViewById(R.id.tvSiteName)
        tvClockInTime = findViewById(R.id.tvClockInTime)
        tvRealName = findViewById(R.id.tvRealName)
        val btnScanNfc = findViewById<Button>(R.id.btnScanNfc)

        // Load session details
        loadStoredDetails()

        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Button to start scanning NFC
        btnScanNfc.setOnClickListener {
            startWaitingForTag()
        }
    }

    private fun loadStoredDetails() {
        val orgName = sessionManager.fetchOrgName() ?: "Unknown Organization"
        val siteName = sessionManager.fetchSiteName() ?: "Unknown Site"
        val bookOnTime = sessionManager.fetchBookOnTime() ?: "Not Available"
        val realName = sessionManager.fetchRealName() ?: "Unknown User"

        tvOrganizationName.text = "Organization: $orgName"
        tvSiteName.text = "Site: $siteName"
        tvClockInTime.text = "Clock In Time: $bookOnTime"
        tvRealName.text = "Name: $realName"

        Log.d("ClockOut", "Loaded from SessionManager: org=$orgName, site=$siteName, bookOnTime=$bookOnTime")
    }

    private fun startWaitingForTag() {
        isWaitingForTag = true

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_waiting_nfc, null)
        loadingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting for NFC Tag")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> stopWaitingForTag() }
            .create()
        loadingDialog.show()

        // Enable NFC scanning
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)

        timeoutHandler.postDelayed({
            if (isWaitingForTag) {
                stopWaitingForTag()
                Toast.makeText(this, "NFC scan timed out.", Toast.LENGTH_SHORT).show()
            }
        }, 10000)
    }

    private fun stopWaitingForTag() {
        isWaitingForTag = false
        if (this::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (isWaitingForTag && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        stopWaitingForTag()

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            val ndefMessage: NdefMessage? = ndef.ndefMessage
            ndef.close()

            if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                val record: NdefRecord = ndefMessage.records[0]
                val payload = String(record.payload, Charset.forName("UTF-8")).substring(3)

                val parts = payload.split(",")
                if (parts.size == 3) {
                    val tagName = parts[0].trim()
                    val tagType = parts[1].trim()
                    val siteId = parts[2].trim().toIntOrNull()

                    if (siteId != null && tagType.equals("Clock Out", ignoreCase = true)) {
                        processClockOut(tagName, siteId)
                    } else {
                        Toast.makeText(this, "Invalid Clock-Out Tag", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Invalid NFC tag format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "NFC tag is empty.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "This tag does not support NDEF.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processClockOut(tagName: String, siteId: Int) {
        val userId = sessionManager.fetchUserId() // ✅ Fetch from session manager
        val orgId = sessionManager.fetchOrgId()   // ✅ Fetch from session manager

        Log.d("ClockOut", "Processing Clock Out: User ID=$userId, Site ID=$siteId, Org ID=$orgId, Tag=$tagName")

        // ✅ Validate required fields before making API call
        if (userId == -1 || siteId == -1 || orgId == -1 || tagName.isBlank()) {
            Log.e("ClockOut", "Validation failed: Missing required fields")
            Toast.makeText(this, "Error: Missing required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ClockOutRequest(userId, siteId, orgId, tagName)

        Log.d("ClockOut", "Sending ClockOut request: $request")

        RetrofitClient.apiService.clockOut(request).enqueue(object : Callback<ClockOutResponse> {
            override fun onResponse(call: Call<ClockOutResponse>, response: Response<ClockOutResponse>) {
                Log.d("ClockOut", "API Response Code: ${response.code()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("ClockOut", "Clock Out Successful: ${response.body()?.message}")

                    // ✅ Update session manager to mark user as OFF-SITE
                    sessionManager.saveIsOnSite(false)

                    tvClockOutStatus.text = "Successfully Clocked Out"
                    tvClockOutStatus.setTextColor(Color.GREEN)
                    ivClockOutStatus.setImageResource(R.drawable.tick)

                    Toast.makeText(this@ClockOutActivity, "Clocked Out Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e("ClockOut", "Clock Out Failed: ${response.body()?.message}")
                    Toast.makeText(this@ClockOutActivity, "Failed to Clock Out", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ClockOutResponse>, t: Throwable) {
                Log.e("ClockOut", "Network Error: ${t.message}", t)
                Toast.makeText(this@ClockOutActivity, "Network error, check your connection", Toast.LENGTH_SHORT).show()
            }
        })
    }


}