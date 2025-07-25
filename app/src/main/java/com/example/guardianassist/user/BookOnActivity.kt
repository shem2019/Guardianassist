package com.example.guardianassist.user

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
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
    private lateinit var tvBookingTime: TextView
    private lateinit var btnScanNfc: MaterialButton
    private lateinit var progressScanning: CircularProgressIndicator
    private lateinit var tvOrgName: TextView
    private lateinit var tvSiteName: TextView

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var isWaitingForTag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_on)

        sessionManager = SessionManager(this)

        // Bind UI
        tvBookingStatus  = findViewById(R.id.tvBookingStatus)
        ivBookingStatus  = findViewById(R.id.ivBookingStatus)
        tvBookingTime    = findViewById(R.id.tvBookingTime)
        btnScanNfc       = findViewById(R.id.btnScanNfc)
        progressScanning = findViewById(R.id.progressScanning)
        tvOrgName        = findViewById(R.id.tvOrganizationName)
        tvSiteName       = findViewById(R.id.tvSiteName)

        // NFC setup
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            ?: return finish().also {
                Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show()
            }
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        btnScanNfc.setOnClickListener { startWaitingForTag() }

        // Initial load
        loadStoredDetails()
        checkBookingStatus()
    }

    private fun loadStoredDetails() {
        tvOrgName.text  = "Organization: ${sessionManager.fetchOrgName() ?: "—"}"
        tvSiteName.text = "Site: ${sessionManager.fetchSiteName() ?: "—"}"
    }

    private fun checkBookingStatus() {
        val token  = sessionManager.fetchUserToken() ?: return
        val userId = sessionManager.fetchUserId()
        val siteId = sessionManager.fetchSiteId()
        val today  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        RetrofitClient.apiService.checkBookOnStatus(
            token  = "Bearer $token",
            userId = userId.toString(),
            siteId = siteId,
            date   = today
        ).enqueue(object: Callback<BookOnStatusResponse> {
            override fun onResponse(call: Call<BookOnStatusResponse>, resp: Response<BookOnStatusResponse>) {
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val body = resp.body()!!
                    sessionManager.saveBookOnTime(body.clockInTime ?: "")
                    sessionManager.saveOnSiteStatus(body.isBookedOn)
                    fetchAndStoreSiteName(body.siteId ?: siteId)
                    updateUIForStatus(body.isBookedOn)
                } else {
                    Toast.makeText(this@BookOnActivity, "Failed to get status", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<BookOnStatusResponse>, t: Throwable) {
                Log.e("CheckBookOn", "Error", t)
                Toast.makeText(this@BookOnActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAndStoreSiteName(siteId: Int) {
        RetrofitClient.apiService.getSites(sessionManager.fetchOrgId())
            .enqueue(object: Callback<SiteResponse> {
                override fun onResponse(call: Call<SiteResponse>, resp: Response<SiteResponse>) {
                    if (resp.isSuccessful) {
                        resp.body()?.sites
                            ?.find { it.site_id == siteId }
                            ?.let {
                                sessionManager.saveSiteName(it.site_name)
                                tvSiteName.text = "Site: ${it.site_name}"
                            }
                    }
                }
                override fun onFailure(call: Call<SiteResponse>, t: Throwable) {
                    Log.e("FetchSite", "Error", t)
                }
            })
    }

    private fun updateUIForStatus(isOn: Boolean) {
        if (isOn) {
            ivBookingStatus.setImageResource(R.drawable.tick)
            tvBookingStatus.text = "BOOKED ON"
            tvBookingTime.text   = "Since: ${sessionManager.fetchBookOnTime() ?: "—"}"
            btnScanNfc.text      = "Scan NFC to Book Off"
            btnScanNfc.setBackgroundTintList(getColorStateList(R.color.red))
        } else {
            ivBookingStatus.setImageResource(R.drawable.cancel)
            tvBookingStatus.text = "NOT BOOKED ON"
            tvBookingTime.text   = "—"
            btnScanNfc.text      = "Scan NFC to Book On"
            btnScanNfc.setBackgroundTintList(getColorStateList(R.color.colorPrimary))
        }
    }

    private fun startWaitingForTag() {
        isWaitingForTag = true
        progressScanning.visibility = android.view.View.VISIBLE
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        timeoutHandler.postDelayed({
            if (isWaitingForTag) {
                stopWaitingForTag()
                Toast.makeText(this, "NFC scan timed out.", Toast.LENGTH_SHORT).show()
            }
        }, 10_000)
    }

    private fun stopWaitingForTag() {
        isWaitingForTag = false
        progressScanning.visibility = android.view.View.GONE
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isWaitingForTag && intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let {
                stopWaitingForTag()
                handleNfcTag(it)
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        Ndef.get(tag)?.let { ndef ->
            ndef.connect()
            val msg = ndef.ndefMessage
            ndef.close()
            msg?.records?.firstOrNull()?.let { rec ->
                String(rec.payload, Charset.forName("UTF-8"))
                    .substring(3)
                    .split(",")
                    .takeIf { it.size == 3 }
                    ?.let { parts ->
                        val tagType = parts[1].trim()
                        val siteId  = parts[2].trim().toIntOrNull() ?: return@let
                        sessionManager.saveSiteId(siteId)
                        processBookOn(tagType, siteId)
                    }
            }
        } ?: run {
            Toast.makeText(this, "Tag not NDEF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processBookOn(tagType: String, siteId: Int) {
        val userId = sessionManager.fetchUserId()
        val orgId  = sessionManager.fetchOrgId()
        val token  = sessionManager.fetchUserToken() ?: return

        RetrofitClient.apiService.bookOn(
            "Bearer $token",
            BookOnRequest(userId, siteId, orgId, tagType)
        ).enqueue(object: Callback<BookOnResponse> {
            override fun onResponse(call: Call<BookOnResponse>, resp: Response<BookOnResponse>) {
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val nowOn = tagType.contains("In")
                    sessionManager.saveOnSiteStatus(nowOn)
                    // leave saved time as-is; UI will show last known
                    updateUIForStatus(nowOn)
                    Toast.makeText(this@BookOnActivity, resp.body()!!.message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@BookOnActivity, "Failed to Book On/Off", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<BookOnResponse>, t: Throwable) {
                Log.e("BookOn", "Network Error", t)
                Toast.makeText(this@BookOnActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
