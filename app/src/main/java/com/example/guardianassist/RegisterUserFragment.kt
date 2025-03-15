package com.example.guardianassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.RegisterUserRequest
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.LoginResponse
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.databinding.FragmentRegisterUserBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterUserFragment : Fragment() {

    private var _binding: FragmentRegisterUserBinding? = null
    private val binding get() = _binding!!
    private var orgId: Int = -1
    private lateinit var sessionManager: SessionManager

    companion object {
        fun newInstance(orgId: Int): RegisterUserFragment {
            val fragment = RegisterUserFragment()
            val args = Bundle()
            args.putInt("org_id", orgId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orgId = arguments?.getInt("org_id") ?: -1
        sessionManager = SessionManager(requireContext())

        binding.btnRegister.setOnClickListener {
            val realName = binding.etRealName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val isActive = binding.switchActive.isChecked

            if (realName.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                registerUser(realName, username, password, isActive)
            } else {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(realName: String, username: String, password: String, isActive: Boolean) {
        val request = RegisterUserRequest(realName, username, password, orgId, isActive)

        RetrofitClient.apiService.registerUser(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.token ?: ""

                    // Save user token in SessionManager
                    sessionManager.saveUserToken(token)

                    Toast.makeText(requireContext(), "User registered successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to register user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
