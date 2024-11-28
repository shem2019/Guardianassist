package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.guardianassist.appctrl.ApiService
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.databinding.ActivityAdminDashBinding

class AdminDash : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAdminDashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize SessionManager
        sessionManager = SessionManager(this)

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


    }
}

    
