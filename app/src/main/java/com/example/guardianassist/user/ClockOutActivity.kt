package com.example.guardianassist.user

import android.content.Intent
import android.graphics.Color
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
import com.example.guardianassist.HourlyCheckService
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ClockOutActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    // ───── UI refs ─────
    private lateinit var ivStatus  : ImageView
    private lateinit var tvStatus  : TextView
    private lateinit var tvClockIn : TextView
    private lateinit var tvClockOut: TextView
    private lateinit var tvLive    : TextView
    private lateinit var tvOrg     : TextView
    private lateinit var tvSite    : TextView
    private lateinit var tvResp    : TextView
    private lateinit var btnScan   : MaterialButton
    private lateinit var progress  : CircularProgressIndicator

    // ───── NFC & state ─────
    private lateinit var session    : SessionManager
    private lateinit var nfcAdapter : NfcAdapter

    private var isWaiting = false
    private val uiH = Handler(Looper.getMainLooper())

    /** 1-second ticker for live timer */
    private val ticker = object : Runnable {
        override fun run() {
            updateLive()
            uiH.postDelayed(this, 1000)
        }
    }

    // ─────────────────── lifecycle ───────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock_out)

        session = SessionManager(this)
        bindViews()

        tvOrg.text  = "Organization: ${session.fetchOrgName().orEmpty()}"
        tvSite.text = "Site: ${session.fetchSiteName().orEmpty()}"

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            ?: run { toast("NFC not available"); finish(); return }

        btnScan.setOnClickListener { if (!isWaiting) startWaitingForTag() }

        refreshSession()
    }

    override fun onResume() {
        super.onResume()
        // Enable Reader Mode for all NFC types
        nfcAdapter.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableReaderMode(this)
        stopWaitingForTag()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiH.removeCallbacks(ticker)
    }

    // ─────────── ReaderCallback ───────────
    override fun onTagDiscovered(tag: Tag) {
        uiH.post {
            if (!isWaiting) return@post

            val ndef = Ndef.get(tag)
            if (ndef == null) {
                fail("Not an NDEF tag")
                return@post
            }

            try {
                ndef.connect()
                val records = ndef.ndefMessage?.records
                ndef.close()

                if (records.isNullOrEmpty()) {
                    fail("Empty tag")
                    return@post
                }

                // 1) Raw payload (drop 3-byte lang code), toast it
                val raw = String(records[0].payload, Charset.forName("UTF-8")).drop(3)
                toast("NFC Raw: $raw")

                // 2) Validate format
                val parts = raw.split(",")
                if (parts.size != 3) {
                    fail("Bad format")
                    return@post
                }

                val tagName = parts[0].trim()
                val tagType = parts[1].trim()
                val siteId  = parts[2].trim().toIntOrNull() ?: run {
                    fail("Site ID missing")
                    return@post
                }

                // 3) Existing checks
                when {
                    tagType != "Clock Out" ->
                        fail("Need *Written Clock Out* tag")
                    siteId != session.fetchSiteId() ->
                        fail("Tag site $siteId ≠ session site ${session.fetchSiteId()}")
                    else ->
                        callClockOut(tagName, siteId)
                }

            } catch (e: Exception) {
                fail("Read error: ${e.localizedMessage}")
            }
        }
    }

    // ─────────── 1. Fetch open session ───────────
    private fun refreshSession() {
        val token = session.fetchUserToken() ?: return showNoSession()
        val uid   = session.fetchUserId()
        val sid   = session.fetchSiteId()
        val date  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        RetrofitClient.apiService.getSessionHistory(
            "Bearer $token", uid, sid, date
        ).enqueue(object : Callback<SessionHistoryResponse> {
            override fun onResponse(
                call: Call<SessionHistoryResponse>,
                resp: Response<SessionHistoryResponse>
            ) {
                val open = resp.body()?.sessions?.firstOrNull { it.clock_out_time == null }
                if (resp.isSuccessful && resp.body()?.success == true && open != null) {
                    showOpenSession(open.clock_in_time, open.site_id)
                } else showNoSession()
            }
            override fun onFailure(call: Call<SessionHistoryResponse>, t: Throwable) {
                Log.e("ClockOut", "session fetch failed", t)
                showNoSession()
            }
        })
    }

    // ─────────── 2. UI states ───────────
    private fun showOpenSession(clockIn: String, siteId: Int) {
        session.saveSiteId(siteId)
        session.saveBookOnTime(clockIn)

        tvClockIn.text = "Clock In: $clockIn"
        setStatus(R.drawable.tick, "Ready to Clock Out", Color.BLUE)
        btnScan.isEnabled = true
        tvClockOut.hide()
        tvResp.hide()
        uiH.post(ticker)
    }

    private fun showNoSession() {
        uiH.removeCallbacks(ticker)
        tvClockIn.text = "Clock In: —"
        tvLive.text    = "Time In: 00:00:00"
        setStatus(R.drawable.cancel, "Not Clocked In", Color.RED)
        btnScan.isEnabled = false
        tvClockOut.hide()
        tvResp.hide()
    }

    private fun setStatus(icon: Int, text: String, textColor: Int) {
        ivStatus.setImageResource(icon)
        tvStatus.text = text
        tvStatus.setTextColor(textColor)
    }

    // ─────────── 3. Waiting workflow ───────────
    private fun startWaitingForTag() {
        isWaiting = true
        progress.show()
        setStatus(R.drawable.pending, "Tap a *Written Clock Out* tag", Color.DKGRAY)
        tvResp.hide()

        uiH.postDelayed({
            if (isWaiting) {
                stopWaitingForTag()
                fail("Scan timed-out")
            }
        }, 10_000)
    }

    private fun stopWaitingForTag() {
        isWaiting = false
        progress.hide()
    }

    // ─────────── 4. API call ───────────
    private fun callClockOut(tagName: String, siteId: Int) {
        showLoading()

        val req = BookSessionRequest(
            user_id = session.fetchUserId(),
            org_id  = session.fetchOrgId(),
            site_id = siteId,
            tag     = tagName
        )
        RetrofitClient.apiService.clockOut(
            "Bearer ${session.fetchUserToken()}",
            req
        ).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(c: Call<ApiResponse>, r: Response<ApiResponse>) {
                if (r.isSuccessful && r.body()?.success == true) {
                    success(r.body()!!.message)
                } else fail(r.body()?.message ?: "Server error")
            }
            override fun onFailure(c: Call<ApiResponse>, t: Throwable) {
                fail(t.localizedMessage ?: "Network error")
            }
        })
    }

    private fun showLoading() {
        progress.show()
        setStatus(R.drawable.pending, "Clocking out…", Color.DKGRAY)
        tvResp.hide()
    }

    private fun success(msg: String) {
        stopService(Intent(this, HourlyCheckService::class.java))

        uiH.removeCallbacks(ticker)
        tvClockOut.text = "Clock Out: " +
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        tvClockOut.show()
        setStatus(R.drawable.tick, "Clock-Out Successful", Color.parseColor("#2e7d32"))
        progress.hide()
        showResp(msg, Color.parseColor("#2e7d32"))
        toast(msg)
        uiH.postDelayed({ finish() }, 2500)
    }

    private fun fail(msg: String) {
        setStatus(R.drawable.cancel, "Clock-Out Failed", Color.RED)
        progress.hide()
        showResp(msg, Color.RED)
        toast(msg)
    }

    // ─────────── helpers ───────────
    private fun updateLive() {
        val since = session.fetchBookOnTime() ?: return
        try {
            val start = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(since) ?: return
            val d = System.currentTimeMillis() - start.time
            val h = TimeUnit.MILLISECONDS.toHours(d)
            val m = TimeUnit.MILLISECONDS.toMinutes(d) % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(d) % 60
            tvLive.text = String.format("Time In: %02d:%02d:%02d", h, m, s)
        } catch (_: ParseException) {
            tvLive.text = "Time In: 00:00:00"
        }
    }

    private fun bindViews() {
        ivStatus   = findViewById(R.id.ivClockOutStatus)
        tvStatus   = findViewById(R.id.tvClockOutStatus)
        tvClockIn  = findViewById(R.id.tvClockInTime)
        tvClockOut = findViewById(R.id.tvClockOutTime)
        tvLive     = findViewById(R.id.tvLiveTimer)
        btnScan    = findViewById(R.id.btnScanNfc)
        progress   = findViewById(R.id.progressScanning)
        tvOrg      = findViewById(R.id.tvOrganizationName)
        tvSite     = findViewById(R.id.tvSiteName)
        tvResp     = findViewById(R.id.tvResponse)
    }

    private fun showResp(text: String, color: Int) {
        tvResp.text = text
        tvResp.setTextColor(color)
        tvResp.show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    // view-extensions
    private fun TextView.show() = run { visibility = android.view.View.VISIBLE }
    private fun TextView.hide() = run { visibility = android.view.View.GONE }
    private fun CircularProgressIndicator.show() = run { visibility = android.view.View.VISIBLE }
    private fun CircularProgressIndicator.hide() = run { visibility = android.view.View.GONE }
}
