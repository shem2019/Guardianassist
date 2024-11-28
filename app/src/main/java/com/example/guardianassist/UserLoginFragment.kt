package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.UserDetailsResponse
import com.example.guardianassist.appctrl.UserLoginRequest
import com.example.guardianassist.appctrl.UserLoginResponse
import com.example.guardianassist.databinding.FragmentUserLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserLoginFragment : Fragment() {

    private var _binding: FragmentUserLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using View Binding
        _binding = FragmentUserLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager(requireContext())
        //Welcome user


        // Login button click listener
        binding.loginButton.setOnClickListener {
            val username = binding.Userusername.text.toString().trim()
            val password = binding.Etuserpassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        val loginRequest = UserLoginRequest(username, password)

        RetrofitClient.apiService.loginUser(loginRequest).enqueue(object : Callback<UserLoginResponse> {
            override fun onResponse(call: Call<UserLoginResponse>, response: Response<UserLoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.status == "success" && loginResponse.token != null) {
                        // Save token in SessionManager and navigate to the User Dashboard
                        sessionManager.saveUserToken(loginResponse.token)

                        // Fetch real name using the token
                        fetchRealName(loginResponse.token)

                        startActivity(Intent(requireContext(), UserDashboardActivity::class.java))
                        activity?.finish()

                    } else {
                        Toast.makeText(requireContext(), loginResponse?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserLoginResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    //fetch data
    private fun fetchRealName(token: String) {
        RetrofitClient.apiService.getUserDetails("Bearer $token").enqueue(object : Callback<UserDetailsResponse> {
            override fun onResponse(call: Call<UserDetailsResponse>, response: Response<UserDetailsResponse>) {
                if (response.isSuccessful) {
                    val userDetails = response.body()
                    if (userDetails?.status == "success" && userDetails.data != null) {
                        // Save real_name in SessionManager
                        sessionManager.saveRealName(userDetails.data.real_name)

                        // Navigate to User Dashboard
                        startActivity(Intent(requireContext(), UserDashboardActivity::class.java))
                        activity?.finish()
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserDetailsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }
}
