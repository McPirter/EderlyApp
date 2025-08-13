package com.example.elderly

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONException
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class UbicacionActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var listView: ListView
    private val markers = mutableListOf<Marker>()
    private val ubicaciones = mutableListOf<GeoPoint>()
    private val nombresAdultos = mutableListOf<String>()

    override fun getLayoutId(): Int = R.layout.activity_ubicacion
    override fun getNavItemId(): Int = R.id.nav_location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMap()
        loadLocations()
    }

    private fun setupMap() {
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(applicationContext, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }

        // Especificación explícita de tipos para findViewById
        mapView = findViewById<MapView>(R.id.mapView).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }

        // Especificación explícita de tipos para findViewById
        listView = findViewById<ListView>(R.id.listViewAdultos).apply {
            choiceMode = ListView.CHOICE_MODE_SINGLE
            setOnItemClickListener { _, _, position, _ ->
                if (position in ubicaciones.indices) {
                    selectLocation(position)
                }
            }
        }
    }

    private fun loadLocations() {
        try {
            val prefs = getSharedPreferences("datos_recibidos", Context.MODE_PRIVATE)
            val jsonData = prefs.getString("datos", "[]") ?: "[]"

            val jsonArray = JSONArray(jsonData)
            if (jsonArray.length() > 0) {
                processLocations(jsonArray)
            } else {
                val gpsPrefs = getSharedPreferences("gps_prefs", Context.MODE_PRIVATE)
                val lat = gpsPrefs.getString("latitude", null)?.toDoubleOrNull()
                val lon = gpsPrefs.getString("longitude", null)?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    processLocations(JSONArray("[{\"nombre\":\"GPS\",\"lat\":$lat,\"lon\":$lon}]"))
                } else {
                    showDefaultLocation()
                }
            }

            Log.d("Ubicacion", "Datos cargados: $jsonData")

        } catch (e: JSONException) {
            Log.e("Ubicacion", "Error parsing locations", e)
            showDefaultLocation()
        }
    }


    private fun processLocations(jsonArray: JSONArray) {
        clearData()

        for (i in 0 until jsonArray.length()) {
            try {
                val location = jsonArray.getJSONObject(i)
                val nombre = location.optString("nombre", "Cresti")
                val lat = location.optDouble("lat", 20.4764774)
                val lon = location.optDouble("lon", -103.4476136)

                if (lat != 0.0 && lon != 0.0) {
                    val punto = GeoPoint(lat, lon)
                    val name = "Adulto $nombre"

                    ubicaciones.add(punto)
                    nombresAdultos.add(name)
                    markers.add(createMarker(punto, name))
                }
            } catch (e: Exception) {
                Log.e("Ubicacion", "Error procesando ubicación $i", e)
            }
        }

        if (ubicaciones.isNotEmpty()) {
            showLocationsOnMap()
        } else {
            showDefaultLocation()
        }
    }

    private fun clearData() {
        mapView.overlays.clear()
        markers.clear()
        ubicaciones.clear()
        nombresAdultos.clear()
    }

    private fun showLocationsOnMap() {
        mapView.overlays.addAll(markers)
        setupListView(nombresAdultos)
        selectFirstLocation()
    }

    private fun createMarker(point: GeoPoint, title: String): Marker {
        return Marker(mapView).apply {
            position = point
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@UbicacionActivity, R.drawable.ic_location)
        }
    }

    private fun setupListView(items: List<String>) {
        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_activated_1,
            items
        )
    }

    private fun selectFirstLocation() {
        if (ubicaciones.isNotEmpty()) {
            selectLocation(0)
        }
    }

    private fun selectLocation(position: Int) {
        if (position in ubicaciones.indices) {
            listView.setItemChecked(position, true)
            mapView.controller.animateTo(ubicaciones[position])
            mapView.controller.setZoom(15.0)
        }
    }

    private fun showDefaultLocation() {
        val defaultPoint = GeoPoint(19.4326, -99.1332)
        mapView.controller.animateTo(defaultPoint)
        mapView.controller.setZoom(12.0)

        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listOf("No hay ubicaciones disponibles")
        )
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}