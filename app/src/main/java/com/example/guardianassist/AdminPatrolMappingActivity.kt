package com.example.guardianassist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class AdminPatrolMappingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val checkpoints = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_patrol_mapping)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapClickListener { latLng ->
            val marker = googleMap.addMarker(MarkerOptions().position(latLng).title("Checkpoint"))
            marker?.let { checkpoints.add(it) }
        }

        googleMap.setOnMapLongClickListener {
            if (checkpoints.size > 1) {
                val route = PolylineOptions().addAll(checkpoints.map { it.position }).width(5f)
                googleMap.addPolyline(route)
                savePatrolRoute()
            }
        }
    }

    private fun savePatrolRoute() {
        val routeData = checkpoints.mapIndexed { index, marker ->
            //RouteCheckpointData(index + 1, marker.position.latitude, marker.position.longitude)
        }
        // Send routeData to the server
        Toast.makeText(this, "Route saved", Toast.LENGTH_SHORT).show()
    }
}
