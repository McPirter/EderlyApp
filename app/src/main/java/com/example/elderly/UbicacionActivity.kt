package com.example.elderly

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.content.ContextCompat
import com.example.elderly.models.AdultoList
import com.example.elderly.network.ApiClient
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UbicacionActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var listView: ListView
    private lateinit var mapController: IMapController

    private val markerMap = mutableMapOf<String, Marker>()
    private val geoPointMap = mutableMapOf<String, GeoPoint>()
    private val adultInfoList = mutableListOf<Pair<String, String>>() // Pares de (ID, Nombre)

    override fun getLayoutId(): Int = R.layout.activity_ubicacion
    override fun getNavItemId(): Int = R.id.nav_location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMap()
        loadInitialDataFromApi()
        observeRealTimeUpdates()
    }

    private fun setupMap() {
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(applicationContext, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }

        mapView = findViewById<MapView>(R.id.mapView).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            mapController = this.controller
        }

        listView = findViewById<ListView>(R.id.listViewAdultos).apply {
            choiceMode = ListView.CHOICE_MODE_SINGLE
            setOnItemClickListener { _, _, position, _ ->
                if (position < adultInfoList.size) {
                    val adultoId = adultInfoList[position].first
                    selectLocation(adultoId)
                }
            }
        }
    }

    private fun loadInitialDataFromApi() {
        // --- CAMBIO CLAVE 1: OBTENCIÓN ROBUSTA DEL userId ---
        // Primero intenta obtenerlo del Intent, si no, búscalo en SharedPreferences (como fallback)
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = intent.getStringExtra("userId") ?: prefs.getString("userId", null)

        if (userId == null) {
            Log.e("UbicacionActivity", "No se encontró userId. Mostrando ubicación por defecto.")
            showDefaultState()
            return
        }

        ApiClient.instance.getAdultosPorUsuario(userId).enqueue(object : Callback<List<AdultoList>> {
            override fun onResponse(call: Call<List<AdultoList>>, response: Response<List<AdultoList>>) {
                if (response.isSuccessful) {
                    val adultos = response.body() ?: emptyList()
                    if (adultos.isNotEmpty()) {
                        clearData()
                        adultos.forEach { adulto ->
                            adultInfoList.add(Pair(adulto._id, adulto.nombre))
                            loadLastKnownLocationFor(adulto._id, adulto.nombre)
                        }
                        setupListView(adultInfoList.map { it.second })
                        zoomToFitAllMarkers() // Centramos el mapa después de cargar todo
                    } else {
                        showDefaultState()
                    }
                } else {
                    showDefaultState()
                }
            }
            override fun onFailure(call: Call<List<AdultoList>>, t: Throwable) {
                Log.e("UbicacionActivity", "Error al cargar la lista de adultos", t)
                showDefaultState()
            }
        })
    }

    // El resto del código se mantiene muy similar, pero con la lógica de actualización corregida.

    private fun loadLastKnownLocationFor(adultoId: String, nombre: String) {
        val prefs = getSharedPreferences("ubicaciones", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("ultima_ubicacion_$adultoId", null)
        jsonString?.let {
            try {
                val json = JSONObject(it)
                val lat = json.getDouble("lat")
                val lon = json.getDouble("lon")
                if (lat != 0.0 && lon != 0.0) {
                    addOrUpdateMarker(adultoId, nombre, GeoPoint(lat, lon))
                }
            } catch (e: Exception) { /* Ignorar error */ }
        }
    }

    private fun observeRealTimeUpdates() {
        WearableDataRepository.nuevosDatos.observe(this) { datos ->
            if (datos.lat != 0.0 && datos.lon != 0.0) {
                Log.d("UbicacionActivity", "📍¡Ubicación en tiempo real para ${datos.adultoId}!")
                val nombre = adultInfoList.find { it.first == datos.adultoId }?.second ?: "Desconocido"
                addOrUpdateMarker(datos.adultoId, nombre, GeoPoint(datos.lat, datos.lon))
            }
        }
    }

    private fun addOrUpdateMarker(adultoId: String, nombre: String, point: GeoPoint) {
        geoPointMap[adultoId] = point
        if (markerMap.containsKey(adultoId)) {
            markerMap[adultoId]?.position = point
        } else {
            val newMarker = createMarker(point, nombre)
            markerMap[adultoId] = newMarker
            mapView.overlays.add(newMarker)
        }
        mapView.invalidate()
    }

    private fun clearData() {
        mapView.overlays.clear()
        markerMap.clear()
        geoPointMap.clear()
        adultInfoList.clear()
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
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_activated_1, items)
    }

    private fun selectLocation(adultoId: String) {
        val position = adultInfoList.indexOfFirst { it.first == adultoId }
        if (position != -1) {
            listView.setItemChecked(position, true)
        }
        geoPointMap[adultoId]?.let { point ->
            mapController.animateTo(point)
            mapController.setZoom(18.0)
        }
    }

    private fun showDefaultState() {
        val defaultPoint = GeoPoint(20.483450, -103.533339)
        mapController.setCenter(defaultPoint)
        mapController.setZoom(10.0)
        setupListView(listOf("Ubicaciòn Actual"))
    }

    // --- CAMBIO CLAVE 2: NUEVA FUNCIÓN PARA CENTRAR EL MAPA ---
    private fun zoomToFitAllMarkers() {
        if (geoPointMap.isEmpty()) {
            showDefaultState()
            return
        }

        if (geoPointMap.size == 1) {
            // Si solo hay un marcador, lo centramos con un zoom fijo
            mapController.setCenter(geoPointMap.values.first())
            mapController.setZoom(16.0)
        } else {
            // Si hay varios, calculamos el recuadro que los contiene a todos
            val boundingBox = BoundingBox.fromGeoPoints(geoPointMap.values.toList())

            // --- LA CORRECCIÓN CLAVE ESTÁ AQUÍ ---
            // Usamos post() para asegurarnos de que el mapa ya se midió a sí mismo
            mapView.post {
                // Hacemos zoom para que todos los marcadores quepan en la pantalla,
                // con un pequeño margen de 100 píxeles.
                mapView.zoomToBoundingBox(boundingBox, true, 100)
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