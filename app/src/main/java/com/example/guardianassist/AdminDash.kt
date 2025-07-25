package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        setSupportActionBar(binding.toolbar)

        // 2. Inflate the menu
        binding.toolbar.inflateMenu(R.menu.admin_dash_menu)

        // 3. Handle menu-item clicks here
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    // clear session
                    sessionManager.clearSession()
                    // go back to landing page and clear backstack
                    startActivity(
                        Intent(this, LandingPage::class.java)
                            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK }
                    )
                    true
                }
                else -> false
            }
        }


        // 2) Buttons
        binding.btnRegisterSite.setOnClickListener {
            startActivity(Intent(this, MappingActivity::class.java))
        }
        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }
        binding.fabTags.setOnClickListener {
            startActivity(Intent(this, NfcActivity::class.java))
        }

        // 3) ViewPager2 + Tabs
        binding.viewPager.adapter = ViewPagerAdapter(this)
        val tabTitles = arrayOf("Clock In", "Clock Out", "Hourly Check", "Patrols", "Uniform Check")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        // 4) Load Admin info shortly after onCreate
        Handler(Looper.getMainLooper()).postDelayed({
            displayAdminDetails()
        }, 500)
    }

    private fun displayAdminDetails() {
        // Show the admin level
        val adminLevel = sessionManager.fetchAdminLevel()
        binding.tvAdminLevel.text = "Admin Level: $adminLevel"

        // Clear any old chips
        binding.chipGroupSites.removeAllViews()

        // Fetch and show accessible sites
        val siteIds = sessionManager.fetchSiteAccess()
        if (siteIds.isNotEmpty()) {
            fetchSiteNames(siteIds)
        } else {
            // No sites → show a disabled “None” chip
            val noneChip = Chip(this).apply {
                text = "None"
                isCheckable = false
                isEnabled = false
            }
            binding.chipGroupSites.addView(noneChip)
            Toast.makeText(this, "No accessible sites found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchSiteNames(siteIds: List<Int>) {
        val request = SiteRequest(siteIds)
        RetrofitClient.apiService
            .getSiteNames(request)
            .enqueue(object : Callback<SiteNamesResponse> {
                override fun onResponse(
                    call: Call<SiteNamesResponse>,
                    response: Response<SiteNamesResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val sites: List<SiteNames> = response.body()?.sites ?: emptyList()
                        binding.chipGroupSites.removeAllViews()
                        sites.forEach { site ->
                            val chip = Chip(this@AdminDash).apply {
                                text = site.siteName
                                isCheckable = false
                            }
                            binding.chipGroupSites.addView(chip)
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
