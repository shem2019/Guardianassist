package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SignupRequest
import com.example.guardianassist.appctrl.SignupResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminSignupFragment : Fragment() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var signupButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_signup, container, false)

        // Initialize views
        usernameInput = view.findViewById(R.id.Etadminsignupusername)
        passwordInput = view.findViewById(R.id.Etadminsignuppassword)
        signupButton = view.findViewById(R.id.signupButton)

        // Handle signup button click
        signupButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Perform signup API call
                performSignup(username, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Function to make the API request for admin signup
    private fun performSignup(username: String, password: String) {
        val signupRequest = SignupRequest(username, password)

        // Use Retrofit to send the signup request
        RetrofitClient.apiService.signupAdmin(signupRequest).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val signupResponse = response.body()!!
                    if (signupResponse.status == "success") {
                        // Show success message and token
                        Toast.makeText(requireContext(), "Signup successful!", Toast.LENGTH_LONG).show()
                        navigateToAdminLoginFragment()



                    } else {
                        // Show error message from the server
                        Toast.makeText(requireContext(), signupResponse.message, Toast.LENGTH_SHORT).show()
                        navigateToAdminLoginFragment()
                    }
                } else {
                    // Handle the error case
                    Toast.makeText(requireContext(), "Failed to signup. Try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                // Handle network error
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
    // Function to navigate to AdminLoginFragment
    private fun navigateToAdminLoginFragment() {
        // Create an instance of AdminLoginFragment
        val adminLoginFragment = AdminLoginFragment()

        // Use FragmentManager to replace the current fragment with AdminLoginFragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, adminLoginFragment)  // Replace with the correct container ID
            .addToBackStack(null)  // Optional: Adds the transaction to the backstack for back navigation
            .commit()
    }
}
