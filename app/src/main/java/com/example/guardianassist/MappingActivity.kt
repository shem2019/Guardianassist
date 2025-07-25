package com.example.guardianassist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.adapters.OrganizationAdapter
import com.example.guardianassist.appctrl.BasicResponse
import com.example.guardianassist.appctrl.Organization
import com.example.guardianassist.appctrl.OrganizationResponse
import com.example.guardianassist.appctrl.RetrofitClient

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MappingActivity : AppCompatActivity() {

    private lateinit var recyclerViewOrganizations: RecyclerView
    private lateinit var adapter: OrganizationAdapter
    private val organizationList = mutableListOf<Organization>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapping)

        recyclerViewOrganizations = findViewById(R.id.recyclerViewOrganizations)
        recyclerViewOrganizations.layoutManager = LinearLayoutManager(this)

        // Handle click event to navigate to SitesActivity
        adapter = OrganizationAdapter(organizationList) { selectedOrg ->
            val intent = Intent(this, SitesActivity::class.java)
            intent.putExtra("org_id", selectedOrg.org_id)
            intent.putExtra("org_name", selectedOrg.org_name)
            startActivity(intent)
        }
        recyclerViewOrganizations.adapter = adapter

        findViewById<Button>(R.id.btnAddOrganization).setOnClickListener {
            showAddOrganizationDialog()
        }

        fetchOrganizations()
    }
    private fun showAddOrganizationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_organization, null)
        val etOrgName = dialogView.findViewById<EditText>(R.id.etOrgName)
        val etOrgEmail = dialogView.findViewById<EditText>(R.id.etOrgEmail)

        AlertDialog.Builder(this)
            .setTitle("Add Organization")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val orgName = etOrgName.text.toString().trim()
                val orgEmail = etOrgEmail.text.toString().trim()

                if (orgName.isNotEmpty() && orgEmail.isNotEmpty()) {
                    addNewOrganization(orgName, orgEmail)
                } else {
                    Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun addNewOrganization(orgName: String, email: String) {
        val newOrg = Organization(0, orgName, email, "Active")

        RetrofitClient.apiService.addOrganization(newOrg).enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                if (response.isSuccessful) {
                    fetchOrganizations() // Refresh organization list
                    Toast.makeText(this@MappingActivity, "Organization added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MappingActivity, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                Toast.makeText(this@MappingActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Fetch Organizations from API */
    private fun fetchOrganizations() {
        RetrofitClient.apiService.getOrganizations().enqueue(object : Callback<OrganizationResponse> {
            override fun onResponse(call: Call<OrganizationResponse>, response: Response<OrganizationResponse>) {
                if (response.isSuccessful) {
                    organizationList.clear()
                    organizationList.addAll(response.body()?.organizations ?: emptyList())
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<OrganizationResponse>, t: Throwable) {
                Toast.makeText(this@MappingActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

