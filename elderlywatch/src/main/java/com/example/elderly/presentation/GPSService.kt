package com.example.elderly.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

class GPSService(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 10_000L
    ).apply {
        setMinUpdateIntervalMillis(5_000L)
    }.build()

    private lateinit var locationCallback: LocationCallback

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val lat = location.latitude
                val lon = location.longitude

                // Guardar coordenadas
                val sharedPref: SharedPreferences = context.getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("latitude", lat.toString())
                    putString("longitude", lon.toString())
                    apply()
                }

                Log.d("GPSService", "Coordenadas: Lat=$lat, Lon=$lon")
            }
        }

        Log.d("GPSService", "Iniciando actualizaciones con nueva API de LocationRequest")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun getLastKnownLocation(): android.location.Location? {
        val sharedPref: SharedPreferences = context.getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
        val lat = sharedPref.getString("latitude", null)?.toDoubleOrNull()
        val lon = sharedPref.getString("longitude", null)?.toDoubleOrNull()

        return if (lat != null && lon != null) {
            android.location.Location("gps").apply {
                latitude = lat
                longitude = lon
            }
        } else {
            null
        }
    }

}
