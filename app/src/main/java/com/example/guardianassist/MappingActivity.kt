package com.example.guardianassist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.appctrl.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MappingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var siteSpinner: Spinner
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerViewCheckpoints: RecyclerView
    private lateinit var adapter: CheckpointAdapter
    private val checkpointList = mutableListOf<Tag>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private var selectedSiteId: Int = -1
    private val sharedPrefs by lazy { getSharedPreferences("app_data", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapping)

        siteSpinner = findViewById(R.id.spinnerSites)
        recyclerViewCheckpoints = findViewById(R.id.recyclerViewCheckpoints)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load offline data
        loadOfflineSites()
        loadOfflineCheckpoints()

        // Initialize RecyclerView
        adapter = CheckpointAdapter(checkpointList) { checkpoint ->
            deleteCheckpoint(checkpoint)
        }
        recyclerViewCheckpoints.layoutManager = LinearLayoutManager(this)
        recyclerViewCheckpoints.adapter = adapter

        setupSwipeToDelete()

        // Initialize Google Maps
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fetchSites()

        // Floating Buttons
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddSite).setOnClickListener {
            showAddSiteDialog()
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddCheckpoint).setOnClickListener {
            addTag()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (checkLocationPermission()) {
            map.isMyLocationEnabled = true
            centerMapOnCurrentLocation()
        } else {
            requestLocationPermission()
        }

        map.setOnMapClickListener { latLng ->
            showTagDialog(latLng)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun centerMapOnCurrentLocation() {
        if (!checkLocationPermission()) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }
    private fun deleteCheckpoint(checkpoint: Tag) {
        checkpointList.remove(checkpoint) // Remove from list
        adapter.notifyDataSetChanged() // Refresh RecyclerView
        saveOfflineCheckpoints() // Save updated list to offline storage
        Toast.makeText(this, "Checkpoint deleted", Toast.LENGTH_SHORT).show()
    }

    private fun addTag() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                showTagDialog(LatLng(location.latitude, location.longitude))
            } else {
                Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showTagDialog(latLng: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupTagType)
        val editTextTagName = dialogView.findViewById<EditText>(R.id.editTextTagName)

        AlertDialog.Builder(this)
            .setTitle("Add Tag")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val tagType = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioStartTag -> "Start"
                    R.id.radioEndTag -> "End"
                    R.id.radioIntermediateTag -> "Intermediate"
                    else -> return@setPositiveButton
                }
                val tagName = editTextTagName.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    saveTagToDatabase(tagName, tagType, latLng)
                } else {
                    Toast.makeText(this, "Tag name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTagToDatabase(name: String, type: String, latLng: LatLng) {
        val newTag = Tag(0, selectedSiteId, name, type, latLng.latitude, latLng.longitude)

        // Add to local list & update UI
        checkpointList.add(newTag)
        adapter.notifyDataSetChanged()

        // Save for offline use
        saveOfflineCheckpoints()

        if (!isInternetAvailable()) {
            Toast.makeText(this, "Tag saved offline. Will sync when online.", Toast.LENGTH_SHORT).show()
            return
        }

        // Send to backend
        RetrofitClient.apiService.addTag(newTag).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MappingActivity, "Tag saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MappingActivity, "Failed to save tag", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MappingActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun saveOfflineCheckpoints() {
        val editor = sharedPrefs.edit()
        val json = Gson().toJson(checkpointList)
        editor.putString("offline_checkpoints", json)
        editor.apply()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                checkpointList.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveOfflineCheckpoints()
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerViewCheckpoints)
    }
    private fun loadOfflineSites() {
        val json = sharedPrefs.getString("offline_sites", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Site>>() {}.type
            val savedSites: List<Site> = Gson().fromJson(json, type)

            if (savedSites.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, savedSites.map { it.name })
                siteSpinner.adapter = adapter
            }
        }
    }
    private fun loadOfflineCheckpoints() {
        val json = sharedPrefs.getString("offline_checkpoints", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Tag>>() {}.type
            val savedCheckpoints: List<Tag> = Gson().fromJson(json, type)

            checkpointList.clear()
            checkpointList.addAll(savedCheckpoints)
            adapter.notifyDataSetChanged()
        }
    }



    private fun showAddSiteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_site, null)
        val etSiteName = dialogView.findViewById<EditText>(R.id.etSiteName)

        AlertDialog.Builder(this)
            .setTitle("Add New Site")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val siteName = etSiteName.text.toString().trim()
                if (siteName.isNotEmpty()) {
                    addNewSite(siteName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewSite(name: String) {
        if (!isInternetAvailable()) {
            val offlineSite = Site(0, name, 0.0, 0.0)
            saveOfflineSites(listOf(offlineSite))
            return
        }

        val newSite = Site(0, name, 0.0, 0.0)

        RetrofitClient.apiService.addSite(newSite).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) fetchSites()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
    private fun saveOfflineSites(sites: List<Site>) {
        val editor = sharedPrefs.edit()
        val json = Gson().toJson(sites)
        editor.putString("offline_sites", json)
        editor.apply()
    }


    private fun fetchSites() {
        if (!isInternetAvailable()) {
            loadOfflineSites()
            return
        }

        RetrofitClient.apiService.fetchSites().enqueue(object : Callback<SiteResponse> {
            override fun onResponse(call: Call<SiteResponse>, response: Response<SiteResponse>) {
                if (response.isSuccessful) {
                    val siteList = response.body()?.sites ?: emptyList()
                    val adapter = ArrayAdapter(this@MappingActivity, android.R.layout.simple_spinner_dropdown_item, siteList.map { it.name })
                    siteSpinner.adapter = adapter
                    saveOfflineSites(siteList)
                }
            }
            override fun onFailure(call: Call<SiteResponse>, t: Throwable) {}
        })
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
