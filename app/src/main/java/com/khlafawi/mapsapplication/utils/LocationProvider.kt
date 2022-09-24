package com.khlafawi.mapsapplication.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.roundToInt

class LocationProvider(activity: AppCompatActivity) : ViewModel() {

    private val activity by lazy { activity }
    private val client by lazy { LocationServices.getFusedLocationProviderClient(activity) }
    private val locations = mutableListOf<LatLng>()
    private var distance = 0

    val liveLocations = MutableLiveData<List<LatLng>>()
    val liveDistance = MutableLiveData<Int>()
    val liveLocation = MutableLiveData<LatLng>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                val lastLocation = locations.lastOrNull()

                if (lastLocation != null) {
                    distance += SphericalUtil.computeDistanceBetween(lastLocation, latLng)
                        .roundToInt()
                    liveDistance.value = distance
                }

                locations.add(latLng)
                liveLocations.value = locations

                //update the current location also
                liveLocation.value = latLng
            }
        }
    }

    fun startUserTracking() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            client.lastLocation.addOnSuccessListener { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                locations.add(latLng)
                liveLocation.value = latLng
            }


            // track the user
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 5000
            client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopTracking() {
        client.removeLocationUpdates(locationCallback)
        locations.clear()
        distance = 0
    }
}