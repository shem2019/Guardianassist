package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.LoginRequest
import com.example.guardianassist.appctrl.AdminLoginResponse
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.databinding.FragmentAdminLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminLoginFragment : Fragment() {

    private var _binding: FragmentAdminLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        binding.loginButton.setOnClickListener {
            val username = binding.Etadminusername.text.toString().trim()
            val password = binding.Etadminpassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                Log.i("AdminLogin", "Attempting admin login for user: $username")
                performLogin(username, password)
            } else {
                Log.e("AdminLogin", "Login fields are empty.")
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(username: String, password: String) {
        val loginRequest = LoginRequest(username, password)

        RetrofitClient.apiService.loginAdmin(loginRequest).enqueue(object : Callback<AdminLoginResponse> {
            override fun onResponse(call: Call<AdminLoginResponse>, response: Response<AdminLoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.token != null) {
                        sessionManager.saveAdminToken(loginResponse.token)
                        sessionManager.saveAdminPrivileges(loginResponse.orgId, loginResponse.siteId)  // ✅ Save Access Level
                        navigateToAdminDashboard()
                    } else {
                        Toast.makeText(requireContext(), loginResponse?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminLoginResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun navigateToAdminDashboard() {
        Log.i("AdminLogin", "Navigating to AdminDashboardActivity.")

        val intent = Intent(requireContext(), AdminDash::class.java)
        startActivity(intent)

        Log.i("AdminLogin", "Closing admin login activity.")
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
