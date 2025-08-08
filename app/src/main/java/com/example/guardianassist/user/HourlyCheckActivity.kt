package com.example.guardianassist.user

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HourlyCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHourlyCheckBinding
    private lateinit var sessionManager: SessionManager

    // NFC Components
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var loadingDialog: AlertDialog
    private var isWaitingForTag = true

    // Timing
    private var nextDueMillis: Long = 0L
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHourlyCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        binding.tvRealName.text = "Welcome, ${sessionManager.fetchRealName()}"

        // 1) Load persisted nextDue or compute from last check/book-on
        val persisted = sessionManager.fetchNextHourlyDueTime()
        nextDueMillis = if (persisted != null) {
            persisted
        } else {
            // derive base: use last check time if exists, else book-on
            val lastCheck = sessionManager.fetchLastHourlyCheckTime()
            val baseTime = lastCheck?.let { dateFmt.parse(it)?.time }
                ?: dateFmt.parse(sessionManager.fetchBookOnTime().orEmpty())?.time
                ?: run {
                    Toast.makeText(this, "No book-on time found.", Toast.LENGTH_LONG).show()
                    finish(); return
                }
            // compute nextDue = base + 1 hour
            val next = baseTime + TimeUnit.HOURS.toMillis(1)
            sessionManager.saveNextHourlyDueTime(next)
            next
        }
        showNextDue()

        disableUI()
        setupNfc()
        setupChecklistListeners()

        binding.btnSubmit.setOnClickListener { submitHourlyCheck() }
    }

    private fun showNextDue() {
        binding.tvNextDue.text = "Next check due at: ${dateFmt.format(Date(nextDueMillis))}"
    }

    private fun disableUI() {
        binding.cbSiteSecure.visibility = View.GONE
        binding.cbEquipmentFunctional.visibility = View.GONE
        binding.cbPersonalSafety.visibility = View.GONE
        binding.btnSubmit.isEnabled = false
    }

    private fun enableUI() {
        binding.cbSiteSecure.visibility = View.VISIBLE
        binding.cbEquipmentFunctional.visibility = View.VISIBLE
        binding.cbPersonalSafety.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = true
    }

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            ?: run { Toast.makeText(this, "NFC unavailable", Toast.LENGTH_LONG).show(); finish(); return }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        loadingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting to Scan NFC Tag")
            .setMessage("Place your device near the 'Hourly Check' tag")
            .setNegativeButton("Quit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
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
        if (!isWaitingForTag || intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) return
        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { handleNfcTag(it) }
    }

    private fun handleNfcTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag) ?: throw Exception("Not NDEF")
            ndef.connect()
            val records = ndef.ndefMessage?.records.orEmpty()
            ndef.close()

            if (records.isEmpty()) throw Exception("Empty tag")
            val raw = String(records[0].payload, Charsets.UTF_8).drop(3)

            val now = System.currentTimeMillis()
            loadingDialog.dismiss()
            isWaitingForTag = false

            if (now < nextDueMillis) {
                Toast.makeText(this,
                    "Too early! Next check is at ${dateFmt.format(Date(nextDueMillis))}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }

            enableUI()
            Toast.makeText(this, "Hourly Check tag OK", Toast.LENGTH_SHORT).show()

            // update last check and compute next
            sessionManager.saveLastHourlyCheckTime(dateFmt.format(Date(now)))
            nextDueMillis = now + TimeUnit.HOURS.toMillis(1)
            sessionManager.saveNextHourlyDueTime(nextDueMillis)
            showNextDue()

        } catch (e: Exception) {
            Log.e("HourlyCheck", "NFC read error", e)
            Toast.makeText(this, "Invalid Hourly Check tag", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChecklistListeners() {
        val boxes = listOf(
            binding.cbPersonalSafety,
            binding.cbSiteSecure,
            binding.cbEquipmentFunctional
        )
        boxes.forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                binding.etComments.visibility =
                    if (boxes.any { !it.isChecked }) View.VISIBLE else View.GONE
            }
        }
    }

    private fun submitHourlyCheck() {
        val token  = sessionManager.fetchUserToken().orEmpty()
        val siteId = sessionManager.fetchSiteId()
        val orgId  = sessionManager.fetchOrgId()
        val real   = sessionManager.fetchRealName().orEmpty()

        if (token.isBlank() || siteId < 0 || orgId < 0) {
            Toast.makeText(this, "Invalid session data", Toast.LENGTH_SHORT).show()
            return
        }

        val req = SaveHourlyCheckRequest(
            token = token,
            site_id = siteId,
            org_id = orgId,
            real_name = real,
            personal_safety = binding.cbPersonalSafety.isChecked,
            site_secure = binding.cbSiteSecure.isChecked,
            equipment_functional = binding.cbEquipmentFunctional.isChecked,
            comments = binding.etComments.text.toString().takeIf { it.isNotBlank() }
        )

        RetrofitClient.apiService.saveHourlyCheck(req)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, resp: Response<Void>) {
                    if (resp.isSuccessful) {
                        Toast.makeText(
                            this@HourlyCheckActivity,
                            "Hourly Check Saved", Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@HourlyCheckActivity,
                            "Save failed", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(
                        this@HourlyCheckActivity,
                        "Network error", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}