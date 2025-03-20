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
import com.example.guardianassist.appctrl.AdminClockOutResponse
import com.example.guardianassist.appctrl.ApiService
import com.example.guardianassist.appctrl.ClockOutResponse
import com.example.guardianassist.appctrl.ClockOutRecord
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.SiteRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClockOut : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ClockOutAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_clock_out, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewClockOut)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sessionManager = SessionManager(requireContext())

        fetchClockOutRecords()

        return view
    }

    private fun fetchClockOutRecords() {
        val siteIds = sessionManager.fetchSiteAccess()
        if (siteIds.isEmpty()) {
            Toast.makeText(requireContext(), "No accessible sites.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = SiteRequest(siteIds)

        RetrofitClient.apiService.getClockOutRecords(request).enqueue(object : Callback<AdminClockOutResponse> {
            override fun onResponse(call: Call<AdminClockOutResponse>, response: Response<AdminClockOutResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val clockOutList = response.body()?.clockOuts ?: emptyList()
                    adapter = ClockOutAdapter(clockOutList)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Failed to load Clock-Out records", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminClockOutResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
