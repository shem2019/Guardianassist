package com.example.guardianassist

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.adapters.NfcTagAdapter
import com.example.guardianassist.appctrl.BasicResponse
import com.example.guardianassist.appctrl.NfcTag
import com.example.guardianassist.appctrl.NfcTagResponse
import com.example.guardianassist.appctrl.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NfcTagsActivity : AppCompatActivity() {

    private lateinit var recyclerViewTags: RecyclerView
    private var currentTagToWrite: NfcTag? = null
    private lateinit var adapter: NfcTagAdapter
    private val tagList = mutableListOf<NfcTag>()
    private var siteId: Int = -1

    // ✅ NFC Components
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private var isWaitingForTag = false // ✅ Prevents multiple writes
    private var writingDialog: AlertDialog? = null // ✅ Dialog for waiting to write

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_tags)

        siteId = intent.getIntExtra("site_id", -1)
        val siteName = intent.getStringExtra("site_name") ?: "Unknown"

        val titleTextView = findViewById<TextView>(R.id.tvTitle)
        titleTextView.text = "NFC Tags for $siteName"

        recyclerViewTags = findViewById(R.id.recyclerViewNfcTags)
        recyclerViewTags.layoutManager = LinearLayoutManager(this)
        adapter = NfcTagAdapter(tagList) { tag -> showNfcWritingDialog(tag) }
        recyclerViewTags.adapter = adapter

        findViewById<Button>(R.id.btnAddTag).setOnClickListener {
            showAddTagDialog()
        }

        // ✅ Initialize NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        fetchTags()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null && currentTagToWrite != null && isWaitingForTag) {
                Log.d("NfcTagsActivity", "NFC tag detected: ${tag.id}")
                writeNfcTag(tag, currentTagToWrite!!)
            } else {
                Toast.makeText(this, "No NFC tag detected or writing canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Fetches NFC tags for the selected site */
    private fun fetchTags() {
        if (siteId == -1) {
            Log.e("NfcTagsActivity", "fetchTags: siteId is -1, cannot fetch tags")
            return
        }

        Log.d("NfcTagsActivity", "Fetching NFC tags for siteId: $siteId")

        RetrofitClient.apiService.getNfcTags(siteId).enqueue(object : Callback<NfcTagResponse> {
            override fun onResponse(call: Call<NfcTagResponse>, response: Response<NfcTagResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("NfcTagsActivity", "API Response: $body")

                    if (body?.success == true) {
                        tagList.clear()
                        tagList.addAll(body.tags ?: emptyList())
                        adapter.notifyDataSetChanged()
                        Log.d("NfcTagsActivity", "Loaded ${tagList.size} tags")
                    } else {
                        Log.e("NfcTagsActivity", "Error: ${body?.message}")
                        Toast.makeText(this@NfcTagsActivity, "Error: ${body?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("NfcTagsActivity", "API Error Response: $errorBody")
                    Toast.makeText(this@NfcTagsActivity, "Error fetching NFC tags", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<NfcTagResponse>, t: Throwable) {
                Log.e("NfcTagsActivity", "Network Error: ${t.message}", t)
                Toast.makeText(this@NfcTagsActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Shows dialog for waiting to write NFC */
    private fun showNfcWritingDialog(tag: NfcTag) {
        if (isWaitingForTag) {
            Toast.makeText(this, "Please wait for the current session to finish", Toast.LENGTH_SHORT).show()
            return
        }

        currentTagToWrite = tag
        isWaitingForTag = true

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_waiting_nfc, null)
        val tvTagInfo = dialogView.findViewById<TextView>(R.id.tvTagInfo)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelWrite)

        tvTagInfo.text = "Waiting to write: ${tag.tagName} (${tag.tagType})"

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        writingDialog = dialogBuilder.create()
        writingDialog?.show()

        btnCancel.setOnClickListener {
            cancelNfcWriting()
        }

        Toast.makeText(this, "Tap an NFC tag to write...", Toast.LENGTH_LONG).show()
    }

    /** Cancels NFC writing session */
    private fun cancelNfcWriting() {
        isWaitingForTag = false
        currentTagToWrite = null
        writingDialog?.dismiss()
        Toast.makeText(this, "NFC writing canceled", Toast.LENGTH_SHORT).show()
    }

    /** Writes NFC tag data */
    private fun writeNfcTag(tag: Tag, nfcTag: NfcTag) {
        try {
            val ndef = Ndef.get(tag)
            ndef?.connect()

            val message = NdefMessage(arrayOf(
                NdefRecord.createTextRecord("en", "${nfcTag.tagName},${nfcTag.tagType},${nfcTag.site_id}")
            ))

            ndef?.writeNdefMessage(message)
            ndef?.close()

            Toast.makeText(this, "Tag written successfully!", Toast.LENGTH_SHORT).show()
            finishNfcWriting()

        } catch (e: Exception) {
            Toast.makeText(this, "Error writing NFC tag", Toast.LENGTH_SHORT).show()
        }
    }
    private fun addNewTag(tagName: String, tagType: String) {
        val newTag = NfcTag(
            tag_id = 0, // ID is auto-generated in the database
            tagName = tagName,
            site_id = siteId,
            tagType = tagType,
            latitude = 0.0, // Default coordinates (can be updated later)
            longitude = 0.0,
            isActive = true
        )

        RetrofitClient.apiService.addNfcTag(newTag).enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchTags() // ✅ Refresh NFC tag list
                    Toast.makeText(this@NfcTagsActivity, "NFC Tag added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = response.body()?.message ?: "Unknown error"
                    Toast.makeText(this@NfcTagsActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                Toast.makeText(this@NfcTagsActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddTagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_nfc_tag, null)
        val etTagName = dialogView.findViewById<EditText>(R.id.etTagName)
        val spinnerTagType = dialogView.findViewById<Spinner>(R.id.spinnerTagType)

        // ✅ Set up spinner for selecting tag type
        val tagTypes = arrayOf("Clock In", "Clock Out", "Patrol")
        spinnerTagType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tagTypes)

        // ✅ Build dialog
        AlertDialog.Builder(this)
            .setTitle("Add New NFC Tag")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val tagName = etTagName.text.toString().trim()
                val tagType = spinnerTagType.selectedItem.toString()

                if (tagName.isNotEmpty()) {
                    addNewTag(tagName, tagType)
                } else {
                    Toast.makeText(this, "Tag name is required!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    /** Finishes NFC writing session and closes dialog */
    private fun finishNfcWriting() {
        isWaitingForTag = false
        currentTagToWrite = null
        writingDialog?.dismiss()
    }
}
