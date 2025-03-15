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
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SaveHourlyCheckRequest
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.databinding.ActivityHourlyCheckBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HourlyCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHourlyCheckBinding
    private lateinit var sessionManager: SessionManager

    // NFC Components
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var loadingDialog: AlertDialog
    private var isWaitingForTag = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHourlyCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize session manager
        sessionManager = SessionManager(this)
        val realName = sessionManager.fetchRealName()
        binding.tvRealName.text = "Welcome, $realName"

        // Initially disable the UI
        disableUI()

        // NFC Setup
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
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
        showNfcPrompt()

        // Checklist listeners
        setupChecklistListeners()

        // Submit button
        binding.btnSubmit.setOnClickListener {
            submitHourlyCheck()
        }
    }

    private fun disableUI() {
        binding.cbSiteSecure.visibility = View.GONE
        binding.cbEquipmentFunctional.visibility=View.GONE
        binding.cbPersonalSafety.visibility=View.GONE
        binding.btnSubmit.isEnabled = false           // Disable Submit button
    }

    private fun enableUI() {
        binding.cbSiteSecure.visibility = View.VISIBLE
        binding.cbEquipmentFunctional.visibility=View.VISIBLE
        binding.cbPersonalSafety.visibility=View.VISIBLE
        binding.btnSubmit.isEnabled = true               }

    private fun showNfcPrompt() {
        loadingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting to Scan NFC Tag")
            .setMessage("Place your device near a tag marked 'Hourly Check'")
            
            .create()
        loadingDialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (isWaitingForTag) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isWaitingForTag && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            } else {
                Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            ndef?.connect()
            val ndefMessage: NdefMessage? = ndef?.ndefMessage
            ndef?.close()

            if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                val record: NdefRecord = ndefMessage.records[0]
                val payload = String(record.payload, Charsets.UTF_8)
                val cleanedPayload = payload.substring(3) // Skip the language code prefix

                if (cleanedPayload.contains("", ignoreCase = true)) {
                    loadingDialog.dismiss()
                    isWaitingForTag = false
                    enableUI() // Enable checklist and submit button
                    Toast.makeText(this, "Hourly Check tag detected!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Invalid NFC tag. Expected 'Hourly Check'.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "NFC tag is empty.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error reading NFC tag", e)
            Toast.makeText(this, "Error reading NFC tag.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChecklistListeners() {
        val checkboxes = listOf(
            binding.cbPersonalSafety,
            binding.cbSiteSecure,
            binding.cbEquipmentFunctional
        )
        checkboxes.forEach { checkbox ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                binding.etComments.visibility =
                    if (checkboxes.any { !it.isChecked }) View.VISIBLE else View.GONE
            }
        }
    }

    private fun submitHourlyCheck() {
        val token = sessionManager.fetchUserToken()
        val siteId = sessionManager.fetchSiteId()
        val orgId = sessionManager.fetchOrgId()
        val realName = sessionManager.fetchRealName()

        // ✅ Log values before making API request
        Log.d("HourlyCheck", "Submitting Hourly Check:")
        Log.d("HourlyCheck", "Token: $token")
        Log.d("HourlyCheck", "User ID: ${sessionManager.fetchUserId()}")
        Log.d("HourlyCheck", "Org ID: $orgId")
        Log.d("HourlyCheck", "Site ID: $siteId")
        Log.d("HourlyCheck", "Real Name: $realName")

        if (token.isNullOrEmpty() || siteId == -1 || orgId == -1) {
            Log.e("HourlyCheck", "Invalid site or orgname!")
            Toast.makeText(this, "Invalid site or organization details", Toast.LENGTH_SHORT).show()
            return
        }

        val personalSafety = binding.cbPersonalSafety.isChecked
        val siteSecure = binding.cbSiteSecure.isChecked
        val equipmentFunctional = binding.cbEquipmentFunctional.isChecked
        val comments = binding.etComments.text.toString().takeIf { it.isNotBlank() }

        val request = SaveHourlyCheckRequest(
            token = token,
            site_id = siteId,
            org_id = orgId,
            real_name = realName ?: "Unknown User",
            personal_safety = personalSafety,
            site_secure = siteSecure,
            equipment_functional = equipmentFunctional,
            comments = comments
        )

        Log.d("HourlyCheck", "Sending request: $request")

        RetrofitClient.apiService.saveHourlyCheck(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("HourlyCheck", "API Response Code: ${response.code()}")

                if (response.isSuccessful) {
                    Toast.makeText(this@HourlyCheckActivity, "Hourly Check Submitted Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e("HourlyCheck", "Failed: ${response.errorBody()?.string()}")
                    Toast.makeText(this@HourlyCheckActivity, "Failed to Submit Hourly Check", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("HourlyCheck", "Network Error: ${t.message}")
                Toast.makeText(this@HourlyCheckActivity, "Network error, check your connection", Toast.LENGTH_SHORT).show()
            }
        })
    }




}
