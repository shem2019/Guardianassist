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
import kotlin.collections.iterator

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

        patrolProgressBar = findViewById(R.id.patrolProgressBar)
        btnEndPatrol = findViewById(R.id.btnEndPatrol)
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        patrolRecyclerView = findViewById(R.id.patrolRecyclerView)
        patrolTable = findViewById(R.id.patrolTable)

        patrolRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PatrolAdapter(patrolList)
        patrolRecyclerView.adapter = adapter

        loadSessionDetails()

        btnEndPatrol.setOnClickListener {
            isPatrolling = false
            Log.d("PatrolActivity", "Patrol ended by user")
            finish()
        }

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
        if (isPatrolling) {
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
            if (tag != null) {
                handlePatrolTag(tag)
            }
        }
    }

    private fun handlePatrolTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        ndef?.connect()
        val ndefMessage: NdefMessage? = ndef?.ndefMessage
        ndef?.close()

        if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
            val record: NdefRecord = ndefMessage.records[0]
            val payload = String(record.payload, Charset.forName("UTF-8")).substring(3)

            val tagName = payload.trim()
            patrolCounts[tagName] = (patrolCounts[tagName] ?: 0) + 1
            updatePatrolTable()
            sendPatrolLog(tagName)
        } else {
            Toast.makeText(this, "Invalid Patrol Tag", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendPatrolLog(tagName: String) {
        val userId = sessionManager.fetchUserId()
        val siteId = sessionManager.fetchSiteId()
        val orgId = sessionManager.fetchOrgId()

        // ✅ Validate required fields before making API call
        if (userId == -1 || siteId == -1 || orgId == -1) {
            Log.e("PatrolActivity", "❌ sendPatrolLog: Missing required fields - userId=$userId, siteId=$siteId, orgId=$orgId")
            Toast.makeText(this, "Error: Missing required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Log API request parameters
        Log.d("PatrolActivity", "🔍 Sending Patrol Log -> userId=$userId, siteId=$siteId, orgId=$orgId, tagName=$tagName")

        // ✅ Create API request object
        val request = PatrolLogRequest(
            userId = userId,
            siteId = siteId,
            orgId = orgId,
            tagName = tagName
        )

        // ✅ Call API to save patrol log
        RetrofitClient.apiService.savePatrolLog(request)
            .enqueue(object : Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    Log.d("PatrolActivity", "✅ API Response Code: ${response.code()}")

                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("PatrolActivity", "✅ Patrol Log Saved Successfully: ${response.body()?.message}")

                        // ✅ Show success message
                        Toast.makeText(this@PatrolActivity, "Patrol Log Saved", Toast.LENGTH_SHORT).show()

                        // ✅ Update UI: Refresh Patrol Table & RecyclerView
                        fetchPatrolLogs()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("PatrolActivity", "❌ Failed to Save Patrol Log: $errorBody")
                        Toast.makeText(this@PatrolActivity, "Failed to save patrol log", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    Log.e("PatrolActivity", "❌ Network Error saving patrol log: ${t.message}", t)
                    Toast.makeText(this@PatrolActivity, "Network error, check connection", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun updatePatrolTable() {
        Log.d("PatrolActivity", "🔄 Updating Patrol Table. Current patrol counts: $patrolCounts")

        // ✅ Clear previous table content
        patrolTable.removeAllViews()

        // ✅ Add Table Header Row with Labels "Patrol Area" and "Count"
        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(Color.parseColor("#2196F3")) // Blue Header Background
        headerRow.addView(createTextView("Patrol Area", isHeader = true))
        headerRow.addView(createTextView("Count", isHeader = true))
        patrolTable.addView(headerRow)

        // ✅ Populate the table with patrol data
        var rowIndex = 0
        for ((tagName, count) in patrolCounts) {
            val row = TableRow(this)

            // ✅ Extract only the first element from comma-separated values
            val cleanedTagName = tagName.split(",")[0] // Only take the first value before a comma

            // ✅ Alternate Row Colors
            val backgroundColor = if (rowIndex % 2 == 0) Color.parseColor("#F0F0F0") else Color.parseColor("#FFFFFF")
            row.setBackgroundColor(backgroundColor)

            row.addView(createTextView(cleanedTagName, isHeader = false))
            row.addView(createTextView(count.toString(), isHeader = false))

            patrolTable.addView(row)
            rowIndex++
        }

        Log.d("PatrolActivity", "✅ Patrol Table Updated Successfully. Total Areas: ${patrolCounts.size}")
    }

    private fun createStyledTextView(text: String, isHeader: Boolean): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(16, 16, 16, 16)
        textView.textSize = if (isHeader) 18f else 16f
        textView.setTextColor(if (isHeader) Color.WHITE else Color.BLACK)
        textView.setBackgroundColor(if (isHeader) Color.TRANSPARENT else Color.LTGRAY)
        textView.setPadding(16, 8, 16, 8)
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER)

        // ✅ Add a border to each cell
        textView.setBackgroundResource(R.drawable.table_cell_border)

        return textView
    }


    private fun createTextView(text: String, isHeader: Boolean): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        textView.textSize = if (isHeader) 18f else 16f
        textView.setTextColor(if (isHeader) Color.BLACK else Color.DKGRAY)
        return textView
    }

    private fun loadSessionDetails() {
        val realName = sessionManager.fetchRealName()
        val siteName = sessionManager.fetchSiteName()

        if (realName == null || siteName == null) {
            Log.e("PatrolActivity", "❌ Session data missing: RealName=$realName, SiteName=$siteName")
            Toast.makeText(this, "Error: Missing user or site details", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("PatrolActivity", "✅ Session Loaded: RealName=$realName, SiteName=$siteName")
        tvWelcomeMessage.text = "Welcome, $realName! \n You are patrolling: $siteName"
    }

    private fun fetchPatrolLogs() {
        val userId = sessionManager.fetchUserId()
        val bookOnTime = sessionManager.fetchBookOnTime()?.trim() ?: ""

        if (userId == -1 || bookOnTime.isEmpty()) {
            Log.e("PatrolActivity", "❌ fetchPatrolLogs: Missing required fields - userId=$userId, bookOnTime='$bookOnTime'")
            return
        }

        Log.d("PatrolActivity", "🔍 Fetching patrol records for userId=$userId, after bookOnTime=$bookOnTime")

        RetrofitClient.apiService.getPatrolRecords(userId = userId, bookOnTime = bookOnTime)
            .enqueue(object : Callback<PatrolResponse> {
                override fun onResponse(call: Call<PatrolResponse>, response: Response<PatrolResponse>) {
                    Log.d("PatrolActivity", "✅ API Response Code: ${response.code()}")

                    if (response.isSuccessful) {
                        val patrols = response.body()?.patrols ?: emptyList()
                        Log.d("PatrolActivity", "✅ Received ${patrols.size} patrol records")

                        patrolList.clear()
                        patrolCounts.clear()

                        for (patrol in patrols) {
                          //  Log.d("PatrolActivity", "📝 Patrol Record: ${patrol.tagName} - ${patrol.patrolTime}")

                            val patrolRecord = PatrolRecord(patrol.tagName, patrol.patrolTime)
                            patrolList.add(patrolRecord)
                            patrolCounts[patrol.tagName] = (patrolCounts[patrol.tagName] ?: 0) + 1
                        }

                        updatePatrolTable()
                        adapter.notifyDataSetChanged()

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("PatrolActivity", "❌ Error fetching patrol records: $errorBody")
                    }
                }

                override fun onFailure(call: Call<PatrolResponse>, t: Throwable) {
                    Log.e("PatrolActivity", "❌ Network Error fetching patrol records: ${t.message}", t)
                }
            })
    }
}