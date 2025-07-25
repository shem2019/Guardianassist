package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.adapters.SiteAdapter
import com.example.guardianassist.appctrl.BasicResponse
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.Site
import com.example.guardianassist.appctrl.SiteResponse
import com.example.guardianassist.appctrl.SiteStatusUpdate
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SitesActivity : AppCompatActivity() {

    private lateinit var recyclerViewSites: RecyclerView
    private lateinit var adapter: SiteAdapter
    private val siteList = mutableListOf<Site>()
    private var orgId: Int = -1
    private lateinit var orgName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sites)

        // Get organization details from intent
        orgId = intent.getIntExtra("org_id", -1)
        orgName = intent.getStringExtra("org_name") ?: "Unknown"

        // Set title with organization name
        val titleTextView = findViewById<TextView>(R.id.tvTitle)
        titleTextView.text = "Sites for $orgName"

        // RecyclerView setup
        recyclerViewSites = findViewById(R.id.recyclerViewSites)
        recyclerViewSites.layoutManager = LinearLayoutManager(this)

        //
        val usermng=findViewById<Button>(R.id.btnManageUsers)
        usermng.setOnClickListener {
            val intent = Intent(this, UserManagementActivity::class.java)
            intent.putExtra("org_id", orgId)
            intent.putExtra("org_name", orgName)
            startActivity(intent)
        }

        // ✅ Clicking a site navigates to NFC tags
        adapter = SiteAdapter(siteList, { selectedSite, switch ->
            toggleSiteStatus(selectedSite, switch)
        }) { selectedSite ->
            val intent = Intent(this, NfcTagsActivity::class.java)
            intent.putExtra("site_id", selectedSite.site_id)
            intent.putExtra("site_name", selectedSite.site_name)
            startActivity(intent)
        }
        recyclerViewSites.adapter = adapter

        // Floating Action Button for adding a new site
        findViewById<FloatingActionButton>(R.id.fabAddSite).setOnClickListener {
            showAddSiteDialog()
        }

        // Fetch sites
        fetchSites()
    }

    /** Fetch sites for selected organization */
    private fun fetchSites() {
        RetrofitClient.apiService.getSites(orgId).enqueue(object : Callback<SiteResponse> {
            override fun onResponse(call: Call<SiteResponse>, response: Response<SiteResponse>) {
                if (response.isSuccessful) {
                    siteList.clear()
                    siteList.addAll(response.body()?.sites ?: emptyList())
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<SiteResponse>, t: Throwable) {
                Toast.makeText(this@SitesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Show dialog to add a new site */
    private fun showAddSiteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_site, null)
        val etSiteName = dialogView.findViewById<EditText>(R.id.etSiteName)
        val etSiteAddress = dialogView.findViewById<EditText>(R.id.etSiteAddress)
        val etLatitude = dialogView.findViewById<EditText>(R.id.etLatitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.etLongitude)

        AlertDialog.Builder(this)
            .setTitle("Add New Site")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val siteName = etSiteName.text.toString().trim()
                val siteAddress = etSiteAddress.text.toString().trim()
                val latitude = etLatitude.text.toString().trim().toDoubleOrNull() ?: 0.0
                val longitude = etLongitude.text.toString().trim().toDoubleOrNull() ?: 0.0

                if (siteName.isNotEmpty() && siteAddress.isNotEmpty()) {
                    addNewSite(siteName, siteAddress, latitude, longitude)
                } else {
                    Toast.makeText(this, "Site Name and Address are required!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** API Call to Add a New Site */
    private fun addNewSite(name: String, address: String, latitude: Double, longitude: Double) {
        val newSite = Site(0, orgId, name, address, latitude, longitude, "Active")

        RetrofitClient.apiService.addSite(newSite).enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                if (response.isSuccessful) {
                    fetchSites() // ✅ Refresh list after adding site
                    Toast.makeText(this@SitesActivity, "Site added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SitesActivity, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                Toast.makeText(this@SitesActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Toggle site activation status */
    /** Toggle site activation status */
    private fun toggleSiteStatus(site: Site, switch: Switch) {
        switch.isEnabled = false // ✅ Disable toggle for 5 seconds

        val newStatus = if (site.subscription_status == "Active") "Inactive" else "Active"

        val updateRequest = SiteStatusUpdate(site_id = site.site_id, subscription_status = newStatus)

        RetrofitClient.apiService.updateSiteStatus(updateRequest)
            .enqueue(object : Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    if (response.isSuccessful) {
                        // ✅ Update site status in list
                        site.subscription_status = newStatus
                        adapter.notifyDataSetChanged() // Refresh UI immediately

                        Toast.makeText(this@SitesActivity, "Site status updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SitesActivity, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                    }

                    // ✅ Re-enable toggle after 5 seconds
                    switch.postDelayed({ switch.isEnabled = true }, 5000)
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    Toast.makeText(this@SitesActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    switch.postDelayed({ switch.isEnabled = true }, 5000)
                }
            })
    }

}
