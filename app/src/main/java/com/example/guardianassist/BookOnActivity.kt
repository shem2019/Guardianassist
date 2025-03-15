package com.example.guardianassist

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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.appctrl.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class BookOnActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var sessionManager: SessionManager
    private lateinit var tvBookingStatus: TextView
    private lateinit var ivBookingStatus: ImageView
    private lateinit var tvOrganizationName: TextView
    private lateinit var tvSiteName: TextView
    private lateinit var loadingDialog: AlertDialog
    private lateinit var tvBookingTime: TextView
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var isWaitingForTag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_on)

        sessionManager = SessionManager(this)

        // UI Elements
        tvBookingStatus = findViewById(R.id.tvBookingStatus)
        ivBookingStatus = findViewById(R.id.ivBookingStatus)
        tvOrganizationName = findViewById(R.id.tvOrganizationName)
        tvSiteName = findViewById(R.id.tvSiteName)
        tvBookingTime=findViewById(R.id.tvBookingTime)
        val btnScanNfc = findViewById<Button>(R.id.btnScanNfc)

        // Load organization & site details from session
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

        // Check if user has already booked on
        checkBookingStatus()
    }

    private fun loadStoredDetails() {
        val orgName = sessionManager.fetchOrgName() ?: "Unknown Organization"
        val siteName = sessionManager.fetchSiteName() ?: "Unknown Site" // ✅ Fetch stored site_name

        tvOrganizationName.text = "Organization: $orgName"
        tvSiteName.text = "Site: $siteName"

        Log.d("BookOn", "Loaded from SessionManager: org=$orgName, site=$siteName")
    }


    private fun checkBookingStatus() {
        val token = sessionManager.fetchUserToken() ?: return
        val userId = sessionManager.fetchUserId()
        val siteId = sessionManager.fetchSiteId() // ✅ Ensure site ID is retrieved correctly
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // ✅ Ensure date is a String

        Log.d("CheckBookOn", "Starting booking check for user_id=$userId, site_id=$siteId, date=$today")

        RetrofitClient.apiService.checkBookOnStatus(
            token = "Bearer $token",
            userId = userId.toString(),
            siteId = siteId,
            date = today
        ).enqueue(object : Callback<BookOnStatusResponse> {
            override fun onResponse(call: Call<BookOnStatusResponse>, response: Response<BookOnStatusResponse>) {
                Log.d("CheckBookOn", "API Response Code: ${response.code()}")
                val responseBody = response.body()
                Log.d("CheckBookOn", "API Response Body: $responseBody")

                if (response.isSuccessful && responseBody?.success == true) {
                    val isBookedOn = responseBody.isBookedOn
                    val retrievedSiteId = responseBody.siteId ?: -1
                    val tagName = responseBody.clockInTag ?: "Unknown"
                    val bookingTime = responseBody.clockInTime ?: "N/A"

                    // ✅ Fetch site name if siteId is available

                    fetchAndStoreSiteName(retrievedSiteId)

                    if (isBookedOn) {
                        sessionManager.saveSiteId(retrievedSiteId)
                        sessionManager.saveClockInTag(tagName)

                        tvBookingStatus.text = "Already Booked On"
                        tvBookingStatus.setTextColor(Color.GREEN)
                        ivBookingStatus.setImageResource(R.drawable.tick)
                        tvBookingTime.text = "Booking Time: $bookingTime"
                    } else {
                        Log.e("CheckBookOn", "User is NOT booked on")
                        tvBookingStatus.text = "Not yet booked on"
                        tvBookingStatus.setTextColor(Color.RED)
                        ivBookingStatus.setImageResource(R.drawable.cancel)
                        tvBookingTime.text = "Booking Time: Not yet booked on"
                    }
                } else {
                    Log.e("CheckBookOn", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(this@BookOnActivity, "Failed to check booking status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookOnStatusResponse>, t: Throwable) {
                Log.e("CheckBookOn", "Network Error: ${t.message}", t)
            }
        })
    }
    private fun fetchAndStoreSiteName(siteId: Int) {
        val token = sessionManager.fetchUserToken() ?: return

        Log.d("BookOn", "Fetching site name for siteId=$siteId")

        RetrofitClient.apiService.getSites(sessionManager.fetchOrgId()).enqueue(object : Callback<SiteResponse> {
            override fun onResponse(call: Call<SiteResponse>, response: Response<SiteResponse>) {
                if (response.isSuccessful) {
                    val siteList = response.body()?.sites ?: emptyList()
                    val selectedSite = siteList.find { it.site_id == siteId }

                    if (selectedSite != null) {
                        Log.d("BookOn", "Site Name Retrieved: ${selectedSite.site_name}")

                        // ✅ Save the site name in session
                        sessionManager.saveSiteName(selectedSite.site_name)

                        Log.d("BookOn", "Site Name saved in SessionManager: ${selectedSite.site_name}")

                        // ✅ Update UI with the site name
                        tvSiteName.text = "Site: ${selectedSite.site_name}"
                    } else {
                        Log.e("BookOn", "Site ID found, but Site Name is NULL")
                    }
                }
            }

            override fun onFailure(call: Call<SiteResponse>, t: Throwable) {
                Log.e("BookOn", "Error fetching sites: ${t.message}")
            }
        })
    }

    private fun startWaitingForTag() {
        isWaitingForTag = true

        // Show NFC scanning dialog
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

        // Set timeout for NFC scanning
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
                    val tagName = parts[0].trim()  // "Main Gate"
                    val tagType = parts[1].trim()  // "Clock In"
                    val siteId = parts[2].trim().toIntOrNull()  // "2"

                    if (siteId != null) {
                        sessionManager.saveSiteId(siteId)

                        Log.d("BookOn", "Site ID saved in SessionManager: $siteId")
                        processBookOn(tagName, tagType, siteId)
                    } else {
                        Toast.makeText(this, "Invalid site ID in NFC tag", Toast.LENGTH_SHORT).show()
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

    private fun processBookOn(tagName: String, tagType: String, siteId: Int) {
        val userId = sessionManager.fetchUserId()
        val orgId = sessionManager.fetchOrgId()
        val token = sessionManager.fetchUserToken() ?: return
        sessionManager.saveSiteId(siteId)

        Log.d("BookOn", "Processing Book On: TagName=$tagName, TagType=$tagType, SiteId=$siteId")

        if (userId == -1 || siteId == -1 || orgId == -1) {
            Log.e("BookOn", "Missing user/site/org details - userId: $userId, siteId: $siteId, orgId: $orgId")
            Toast.makeText(this, "Missing user or site details", Toast.LENGTH_SHORT).show()
            return
        }

        val request = BookOnRequest(userId, siteId, orgId, tagType)

        Log.d("BookOn", "Sending BookOn request: $request")

        RetrofitClient.apiService.bookOn("Bearer $token", request).enqueue(object : Callback<BookOnResponse> {
            override fun onResponse(call: Call<BookOnResponse>, response: Response<BookOnResponse>) {
                Log.d("BookOn", "API Response Code: ${response.code()}")

                if (response.isSuccessful) {
                    val bookOnResponse = response.body()
                    if (bookOnResponse?.success == true) {
                        Log.d("BookOn", "Book On Successful: ${bookOnResponse.message}")

                        // ✅ Save site details in session
                        sessionManager.saveSiteId(siteId)
                        //sessionManager.saveSiteName(bookOnResponse.siteName ?: "Unknown Site")

                        Toast.makeText(this@BookOnActivity, bookOnResponse.message, Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("BookOn", "Book On Failed: ${bookOnResponse?.message}")
                        Toast.makeText(this@BookOnActivity, "Failed to Book On", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("BookOn", "Failed Response: ${response.errorBody()?.string()}")
                    Toast.makeText(this@BookOnActivity, "Failed to Book On. Try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookOnResponse>, t: Throwable) {
                Log.e("BookOn", "Network Error: ${t.message}", t)
                Toast.makeText(this@BookOnActivity, "Network error, check your connection", Toast.LENGTH_SHORT).show()
            }
        })
    }





}
