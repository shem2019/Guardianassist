package com.example.guardianassist

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.appctrl.PatrolCheckpoint
import com.example.guardianassist.appctrl.PatrolRouteResponse
import com.example.guardianassist.appctrl.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatrolActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var currentCheckpointView: TextView
    private lateinit var nextCheckpointView: TextView
    private lateinit var patrolProgressBar: ProgressBar
    private lateinit var checkpoints: List<PatrolCheckpoint>
    private var currentCheckpointIndex: Int = 0
    private val siteId: Int = 1 // Example: Site ID for this patrol

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patrol)

        currentCheckpointView = findViewById(R.id.currentCheckpointView)
        nextCheckpointView = findViewById(R.id.nextCheckpointView)
        patrolProgressBar = findViewById(R.id.patrolProgressBar)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupNfcIntentFilters()
        fetchPatrolRoute()
    }

    /** ✅ Set up NFC listening **/
    private fun setupNfcIntentFilters() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    /** ✅ Fetch patrol route from backend **/
    private fun fetchPatrolRoute() {
        RetrofitClient.apiService.getPatrolRoute(siteId).enqueue(object : Callback<PatrolRouteResponse> {
            override fun onResponse(call: Call<PatrolRouteResponse>, response: Response<PatrolRouteResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    checkpoints = response.body()?.route ?: emptyList()
                    if (checkpoints.isNotEmpty()) {
                        currentCheckpointIndex = 0
                        updateUI()
                    } else {
                        Toast.makeText(this@PatrolActivity, "No checkpoints found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<PatrolRouteResponse>, t: Throwable) {
                Toast.makeText(this@PatrolActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** ✅ Update UI with the current and next checkpoint **/
    private fun updateUI() {
        if (checkpoints.isNotEmpty()) {
            currentCheckpointView.text = "Current: ${checkpoints[currentCheckpointIndex].name}"
            nextCheckpointView.text = if (currentCheckpointIndex + 1 < checkpoints.size) {
                "Next: ${checkpoints[currentCheckpointIndex + 1].name}"
            } else {
                "Final Checkpoint"
            }
            updateProgressBar()
        }
    }

    /** ✅ Handle NFC scan event **/
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val scannedCheckpoint = getCheckpointFromTag(tag)
            if (scannedCheckpoint == checkpoints[currentCheckpointIndex].name) {
                moveToNextCheckpoint()
            } else {
                Toast.makeText(this, "Incorrect checkpoint. Follow the assigned route.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** ✅ Simulate reading NFC tag (Replace with real NFC reading logic) **/
    private fun getCheckpointFromTag(tag: Tag): String {
        return "Checkpoint ${currentCheckpointIndex + 1}" // Simulated checkpoint name
    }

    /** ✅ Move to next checkpoint **/
    private fun moveToNextCheckpoint() {
        if (currentCheckpointIndex < checkpoints.size - 1) {
            currentCheckpointIndex++
            updateUI()
            Toast.makeText(this, "Checkpoint verified!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Patrol Completed!", Toast.LENGTH_LONG).show()
        }
    }

    /** ✅ Update Progress Bar **/
    private fun updateProgressBar() {
        val progress = ((currentCheckpointIndex.toFloat() / checkpoints.size) * 100).toInt()
        patrolProgressBar.progress = progress
    }
}
