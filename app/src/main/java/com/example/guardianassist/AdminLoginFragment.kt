package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.LoginRequest
import com.example.guardianassist.appctrl.LoginResponse
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.UserDetailsResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminLoginFragment : Fragment() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_login, container, false)

        // Initialize views
        usernameInput = view.findViewById(R.id.Etadminusername)
        passwordInput = view.findViewById(R.id.Etadminpassword)
        loginButton = view.findViewById(R.id.loginButton)

        // Initialize SessionManager
        sessionManager = SessionManager(requireContext())

        // Handle login button click
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Perform login API call
                performLogin(username, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Function to make the API request for admin login
    private fun performLogin(username: String, password: String) {
        val loginRequest = LoginRequest(username, password)

        // Use Retrofit to send the login request
        RetrofitClient.apiService.loginAdmin(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.status == "success") {
                        // Save admin token in SharedPreferences
                        loginResponse.token?.let { sessionManager.saveAdminToken(it) }


                        // Show success message
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_LONG).show()

                        // Navigate to Admin Dashboard
                        val intent = Intent(requireContext(), AdminDash::class.java)
                        startActivity(intent)
                        requireActivity().finish()  // Close login screen

                    } else {
                        // Show error message from the server
                        Toast.makeText(requireContext(), loginResponse.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle the error case
                    Toast.makeText(requireContext(), "Failed to login. Try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Handle network error
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}
