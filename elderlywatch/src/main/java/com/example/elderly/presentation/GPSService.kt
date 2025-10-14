package com.example.elderly.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location // Asegúrate de importar esta
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

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // Evitar crear múltiples callbacks si ya existe
        if (locationCallback != null) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val lat = location.latitude
                val lon = location.longitude

                // --- LOG DE CONFIRMACIÓN AÑADIDO ---
                Log.d("GPSService", "📍 Coordenadas RECIBIDAS: Lat=$lat, Lon=$lon")

                // Guardar coordenadas
                val sharedPref: SharedPreferences = context.getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("latitude", lat.toString())
                    putString("longitude", lon.toString())
                    apply()
                }
            }
        }

        Log.d("GPSService", "Iniciando escucha de actualizaciones de ubicación.")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null // Limpiar para evitar fugas de memoria
            Log.d("GPSService", "Deteniendo escucha de actualizaciones.")
        }
    }

    fun getLastKnownLocation(): Location? {
        val sharedPref: SharedPreferences = context.getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
        val lat = sharedPref.getString("latitude", null)?.toDoubleOrNull()
        val lon = sharedPref.getString("longitude", null)?.toDoubleOrNull()

        return if (lat != null && lon != null) {
            Location("gps_prefs").apply {
                latitude = lat
                longitude = lon
            }
        } else {
            null
        }
    }
}