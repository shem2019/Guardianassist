package com.example.guardianassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.User
import com.example.guardianassist.appctrl.UserListResponse
import com.example.guardianassist.appctrl.UpdatePasswordRequest
import com.example.guardianassist.appctrl.UpdatePasswordResponse
import com.example.guardianassist.databinding.FragmentUpdatePasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdatePasswordFragment : Fragment() {

    private var _binding: FragmentUpdatePasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PasswordUpdateAdapter
    private val userList = mutableListOf<User>()
    private var orgId: Int = -1

    companion object {
        fun newInstance(orgId: Int): UpdatePasswordFragment {
            val fragment = UpdatePasswordFragment()
            val args = Bundle()
            args.putInt("org_id", orgId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUpdatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orgId = arguments?.getInt("org_id") ?: -1

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        adapter = PasswordUpdateAdapter(userList) { user, newPassword ->
            updateUserPassword(user.userId, newPassword) // ✅ Ensures userId is an Int
        }
        binding.recyclerViewUsers.adapter = adapter

        fetchUsers()
    }

    private fun fetchUsers() {
        RetrofitClient.apiService.getUsers(orgId).enqueue(object : Callback<UserListResponse> {
            override fun onResponse(call: Call<UserListResponse>, response: Response<UserListResponse>) {
                if (response.isSuccessful) {
                    userList.clear()
                    userList.addAll(response.body()?.users ?: emptyList())
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserListResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserPassword(userId: Int, newPassword: String) {
        val request = UpdatePasswordRequest(userId, newPassword) // ✅ Fixed parameter names

        RetrofitClient.apiService.updatePassword(request).enqueue(object : Callback<UpdatePasswordResponse> {
            override fun onResponse(call: Call<UpdatePasswordResponse>, response: Response<UpdatePasswordResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to update password", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdatePasswordResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
