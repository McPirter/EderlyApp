package com.example.elderly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.elderly.models.*
import com.example.elderly.network.ApiClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : BaseActivity() {

    private val cardMap = mutableMapOf<String, View>()
    // Ya no necesitamos MessageClient ni el listener aquí.

    companion object {
        private const val TAG = "DashboardActivity"
    }

    override fun getLayoutId(): Int = R.layout.activity_dashboard
    override fun getNavItemId(): Int = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        loadUserData()

        // --- LA MAGIA OCURRE AQUÍ ---
        // Nos suscribimos a los cambios en el repositorio central de datos.
        WearableDataRepository.nuevosDatos.observe(this) { datos ->
            Log.d(TAG, "DashboardActivity detectó nuevos datos para ${datos.adultoId}")

            // Actualizamos la UI en el hilo principal
            runOnUiThread {
                updateCard(datos.adultoId, datos.heartRate.toString(), datos.temperatura.toString())

                // La lógica para guardar la ubicación se mantiene
                if (datos.lat != 0.0 && datos.lon != 0.0) {
                    saveLocation(datos.adultoId, datos.lat, datos.lon)
                    showToast("📥 Datos recibidos de ${datos.adultoId}")
                }
            }
        }
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.welcomeMessage).text =
            "Bienvenido, ${intent.getStringExtra("nombreUsuario") ?: "Usuario"}"
    }

    private fun saveLocation(adultoId: String, lat: Double, lon: Double) {
        val prefs = getSharedPreferences("ubicaciones", Context.MODE_PRIVATE)
        JSONObject().apply {
            put("adultoId", adultoId)
            put("lat", lat)
            put("lon", lon)
            put("fecha", System.currentTimeMillis())
        }.also { ubicacion ->
            prefs.edit().putString("ultima_ubicacion_$adultoId", ubicacion.toString()).apply()
            Log.d(TAG, "📍 Ubicación guardada: $ubicacion")
        }
    }

    private fun updateCard(adultoId: String, heartRate: String, temperatura: String) {
        cardMap[adultoId]?.let { card ->
            card.findViewById<TextView>(R.id.temperaturaActual).text = "Temp: $temperatura °C"
            card.findViewById<TextView>(R.id.presionActual).text = "Ritmo: $heartRate bpm"
            Log.d(TAG, "📝 Tarjeta actualizada para $adultoId")
        } ?: Log.w(TAG, "⚠️ Tarjeta no encontrada para $adultoId")
    }

    // El resto de tus funciones (loadUserData, createAdultCard, etc.) se quedan exactamente igual.
    private fun loadUserData() {
        val userId = intent.getStringExtra("userId") ?: return
        val adultosContainer = findViewById<LinearLayout>(R.id.adultosContainer)

        ApiClient.instance.getAdultosPorUsuario(userId).enqueue(object : Callback<List<AdultoList>> {
            override fun onResponse(call: Call<List<AdultoList>>, response: Response<List<AdultoList>>) {
                if (response.isSuccessful) {
                    response.body()?.forEach { adulto ->
                        loadAdultData(adulto, adultosContainer)
                    }
                }
            }
            override fun onFailure(call: Call<List<AdultoList>>, t: Throwable) {
                showError("Error al cargar adultos: ${t.message}")
            }
        })
    }

    private fun loadAdultData(adulto: AdultoList, container: LinearLayout) {
        ApiClient.instance.getInfoAdulto(adulto._id).enqueue(object : Callback<DashboardData> {
            override fun onResponse(call: Call<DashboardData>, response: Response<DashboardData>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        createAdultCard(data, adulto._id, container)
                    }
                }
            }
            override fun onFailure(call: Call<DashboardData>, t: Throwable) {
                Log.e(TAG, "Error cargando datos", t)
            }
        })
    }

    private fun createAdultCard(data: DashboardData, adultoId: String, container: LinearLayout) {
        layoutInflater.inflate(R.layout.item_adulto_card, container, false).apply {
            findViewById<TextView>(R.id.nombreAdulto).text = data.adulto.nombre

            data.temperaturas.lastOrNull()?.let {
                findViewById<TextView>(R.id.temperaturaActual).text = "Temp: ${it.temp}°C"
            }
            data.presiones.lastOrNull()?.let {
                findViewById<TextView>(R.id.presionActual).text = "Ritmo: ${it.pres_sistolica} bpm"
            }
            setOnClickListener {
                startActivity(Intent(this@DashboardActivity, MonitoreoActivity::class.java).apply {
                    putExtra("ADULTO_ID", adultoId)
                })
            }
            container.addView(this)
            cardMap[adultoId] = this
        }
    }

    private fun showError(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        Log.e(TAG, message)
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    // Ya no son necesarios los métodos onResume, onPause, onDestroy, ni processWearableData
}