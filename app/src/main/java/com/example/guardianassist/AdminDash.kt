package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianassist.adapters.ViewPagerAdapter
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.SiteNames
import com.example.guardianassist.appctrl.SiteNamesResponse
import com.example.guardianassist.appctrl.SiteRequest
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.databinding.ActivityAdminDashBinding
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDash : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // 1) Make our MaterialToolbar act as the ActionBar
        setSupportActionBar(binding.toolbar)

        // 2) Wire up the two top buttons
        binding.btnRegisterSite.setOnClickListener {
            startActivity(Intent(this, MappingActivity::class.java))
        }
        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }

        // 3) FAB → NFC screen
        binding.fabTags.setOnClickListener {
            startActivity(Intent(this, NfcActivity::class.java))
        }

        // 4) Setup ViewPager2 + Tabs
        binding.viewPager.adapter = ViewPagerAdapter(this)
        val tabTitles = arrayOf("Clock In", "Clock Out", "Hourly Check", "Patrols", "Uniform Check")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        // 5) Fetch & display admin info after a slight delay
        Handler(Looper.getMainLooper()).postDelayed({
            displayAdminDetails()
        }, 500)
    }

    /** Inflate our logout menu into the ActionBar */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_dash_menu, menu)
        return true
    }

    /** Handle toolbar item clicks */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                sessionManager.clearSession()
                startActivity(
                    Intent(this, LandingPage::class.java)
                        .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK }
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Read admin level + site IDs, then render text + chips */
    private fun displayAdminDetails() {
        binding.tvAdminLevel.text = "Admin Level: ${sessionManager.fetchAdminLevel()}"
        binding.chipGroupSites.removeAllViews()

        val siteIds = sessionManager.fetchSiteAccess()
        if (siteIds.isEmpty()) {
            // show a disabled "None" chip
            binding.chipGroupSites.addView(
                Chip(this).apply {
                    text = "None"
                    isCheckable = false
                    isEnabled = false
                }
            )
            Toast.makeText(this, "No accessible sites found.", Toast.LENGTH_SHORT).show()
        } else {
            fetchSiteNames(siteIds)
        }
    }

    /** Call backend to resolve site names, then add one Chip per site */
    private fun fetchSiteNames(siteIds: List<Int>) {
        RetrofitClient.apiService
            .getSiteNames(SiteRequest(siteIds))
            .enqueue(object : Callback<SiteNamesResponse> {
                override fun onResponse(
                    call: Call<SiteNamesResponse>,
                    response: Response<SiteNamesResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        binding.chipGroupSites.removeAllViews()
                        response.body()!!.sites.forEach { site ->
                            binding.chipGroupSites.addView(
                                Chip(this@AdminDash).apply {
                                    text = site.siteName
                                    isCheckable = false
                                }
                            )
                        }
                    } else {
                        Toast.makeText(
                            this@AdminDash,
                            "Error fetching site names",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onFailure(call: Call<SiteNamesResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AdminDash,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
