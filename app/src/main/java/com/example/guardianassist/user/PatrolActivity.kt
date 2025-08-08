package com.example.guardianassist.user

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
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.adapters.PatrolAdapter
import com.example.guardianassist.adapters.PatrolRecord
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.BasicResponse
import com.example.guardianassist.appctrl.PatrolLogRequest
import com.example.guardianassist.appctrl.PatrolResponse
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset

class PatrolActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var sessionManager: SessionManager
    private lateinit var patrolProgressBar: ProgressBar
    private lateinit var btnEndPatrol: Button
    private lateinit var tvWelcomeMessage: TextView
    private lateinit var patrolRecyclerView: RecyclerView
    private lateinit var patrolTable: TableLayout
    private lateinit var adapter: PatrolAdapter
    private val patrolList = mutableListOf<PatrolRecord>()
    private val patrolCounts = mutableMapOf<String, Int>()
    private var isPatrolling = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patrol)

        sessionManager = SessionManager(this)

        // Startup check: ensure user has booked on
        if (sessionManager.fetchSiteId() <= 0) {
            AlertDialog.Builder(this)
                .setTitle("Booking Required")
                .setMessage("Please Clock In first.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        patrolProgressBar    = findViewById(R.id.patrolProgressBar)
        btnEndPatrol         = findViewById(R.id.btnEndPatrol)
        tvWelcomeMessage     = findViewById(R.id.tvWelcomeMessage)
        patrolRecyclerView   = findViewById(R.id.patrolRecyclerView)
        patrolTable          = findViewById(R.id.patrolTable)

        patrolRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PatrolAdapter(patrolList)
        patrolRecyclerView.adapter = adapter

        loadSessionDetails()

        btnEndPatrol.setOnClickListener {
            isPatrolling = false
            Log.d("PatrolActivity", "Patrol ended by user")
            finish()
        }

        // Initialize NFC only after siteId check
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        fetchPatrolLogs()
    }

    override fun onResume() {
        super.onResume()
        if (::nfcAdapter.isInitialized && isPatrolling) {
            Log.d("PatrolActivity", "Enabling NFC foreground dispatch")
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            Log.d("PatrolActivity", "Disabling NFC foreground dispatch")
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isPatrolling && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) handlePatrolTag(tag)
        }
    }

    private fun handlePatrolTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Toast.makeText(this, "Invalid Patrol Tag", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            val record = message?.records?.firstOrNull()
            val payload = record?.payload?.let { String(it, Charset.forName("UTF-8")).drop(3) }

            if (payload.isNullOrBlank()) {
                Toast.makeText(this, "Invalid Patrol Tag", Toast.LENGTH_SHORT).show()
            } else {
                val tagName = payload.trim()
                patrolCounts[tagName] = (patrolCounts[tagName] ?: 0) + 1
                updatePatrolTable()
                sendPatrolLog(tagName)
            }
        } catch (e: Exception) {
            Log.e("PatrolActivity", "Error reading tag", e)
            Toast.makeText(this, "Failed to read tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPatrolLog(tagName: String) {
        val userId = sessionManager.fetchUserId()
        val siteId = sessionManager.fetchSiteId()
        val orgId  = sessionManager.fetchOrgId()

        if (userId < 0 || siteId < 0 || orgId < 0) {
            Log.e("PatrolActivity", "Missing required fields: userId=$userId, siteId=$siteId, orgId=$orgId")
            Toast.makeText(this, "Error: Missing required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = PatrolLogRequest(userId, siteId, orgId, tagName)
        RetrofitClient.apiService.savePatrolLog(request)
            .enqueue(object : Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@PatrolActivity, "Patrol Log Saved", Toast.LENGTH_SHORT).show()
                        fetchPatrolLogs()
                    } else {
                        Toast.makeText(this@PatrolActivity, "Failed to save patrol log", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    Toast.makeText(this@PatrolActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updatePatrolTable() {
        patrolTable.removeAllViews()
        val header = TableRow(this).apply { setBackgroundColor(Color.LTGRAY) }
        header.addView(createTextView("Area", true))
        header.addView(createTextView("Count", true))
        patrolTable.addView(header)

        patrolCounts.entries.forEachIndexed { i, (area, count) ->
            val row = TableRow(this)
            row.setBackgroundColor(if (i % 2 == 0) Color.WHITE else Color.parseColor("#F0F0F0"))
            row.addView(createTextView(area, false))
            row.addView(createTextView(count.toString(), false))
            patrolTable.addView(row)
        }
    }

    private fun createTextView(text: String, isHeader: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = if (isHeader) 18f else 16f
            setPadding(16, 8, 16, 8)
            setTextColor(if (isHeader) Color.BLACK else Color.DKGRAY)
        }

    private fun loadSessionDetails() {
        val realName = sessionManager.fetchRealName()
        val siteName = sessionManager.fetchSiteName()
        tvWelcomeMessage.text = "Welcome, $realName\nPatrolling: $siteName"
    }

    private fun fetchPatrolLogs() {
        val userId     = sessionManager.fetchUserId()
        val bookOnTime = sessionManager.fetchBookOnTime().orEmpty()
        if (userId < 0 || bookOnTime.isBlank()) return

        RetrofitClient.apiService.getPatrolRecords(userId, bookOnTime)
            .enqueue(object : Callback<PatrolResponse> {
                override fun onResponse(call: Call<PatrolResponse>, response: Response<PatrolResponse>) {
                    patrolList.clear()
                    patrolCounts.clear()
                    response.body()?.patrols?.forEach {
                        patrolList.add(PatrolRecord(it.tagName, it.patrolTime))
                        patrolCounts[it.tagName] = (patrolCounts[it.tagName] ?: 0) + 1
                    }
                    adapter.notifyDataSetChanged()
                    updatePatrolTable()
                }
                override fun onFailure(call: Call<PatrolResponse>, t: Throwable) { /* log */ }
            })
    }
}
