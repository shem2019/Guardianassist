package com.example.guardianassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guardianassist.adapters.UserAdapter
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.User
import com.example.guardianassist.appctrl.UserListResponse
import com.example.guardianassist.appctrl.UserActivationRequest
import com.example.guardianassist.databinding.FragmentUserActivationBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserActivationFragment : Fragment() {

    private var _binding: FragmentUserActivationBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()
    private var orgId: Int = -1

    companion object {
        fun newInstance(orgId: Int): UserActivationFragment {
            val fragment = UserActivationFragment()
            val args = Bundle()
            args.putInt("org_id", orgId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserActivationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orgId = arguments?.getInt("org_id") ?: -1

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Specify explicit types in lambda function
        adapter = UserAdapter(userList, onToggleActivation = { user: User, isActive: Boolean ->
            toggleUserActivation(user, isActive)
        })

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

    private fun toggleUserActivation(user: User, isActive: Boolean) {
        val request = UserActivationRequest(user.userId, isActive)

        RetrofitClient.apiService.updateUserStatus(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    fetchUsers()
                    Toast.makeText(requireContext(), "User status updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to update user status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
