package TSX.telkom.final_project.main

import TSX.telkom.final_project.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.map, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Reference to the Firebase location data
        val mapDataRef = database.child("test_map")

        // Fetch data once
        mapDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latRaw = snapshot.child("lat").value
                val lonRaw = snapshot.child("lon").value
                Log.d("FirebaseData", "Lat: $latRaw, Lon: $lonRaw")

                val lat = latRaw?.toString()?.toDoubleOrNull()
                val lon = lonRaw?.toString()?.toDoubleOrNull()
                Log.d("FirebaseData", "Parsed Lat: $lat, Parsed Lon: $lon")

                // Check if lat and lon values are valid
                if (lat != null && lon != null) {
                    val location = LatLng(lat, lon)
                    mMap.clear() // Clear previous marker if needed
                    mMap.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("Target Location")
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))
                } else {
                    Log.e("FirebaseData", "Invalid latitude or longitude values")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read map data: ${error.message}")
            }
        })
    }
}


