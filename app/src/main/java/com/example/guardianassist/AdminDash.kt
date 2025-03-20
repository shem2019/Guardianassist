package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.guardianassist.appctrl.ApiService
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.SiteNames
import com.example.guardianassist.appctrl.SiteNamesResponse
import com.example.guardianassist.appctrl.SiteRequest
import com.example.guardianassist.databinding.ActivityAdminDashBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDash : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAdminDashBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adminLevelText: TextView
    private lateinit var siteAccessText: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize SessionManager
        sessionManager = SessionManager(this)
        adminLevelText = findViewById(R.id.adminLevelText)
        siteAccessText = findViewById(R.id.siteAccessText)
        //Setting up viewpager.
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter  // 🔹 Ensure Adapter is Set First

        //attach tabs
        val tabTitles = arrayOf("Clock In", "Clock Out", "Hourly Check", "Patrols", "Uniform Check")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        //



        binding.logoutIcon.setOnClickListener {
        sessionManager.clearSession()

            val intent= Intent(this,LandingPage::class.java)
            startActivity(intent)
        }
        binding.registerUserButton.setOnClickListener {
            val intent= Intent(this,UserManagementActivity::class.java)
            startActivity(intent)
        }
        binding.tags.setOnClickListener {
            val intent =Intent(this, NfcActivity::class.java)
            startActivity(intent)
        }
        binding.regsite.setOnClickListener {
            val intent=Intent(this, MappingActivity::class.java)
            startActivity(intent)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            displayAdminDetails()
        }, 500)




    }
    private fun displayAdminDetails() {
        val adminLevel = sessionManager.fetchAdminLevel()
        val siteIds = sessionManager.fetchSiteAccess()

        adminLevelText.text = "Admin Level: $adminLevel"

        if (siteIds.isNotEmpty()) {
            fetchSiteNames(siteIds)
        } else {
            siteAccessText.text = "Accessible Sites: None"
            Toast.makeText(this, "No accessible sites found.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun fetchSiteNames(siteIds: List<Int>) {
        val request = SiteRequest(siteIds)  // ✅ Use the correct request format

        RetrofitClient.apiService.getSiteNames(request).enqueue(object : Callback<SiteNamesResponse> {
            override fun onResponse(call: Call<SiteNamesResponse>, response: Response<SiteNamesResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val siteList: List<SiteNames> = response.body()?.sites ?: emptyList()

                    // ✅ Correctly extract site names
                    val siteNames = siteList.joinToString(", ") { site -> site.siteName }

                    siteAccessText.text = "Accessible Sites: $siteNames"
                } else {
                    siteAccessText.text = "Error fetching site names"
                }
            }

            override fun onFailure(call: Call<SiteNamesResponse>, t: Throwable) {
                siteAccessText.text = "Error: ${t.message}"
            }
        })
    }

}


    
