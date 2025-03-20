package com.example.guardianassist.adminfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.AdminClockInResponse
import com.example.guardianassist.appctrl.ApiService
import com.example.guardianassist.appctrl.ClockInResponse
import com.example.guardianassist.appctrl.ClockInRecord
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.SiteRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClockIn : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ClockInAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_clock_in, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewClockIn)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sessionManager = SessionManager(requireContext())

        fetchClockInRecords()

        return view
    }

    private fun fetchClockInRecords() {
        val siteIds = sessionManager.fetchSiteAccess()
        if (siteIds.isEmpty()) {
            Toast.makeText(requireContext(), "No accessible sites.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = SiteRequest(siteIds)

        RetrofitClient.apiService.getClockInRecords(request).enqueue(object : Callback<AdminClockInResponse> {
            override fun onResponse(call: Call<AdminClockInResponse>, response: Response<AdminClockInResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val clockInList = response.body()?.clockIns ?: emptyList()
                    adapter = ClockInAdapter(clockInList)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Failed to load Clock-In records", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminClockInResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
