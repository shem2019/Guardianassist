package com.example.guardianassist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.guardianassist.databinding.ActivityUserManagementBinding

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private var orgId: Int = -1
    private lateinit var orgName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get organization details from intent
        orgId = intent.getIntExtra("org_id", -1)
        orgName = intent.getStringExtra("org_name") ?: "Unknown"

        binding.tvTitle.text = "User Management for $orgName"

        // Load default fragment (Register User)
        loadFragment(RegisterUserFragment.newInstance(orgId))

        binding.btnRegisterUser.setOnClickListener {
            loadFragment(RegisterUserFragment.newInstance(orgId))
        }

        binding.btnUpdateUserLogin.setOnClickListener {
            loadFragment(UpdatePasswordFragment.newInstance(orgId))
        }

        binding.btnUserActivation.setOnClickListener {
            loadFragment(UserActivationFragment.newInstance(orgId))
        }
    }

    /** Loads the selected fragment */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.userManagementFrame, fragment)
            .commit()
    }
}
