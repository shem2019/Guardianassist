package com.example.guardianassist.adminfragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.AdminPatrolResponse
import com.example.guardianassist.appctrl.AdminSitePatrol
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.appctrl.AdminSiteRequest
import com.example.guardianassist.appctrl.GroupedPatrols
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Patrol : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: AdminPatrolAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_patrol, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewPatrol)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sessionManager = SessionManager(requireContext())

        fetchPatrolRecords()

        return view
    }

    private fun fetchPatrolRecords() {
        val siteIds = sessionManager.fetchSiteAccess()

        Log.d("PatrolFragment", "Sending Site IDs: $siteIds")

        if (siteIds.isEmpty()) {
            Toast.makeText(requireContext(), "No accessible sites.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AdminSiteRequest(siteIds)

        RetrofitClient.apiService.getPatrols(request).enqueue(object : Callback<AdminPatrolResponse> {
            override fun onResponse(call: Call<AdminPatrolResponse>, response: Response<AdminPatrolResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val patrolList = response.body()?.patrols ?: emptyList()

                    Log.d("PatrolFragment", "Retrieved ${patrolList.size} sites with patrol records")

                    // ✅ Group data
                    val groupedPatrols = groupPatrols(patrolList)

                    // ✅ Set adapter with grouped data
                    adapter = AdminPatrolAdapter(groupedPatrols)
                    recyclerView.adapter = adapter
                } else {
                    Log.e("PatrolFragment", "Error fetching patrols: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to load patrols", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AdminPatrolResponse>, t: Throwable) {
                Log.e("PatrolFragment", "API Call Failed: ${t.message}")
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * ✅ Function to Group Patrol Data
     * 1. Group by Site
     * 2. Group by Guard inside the site
     * 3. Group by Patrol Area (first part of tag_name before the first comma)
     * 4. Count the number of times each patrol area was visited
     */
    private fun groupPatrols(patrolList: List<AdminSitePatrol>): List<GroupedPatrols> {
        val siteMap = mutableMapOf<String, MutableMap<String, MutableMap<String, Int>>>()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -15) // ✅ Get time 15 hours ago
        val cutoffTime = calendar.time

        for (site in patrolList) {
            val siteName = site.siteName

            if (!siteMap.containsKey(siteName)) {
                siteMap[siteName] = mutableMapOf()
            }

            for (patrol in site.patrolRecords) {
                val patrolTime = dateFormat.parse("${patrol.patrolDate} ${patrol.patrolTime}") ?: continue

                // ✅ Filter only last 15 hours
                if (patrolTime.before(cutoffTime)) continue

                val guardName = patrol.realName
                val patrolArea = patrol.tagName.split(",")[0] // ✅ Extract first part before comma

                if (!siteMap[siteName]!!.containsKey(guardName)) {
                    siteMap[siteName]!![guardName] = mutableMapOf()
                }

                val visitCount = siteMap[siteName]!![guardName]!!.getOrDefault(patrolArea, 0)
                siteMap[siteName]!![guardName]!![patrolArea] = visitCount + 1
            }
        }

        return siteMap.map { (siteName, guards) -> GroupedPatrols(siteName, guards) }
    }

}
