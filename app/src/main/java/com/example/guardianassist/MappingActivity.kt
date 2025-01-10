package com.example.guardianassist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.guardianassist.databinding.ActivityMappingBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MappingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMappingBinding
    private lateinit var siteSpinner: Spinner
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val checkpointList = mutableListOf<Checkpoint>() // Holds checkpoint data
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private var lastCheckpointLatLng: LatLng? = null // Keeps track of the last checkpoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMappingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        siteSpinner = binding.spinnerSites
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Google Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Populate site spinner
        fetchSites()

        // Add Checkpoint Button
        binding.btnAddCheckpoint.setOnClickListener {
            addCheckpoint()
        }

        // Save Patrol Route Button
        binding.btnSavePatrolRoute.setOnClickListener {
            savePatrolRoute()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set default map position
        if (checkLocationPermission()) {
            centerMapOnCurrentLocation()
        } else {
            requestLocationPermission()
        }

        // Enable My Location layer if permission granted
        if (checkLocationPermission()) {
            map.isMyLocationEnabled = true
        }

        // Handle map clicks
        map.setOnMapClickListener { latLng ->
            promptCheckpointName(latLng)
        }
    }

    private fun fetchSites() {
        // Mock API call to fetch sites
        val siteNames = listOf("Site A", "Site B", "Site C")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, siteNames)
        siteSpinner.adapter = adapter

        siteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSite = siteNames[position]
                zoomToSite(selectedSite) // Zoom map to selected site
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun zoomToSite(siteName: String) {
        // Mock site location data
        val siteLocations = mapOf(
            "Site A" to LatLng(-1.2921, 36.8219),
            "Site B" to LatLng(-1.3000, 36.8000),
            "Site C" to LatLng(-1.3200, 36.8500)
        )

        val location = siteLocations[siteName]
        if (location != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun centerMapOnCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } else {
                Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch location: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptCheckpointName(latLng: LatLng) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Add Checkpoint")
        dialog.setMessage("Enter checkpoint name:")

        val input = EditText(this)
        dialog.setView(input)

        dialog.setPositiveButton("Add") { _, _ ->
            val checkpointName = input.text.toString().trim()
            if (checkpointName.isNotEmpty()) {
                addCheckpointMarker(latLng, checkpointName)
                checkpointList.add(Checkpoint(name = checkpointName, lat = latLng.latitude, lng = latLng.longitude))
                Toast.makeText(this, "Checkpoint added: $checkpointName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Checkpoint name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    private fun addCheckpointMarker(latLng: LatLng, name: String) {
        // Generate a random color for the marker
        val randomColor = getRandomColor()

        // Add the marker to the map
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(randomColor))
        )

        // Connect this checkpoint to the last checkpoint with a polyline
        if (lastCheckpointLatLng != null) {
            map.addPolyline(
                PolylineOptions()
                    .add(lastCheckpointLatLng, latLng)
                    .width(5f)
                    .color(ContextCompat.getColor(this, R.color.patrol_route))
            )
        }

        // Update the last checkpoint
        lastCheckpointLatLng = latLng
    }

    private fun getRandomColor(): Float {
        // Returns a random hue for the marker
        return kotlin.random.Random.nextFloat() * 360
    }

    private fun addCheckpoint() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                promptCheckpointName(currentLatLng)
            } else {
                Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch location: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePatrolRoute() {
        if (checkpointList.isEmpty()) {
            Toast.makeText(this, "No checkpoints to save", Toast.LENGTH_SHORT).show()
            return
        }

        // Mock API call to save route
        Toast.makeText(this, "Patrol route saved!", Toast.LENGTH_SHORT).show()
    }

    private fun checkLocationPermission(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerMapOnCurrentLocation()
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class Checkpoint(val name: String, val lat: Double, val lng: Double)
