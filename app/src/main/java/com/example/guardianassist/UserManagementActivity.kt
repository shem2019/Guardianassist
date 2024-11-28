package com.example.guardianassist

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.guardianassist.RegisterUserFragment
import com.example.guardianassist.ResetPasswordFragment

class UserManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        // Load Register User fragment by default
        loadFragment(RegisterUserFragment())

        // Register User Button Click
        val registerUserButton = findViewById<Button>(R.id.btnRegisterUser)
        registerUserButton.setOnClickListener {
            loadFragment(RegisterUserFragment())
        }

        // Reset Password Button Click
        val resetPasswordButton = findViewById<Button>(R.id.btnResetPassword)
        resetPasswordButton.setOnClickListener {
            loadFragment(ResetPasswordFragment())
        }
    }

    // Function to load fragments into the FrameLayout
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.userManagementFrame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
