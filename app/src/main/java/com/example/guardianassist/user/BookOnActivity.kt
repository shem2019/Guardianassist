package com.example.guardianassist.user

import android.app.PendingIntent
import android.content.Context
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
import androidx.core.content.ContextCompat
import com.example.guardianassist.HourlyCheckService
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
import java.util.concurrent.TimeUnit

class BookOnActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>

    private lateinit var ivBookingStatus: ImageView
    private lateinit var tvBookingStatus: TextView
    private lateinit var tvBookingTime: TextView
    private lateinit var tvLiveTimer: TextView
    private lateinit var btnScanNfc: MaterialButton
    private lateinit var progressScanning: CircularProgressIndicator
    private lateinit var tvOrgName: TextView
    private lateinit var tvSiteName: TextView

    private var isWaitingForTag = false
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timerHandler   = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            updateLiveTimer()
            timerHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        /**
         * Fetch today’s sessions for the given context & sessionManager,
         * then invoke `onResult(isOnSite: Boolean)` with the result.
         */
        fun fetchSessionStatus(
            context: Context,
            sessionManager: SessionManager,
            onResult: (isOnSite: Boolean) -> Unit
        ) {
            val token  = sessionManager.fetchUserToken() ?: return onResult(false)
            val userId = sessionManager.fetchUserId()
            val siteId = sessionManager.fetchSiteId()
            val today  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            RetrofitClient.apiService.getSessionHistory(
                token  = "Bearer $token",
                userId = userId,
                siteId = siteId,
                date   = today
            ).enqueue(object : Callback<SessionHistoryResponse> {
                override fun onResponse(
                    call: Call<SessionHistoryResponse>,
                    resp: Response<SessionHistoryResponse>
                ) {
                    if (!resp.isSuccessful || resp.body()?.success != true) {
                        sessionManager.saveOnSiteStatus(false)
                        return onResult(false)
                    }
                    val open = resp.body()!!.sessions.firstOrNull { it.clock_out_time == null }
                    if (open != null) {
                        sessionManager.saveSiteId(open.site_id)
                        sessionManager.saveSiteName(open.site_name ?: "Site ${open.site_id}")
                        sessionManager.saveClockInTag(open.clock_in_tag)
                        sessionManager.saveBookOnTime(open.clock_in_time)
                        sessionManager.saveOnSiteStatus(true)
                        sessionManager.saveBookOnTime(open.clock_in_time)
                        sessionManager.saveLastHourlyCheckTime(open.clock_in_time)
                        // set first hourly‐due = bookOn + 1h
                        val baseMs = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(open.clock_in_time)!!.time
                        sessionManager.saveNextHourlyDueTime(baseMs + TimeUnit.HOURS.toMillis(1))
                        // **START the HourlyCheckService** as a foreground service
                        Intent(context, HourlyCheckService::class.java).also { svc ->
                            ContextCompat.startForegroundService(context, svc)
                        }


                        onResult(true)

                    } else {
                        sessionManager.saveOnSiteStatus(false)
                        onResult(false)
                    }
                }

                override fun onFailure(call: Call<SessionHistoryResponse>, t: Throwable) {
                    Log.e("SessionHistory", "Error", t)
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    sessionManager.saveOnSiteStatus(false)
                    onResult(false)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_on)

        sessionManager = SessionManager(this)

        ivBookingStatus  = findViewById(R.id.ivBookingStatus)
        tvBookingStatus  = findViewById(R.id.tvBookingStatus)
        tvBookingTime    = findViewById(R.id.tvBookingTime)
        tvLiveTimer      = findViewById(R.id.tvLiveTimer)
        btnScanNfc       = findViewById(R.id.btnScanNfc)
        progressScanning = findViewById(R.id.progressScanning)
        tvOrgName        = findViewById(R.id.tvOrganizationName)
        tvSiteName       = findViewById(R.id.tvSiteName)


        // Show stored org immediately
        tvOrgName.text = "Organization: ${sessionManager.fetchOrgName() ?: "—"}"

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

        // Fetch session status and update UI
        fetchSessionStatus(this, sessionManager) { isOn ->
            updateUI(isOn)
            if (isOn) {
                tvSiteName.text = "Site: ${sessionManager.fetchSiteName()}"
            }
        }
    }

    private fun updateUI(isOn: Boolean) {
        timerHandler.removeCallbacks(timerRunnable)

        if (isOn) {
            ivBookingStatus.setImageResource(R.drawable.tick)
            tvBookingStatus.text = "BOOKED ON"
            btnScanNfc.isEnabled = false
            btnScanNfc.text      = "Already Clocked In"

            // Static since time
            val since = sessionManager.fetchBookOnTime() ?: "—"
            tvBookingTime.text = "Since: $since"

            // Start live timer
            timerHandler.post(timerRunnable)
        } else {
            ivBookingStatus.setImageResource(R.drawable.cancel)
            tvBookingStatus.text  = "NOT BOOKED ON"
            tvBookingTime.text    = "Since: —"
            tvLiveTimer.text      = "Time In: 00:00:00"
            btnScanNfc.isEnabled  = true
            btnScanNfc.text       = "Scan NFC to Book On"
            tvSiteName.text       = "Site: —"
        }
    }

    private fun updateLiveTimer() {
        val sinceStr = sessionManager.fetchBookOnTime() ?: return
        val fmt      = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val start    = try { fmt.parse(sinceStr) } catch (_: Exception) { null } ?: return
        val diff     = System.currentTimeMillis() - start.time

        val h = TimeUnit.MILLISECONDS.toHours(diff)
        val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        tvLiveTimer.text = String.format("Time In: %02d:%02d:%02d", h, m, s)
    }

    private fun startWaitingForTag() {
        isWaitingForTag = true
        progressScanning.visibility = android.view.View.VISIBLE
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        Handler(Looper.getMainLooper()).postDelayed({
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
                val parts = String(rec.payload, Charset.forName("UTF-8"))
                    .substring(3)
                    .split(",")
                if (parts.size == 3) {
                    val tagName = parts[0].trim()
                    val tagType = parts[1].trim()
                    val siteId  = parts[2].trim().toIntOrNull() ?: return@let

                    if (!tagType.equals("Clock In", ignoreCase = true)) {
                        Toast.makeText(this,
                            "Please scan a Clock In tag",
                            Toast.LENGTH_LONG).show()
                        return@let
                    }
                    sessionManager.saveSiteId(siteId)
                    processClockIn(siteId, tagName)
                }
            }
        }
    }

    private fun processClockIn(siteId: Int, tagName: String) {
        val userId = sessionManager.fetchUserId()
        val orgId  = sessionManager.fetchOrgId()
        val token  = sessionManager.fetchUserToken() ?: return

        RetrofitClient.apiService.clockIn(
            "Bearer $token",
            BookSessionRequest(userId, orgId, siteId, tagName)
        ).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(
                call: Call<ApiResponse>,
                resp: Response<ApiResponse>
            ) {
                if (resp.isSuccessful && resp.body()?.success == true) {
                    // re-fetch to refresh UI & timers
                    fetchSessionStatus(this@BookOnActivity, sessionManager) { isOn ->
                        updateUI(isOn)
                        if (isOn) {
                            tvSiteName.text = "Site: ${sessionManager.fetchSiteName()}"
                        }
                    }
                    Toast.makeText(this@BookOnActivity,
                        resp.body()!!.message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@BookOnActivity,
                        resp.body()?.message ?: "Clock-in failed",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("ClockIn", "Error", t)
                Toast.makeText(this@BookOnActivity,
                    "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}
