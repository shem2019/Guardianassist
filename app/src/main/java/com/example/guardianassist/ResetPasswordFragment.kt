package com.example.guardianassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.UpdatePasswordRequest
import com.example.guardianassist.appctrl.UserResponse
import com.example.guardianassist.appctrl.UpdatePasswordResponse
import com.example.guardianassist.databinding.FragmentResetPasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private val usersList = mutableListOf<UserResponse>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ListView adapter
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvUsers.adapter = adapter

        // Search button click listener
        binding.btnSearchUser.setOnClickListener {
            val searchTerm = binding.etSearchUser.text.toString().trim()
            if (searchTerm.isNotEmpty()) {
                fetchUsers(searchTerm)
            } else {
                Toast.makeText(requireContext(), "Enter a name to search", Toast.LENGTH_SHORT).show()
            }
        }

        // ListView item click listener to reset password
        binding.lvUsers.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = usersList[position]
            showResetPasswordDialog(selectedUser.user_id)
        }
    }

    // Fetch users based on search term
    private fun fetchUsers(searchTerm: String) {
        RetrofitClient.apiService.fetchUsers(searchTerm).enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                if (response.isSuccessful) {
                    usersList.clear()
                    usersList.addAll(response.body() ?: emptyList())
                    adapter.clear()
                    adapter.addAll(usersList.map { "${it.real_name} (${it.username})" })
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch users", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Show a dialog to reset the password
    private fun showResetPasswordDialog(userId: Int) {
        val inputPassword = EditText(requireContext()).apply {
            hint = "Enter new password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Reset Password")
            .setView(inputPassword)
            .setPositiveButton("Reset") { _, _ ->
                val newPassword = inputPassword.text.toString().trim()
                if (newPassword.isNotEmpty()) {
                    resetPassword(userId, newPassword)
                } else {
                    Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Reset password by calling the API
    private fun resetPassword(userId: Int, newPassword: String) {
        val request = UpdatePasswordRequest(user_id = userId, new_password = newPassword)

        RetrofitClient.apiService.updatePassword(request).enqueue(object : Callback<UpdatePasswordResponse> {
            override fun onResponse(call: Call<UpdatePasswordResponse>, response: Response<UpdatePasswordResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(requireContext(), "Password reset successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to reset password", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdatePasswordResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Clear binding to prevent memory leaks
    }
}
