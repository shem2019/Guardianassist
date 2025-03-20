package com.example.guardianassist.adminfragments

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.ApiService
import com.example.guardianassist.appctrl.HourlyCheckResponse
import com.example.guardianassist.appctrl.HourlyCheckRecord
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.SiteRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Hourlycheck : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: HourlyCheckAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hourlycheck, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewHourlyCheck)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sessionManager = SessionManager(requireContext())

        fetchHourlyCheckRecords()

        return view
    }

    private fun fetchHourlyCheckRecords() {
        val siteIds = sessionManager.fetchSiteAccess()
        if (siteIds.isEmpty()) {
            Toast.makeText(requireContext(), "No accessible sites.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = SiteRequest(siteIds)

        RetrofitClient.apiService.getHourlyChecks(request).enqueue(object : Callback<HourlyCheckResponse> {
            override fun onResponse(call: Call<HourlyCheckResponse>, response: Response<HourlyCheckResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val hourlyCheckList = response.body()?.hourlyChecks ?: emptyList()
                    adapter = HourlyCheckAdapter(hourlyCheckList)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Failed to load Hourly Checks", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HourlyCheckResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
