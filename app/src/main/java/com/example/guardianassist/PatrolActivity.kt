package com.example.guardianassist

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.appctrl.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatrolActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var currentCheckpointView: TextView
    private lateinit var nextCheckpointView: TextView
    private var currentCheckpoint: String? = null
    private var nextCheckpoint: String? = null
    private val siteId: Int = 1 // Example: Site ID for this device

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patrol)

        currentCheckpointView = findViewById(R.id.currentCheckpointView)
        nextCheckpointView = findViewById(R.id.nextCheckpointView)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupNfcIntentFilters()
        fetchPatrolRoute()
    }

    private fun setupNfcIntentFilters() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    private fun fetchPatrolRoute() {
       /* RetrofitClient.apiService.getPatrolRoute(siteId).enqueue(object : Callback<PatrolRouteResponse> {
            override fun onResponse(call: Call<PatrolRouteResponse>, response: Response<PatrolRouteResponse>) {
                if (response.isSuccessful) {
                    val route = response.body()
                    if (route != null) {
                        currentCheckpoint = route.checkpoints.firstOrNull()
                        nextCheckpoint = route.checkpoints.getOrNull(1)
                        updateUI()
                    }
                } else {
                    Toast.makeText(this@PatrolActivity, "Failed to fetch patrol route", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PatrolRouteResponse>, t: Throwable) {
                Toast.makeText(this@PatrolActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })*/
    }

    private fun updateUI() {
        currentCheckpointView.text = "Current: $currentCheckpoint"
        nextCheckpointView.text = "Next: $nextCheckpoint"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val scannedCheckpoint = getCheckpointFromTag(tag)
            if (scannedCheckpoint == currentCheckpoint) {
                moveToNextCheckpoint()
            } else {
                Toast.makeText(this, "Incorrect checkpoint", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCheckpointFromTag(tag: Tag): String {
        // Implement NFC tag reading logic here
        return "Checkpoint 1" // Placeholder
    }

    private fun moveToNextCheckpoint() {
        currentCheckpoint = nextCheckpoint
        // Simulate fetching the next checkpoint
        nextCheckpoint = "Checkpoint 2" // Placeholder logic
        updateUI()
        Toast.makeText(this, "Checkpoint verified", Toast.LENGTH_SHORT).show()
    }
}
