package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.SessionManager

class LandingPage : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Check if admin or user token exists
        val adminToken = sessionManager.fetchAdminToken()
        val userToken = sessionManager.fetchUserToken()

        // If admin token exists, navigate to Admin Dashboard
        if (adminToken != null) {
            val intent = Intent(this, AdminDash::class.java)
            startActivity(intent)
            finish()  // Close current activity
            return
        }

        // If user token exists, navigate to User Dashboard
        if (userToken != null) {
           val intent = Intent(this, UserDashboardActivity::class.java)
            startActivity(intent)
            finish()  // Close current activity
            return
        }

        // Declare Admin and User buttons if no token is found
        val adminButton = findViewById<Button>(R.id.btnadmin)
        //val adminsignupButton = findViewById<Button>(R.id.btnsignup)
        val userButton = findViewById<Button>(R.id.btnuser)

        // Load Admin Login fragment when Admin button is clicked
        adminButton.setOnClickListener {
            loadFragment(AdminLoginFragment())
        }
       //// adminsignupButton.setOnClickListener {
          //  loadFragment(AdminSignupFragment())
        //}


        // Load User Login fragment when User button is clicked
        userButton.setOnClickListener {
            loadFragment(UserLoginFragment())
        }
    }

    // Function to load fragments into the fragment container
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null) // Adds to backstack to allow "back" navigation
        transaction.commit()
    }
}
