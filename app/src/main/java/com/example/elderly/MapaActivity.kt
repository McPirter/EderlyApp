package com.example.elderly

import android.os.Bundle
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapaActivity : BaseActivity() {

    override fun getLayoutId(): Int = R.layout.activity_mapa

    private lateinit var mapView: MapView
    private var adultoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración esencial de osmdroid
        Configuration.getInstance().userAgentValue = applicationContext.packageName
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK) // Fuente de mapas
        mapView.setMultiTouchControls(true)

        adultoId = intent.getStringExtra("adultoId")
        mostrarUbicacion()
    }

    private fun mostrarUbicacion() {
        val prefs = getSharedPreferences("datos_recibidos", MODE_PRIVATE)
        val json = prefs.getString("datos", "[]")
        val lista = JSONArray(json)

        if (lista.length() == 0) {
            // Si no hay datos, muestra una ubicación de prueba (CDMX)
            val puntoDefault = GeoPoint(19.4326, -99.1332)
            val marker = Marker(mapView)
            marker.position = puntoDefault
            marker.title = "Ubicación de prueba"
            mapView.overlays.add(marker)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(puntoDefault)
            return
        }

        for (i in 0 until lista.length()) {
            val obj = lista.getJSONObject(i)
            if (obj.getString("adultoId") == adultoId) {
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")
                val punto = GeoPoint(lat, lon)

                val marker = Marker(mapView)
                marker.position = punto
                marker.title = "Adulto $adultoId"
                mapView.overlays.add(marker)

                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(punto)
                break
            }
        }
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