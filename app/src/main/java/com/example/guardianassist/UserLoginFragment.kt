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
import com.example.guardianassist.appctrl.SaveLogRequest
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.UserLoginRequest
import com.example.guardianassist.appctrl.UserLoginResponse
import com.example.guardianassist.appctrl.UserDetailsResponse
import com.example.guardianassist.databinding.FragmentUserLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserLoginFragment : Fragment() {

    private var _binding: FragmentUserLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        Log.i("Login", "Checking if user is already logged in.")
        val storedToken = sessionManager.fetchUserToken()
        if (!storedToken.isNullOrEmpty()) {
            Log.i("Login", "User is already logged in. Navigating to dashboard.")
            navigateToDashboard()
            return
        }

        binding.loginButton.setOnClickListener {
            val username = binding.Userusername.text.toString().trim()
            val password = binding.Etuserpassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                Log.i("Login", "Attempting login for user: $username")
                loginUser(username, password)
            } else {
                Log.e("Login", "Login fields are empty.")
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        val loginRequest = UserLoginRequest(username, password)
        Log.i("Login", "Sending login request: $loginRequest")

        RetrofitClient.apiService.loginUser(loginRequest).enqueue(object : Callback<UserLoginResponse> {
            override fun onResponse(call: Call<UserLoginResponse>, response: Response<UserLoginResponse>) {
                Log.i("Login", "Received login response: $response")

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    Log.i("Login", "Login API Response Body: $loginResponse")

                    if (loginResponse?.success == true && loginResponse.token != null) {  // ✅ Check `success`
                        Log.i("Login", "Login successful. Token received: ${loginResponse.token}")

                        sessionManager.saveUserToken(loginResponse.token)
                        saveLog("User Login", loginResponse.token)

                        Log.i("Login", "Fetching user real name after login.")
                        fetchRealName(loginResponse.token)
                    } else {
                        Log.e("Login", "Login failed. Response: $loginResponse")
                        Toast.makeText(requireContext(), loginResponse?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("Login", "Login response unsuccessful. HTTP Code: ${response.code()}")
                    Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
                }
            }


            override fun onFailure(call: Call<UserLoginResponse>, t: Throwable) {
                Log.e("Login", "Login request failed: ${t.message}")
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveLog(eventType: String, token: String) {
        val logRequest = SaveLogRequest(event_type = eventType)
        Log.i("Log", "Saving log for event: $eventType with token: $token")

        RetrofitClient.apiService.saveLog("Bearer $token", logRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.i("Log", "Log saved successfully.")
                } else {
                    Log.e("Log", "Failed to save log. Response: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Log", "Error saving log: ${t.message}")
            }
        })
    }

    private fun fetchRealName(token: String) {
        Log.i("Login", "Fetching user real name with token: $token")

        RetrofitClient.apiService.getUserDetails("Bearer $token").enqueue(object : Callback<UserDetailsResponse> {
            override fun onResponse(call: Call<UserDetailsResponse>, response: Response<UserDetailsResponse>) {
                Log.i("Login", "Received user details response: $response")

                if (response.isSuccessful) {
                    val userDetails = response.body()
                    Log.i("Login", "User details API Response Body: $userDetails")

                    if (userDetails?.success == true && userDetails.data != null) {
                        Log.i("Login", "User real name: ${userDetails.data.realName}")

                        sessionManager.saveRealName(userDetails.data.realName)

                        Log.i("Login", "Navigating to dashboard after fetching user details.")
                        navigateToDashboard()
                    } else {
                        Log.e("Login", "Failed to fetch user details. Response: $userDetails")
                        Toast.makeText(requireContext(), userDetails?.message ?: "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("Login", "Fetching user details failed. HTTP Code: ${response.code()}")
                    Toast.makeText(requireContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserDetailsResponse>, t: Throwable) {
                Log.e("Login", "Error fetching user details: ${t.message}")
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun navigateToDashboard() {
        Log.i("Login", "Navigating to UserDashboardActivity.")

        val intent = Intent(requireContext(), UserDashboardActivity::class.java)
        startActivity(intent)

        Log.i("Login", "Closing login activity.")
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
