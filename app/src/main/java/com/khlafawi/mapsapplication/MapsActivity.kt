package com.khlafawi.mapsapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.khlafawi.mapsapplication.databinding.ActivityMapsBinding
import com.khlafawi.mapsapplication.utils.LocationProvider
import com.khlafawi.mapsapplication.utils.StepsCounter
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val TAG = MapsActivity::class.java.simpleName

    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationProvider: LocationProvider
    private lateinit var stepsCounter: StepsCounter
    private var isUserTracked = false
    lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationProvider = LocationProvider(this)
        stepsCounter = StepsCounter(this)

        setUpObservers()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("SetTextI18n")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // TODO: add a new marker to the map
        addMarkerOnMap("Marker in Sydney", LatLng(-34.0, 151.0), moveCamera = true)

        // TODO: add a new market when user long click on a location on the map
        setMapLongClick()

        // TODO: add a map overview
        addOverView(R.drawable.android, LatLng(-34.0, 151.0))

        // TODO: Change the map style
        setMapStyle()

        // TODO: enable the user current location
        enableMyLocation()

        // TODO: start tracking the user
        binding.btnStartStop.setOnClickListener {
            if (!isUserTracked) {
                binding.btnStartStop.text = "Stop"
                isUserTracked = true
                startTimer()
                locationProvider.startUserTracking()
                stepsCounter.setupStepsCounter()
            } else {
                binding.btnStartStop.text = "Start"
                isUserTracked = false
                stopTimer()
                locationProvider.stopTracking()
                stepsCounter.unloadStepCounter()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    // TODO: change the map type from the menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_options_menu, menu)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }

        if (requestCode == REQUEST_ACTIVITY_RECOGNITION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                stepsCounter.setupStepsCounter()
            }
        }
    }

    private fun addMarkerOnMap(
        title: String,
        latLng: LatLng,
        moveCamera: Boolean = true,
        zoomLevel: Float = 12.0f
    ) {

        // Add a marker and move the camera
        val options = MarkerOptions()
        options.position(latLng)
        options.title(title)

        mMap.addMarker(options)

        if (moveCamera) {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
            mMap.moveCamera(cameraUpdate)
        }
    }

    private fun setMapLongClick() {
        mMap.setOnMapLongClickListener { latLng ->

            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }
    }

    private fun setMapStyle() {
        try {

            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun addOverView(drawableRes: Int, homeLatLng: LatLng) {
        val overlaySize = 100f
        val androidOverlay = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(drawableRes))
            .position(homeLatLng, overlaySize)

        mMap.addGroundOverlay(androidOverlay)
    }

    private fun setUpObservers() {
        locationProvider.liveLocations.observe(this) { locations ->
            Log.e("locations", locations.toString())
            drawRoute(locations)
        }

        locationProvider.liveLocation.observe(this) { currentLocation ->
            Log.e("currentLocation", currentLocation.toString())
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f))
        }

        locationProvider.liveDistance.observe(this) { distance ->
            Log.e("distance", distance.toString())

            val formattedDistance = getString(R.string.distance_value, distance)
            binding.txtDistance.text = formattedDistance
        }

        stepsCounter.liveSteps.observe(this) { steps ->
            Log.e("steps", steps.toString())

            binding.txtPace.text = steps.toString()
        }
    }

    private fun drawRoute(locations: List<LatLng>) {
        val polylineOptions = PolylineOptions()

        mMap.clear()

        val points = polylineOptions.points
        polylineOptions.color(getColor(R.color.purple_200))
        points.addAll(locations)

        mMap.addPolyline(polylineOptions)
    }

    private fun startTimer() {
        binding.txtTime.base = SystemClock.elapsedRealtime()
        binding.txtTime.start()
    }

    private fun stopTimer() {
        binding.txtTime.stop()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // get the user's current location
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 2
    }
}