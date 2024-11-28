package com.example.guardianassist

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.databinding.ActivityNfcBinding

class NfcActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var binding: ActivityNfcBinding
    private var isWaitingForTag = false
    private var isWritingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Prepare a PendingIntent for NFC detection
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create a generic intent filter to capture all NFC tags
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Set up button listeners for Read and Write
        binding.readButton.setOnClickListener {
            startWaitingForTag(isWriteMode = false)
        }

        binding.writeButton.setOnClickListener {
            startWaitingForTag(isWriteMode = true)
        }

        binding.clearButton.setOnClickListener {
            clearNfcTag()
        }
    }

    private fun startWaitingForTag(isWriteMode: Boolean) {
        isWritingMode = isWriteMode
        isWaitingForTag = true

        // Show loading indicator
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.locationTextView.text = if (isWriteMode) "Place NFC tag to write..." else "Place NFC tag to read..."

        // Enable foreground dispatch
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)

        // Timeout after 5 seconds if no tag is detected
        Handler(Looper.getMainLooper()).postDelayed({
            if (isWaitingForTag) {
                stopWaitingForTag()
                Toast.makeText(this, "NFC tag not detected", Toast.LENGTH_SHORT).show()
            }
        }, 10000)
    }

    private fun stopWaitingForTag() {
        isWaitingForTag = false
        binding.loadingProgressBar.visibility = View.GONE
        binding.locationTextView.text = "Location: (Waiting timed out)"
    }

    override fun onResume() {
        super.onResume()
        if (isWaitingForTag) {
            // Enable foreground dispatch only if waiting for a tag
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        // Always disable foreground dispatch in onPause
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("NFC", "NFC tag detected")

        if (isWaitingForTag) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            } else {
                Log.e("NFC", "No tag found in the intent")
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        if (isWritingMode) {
            val location = binding.locationEditText.text.toString()
            if (location.isNotEmpty()) {
                writeLocationToNfcTag(location, tag)
            } else {
                Toast.makeText(this, "Please enter a location name", Toast.LENGTH_SHORT).show()
            }
        } else {
            readLocationFromNfcTag(tag)
        }
        stopWaitingForTag() // Only stop waiting; do not disable foreground dispatch here
    }

    private fun writeLocationToNfcTag(location: String, tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            ndef.connect()
            val mimeRecord = NdefRecord.createTextRecord(null, location)
            val ndefMessage = NdefMessage(arrayOf(mimeRecord))
            ndef.writeNdefMessage(ndefMessage)
            ndef.close()
            Toast.makeText(this, "Location '$location' written to tag", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("NFC", "Failed to write to NFC tag", e)
            Toast.makeText(this, "Failed to write to NFC tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readLocationFromNfcTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            ndef.connect()
            val ndefMessage = ndef.ndefMessage
            ndef.close()

            ndefMessage?.let {
                val record = it.records[0]
                val location = String(record.payload)
                binding.locationTextView.text = "Location: $location"
                Toast.makeText(this, "Read location from NFC tag: $location", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "NFC tag is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NFC", "Failed to read NFC tag", e)
            Toast.makeText(this, "Failed to read NFC tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearNfcTag() {
        startWaitingForTag(isWriteMode = true)
        binding.locationEditText.setText("") // Clear the input field
    }
}
