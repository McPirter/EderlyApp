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
    private var userId: String? = null // Guardamos el userId

    companion object {
        private const val TAG = "DashboardActivity"
    }

    override fun getLayoutId(): Int = R.layout.activity_dashboard
    override fun getNavItemId(): Int = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guardamos el userId para usarlo después
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = intent.getStringExtra("userId") ?: prefs.getString("userId", null)

        // Guardamos el userId que obtuvimos para futuras sesiones
        if (userId != null) {
            prefs.edit().putString("userId", userId).apply()
        }

        setupUI()
        loadUserData() // Carga inicial de tarjetas
        observarDatosEnTiempoReal() // Observador de datos en vivo

        // --- INICIA EL SERVICIO DE POLLING ---
        iniciarServicioDePolling()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detenemos el servicio de polling para ahorrar batería cuando la app se cierra
        Log.d(TAG, "Deteniendo el ApiPollingService desde onDestroy.")
        stopService(Intent(this, ApiPollingService::class.java))
    }

    private fun iniciarServicioDePolling() {
        if (userId != null) {
            val serviceIntent = Intent(this, ApiPollingService::class.java).apply {
                putExtra(ApiPollingService.EXTRA_USER_ID, userId)
            }
            startService(serviceIntent)
        } else {
            Log.e(TAG, "No se puede iniciar el PollingService: userId es nulo.")
        }
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.welcomeMessage).text =
            "Bienvenido, ${intent.getStringExtra("nombreUsuario") ?: "Usuario"}"
    }

    /**
     * Esta función se suscribe a los datos que llegan en tiempo real
     * (tanto del reloj en vivo como del servicio de polling).
     */
    private fun observarDatosEnTiempoReal() {
        WearableDataRepository.nuevosDatos.observe(this) { datos ->
            Log.d(TAG, "DashboardActivity detectó nuevos datos para ${datos.adultoId}")
            runOnUiThread {
                // Actualiza la tarjeta con los datos más frescos
                updateCard(datos.adultoId, datos.heartRate.toString(), datos.temperatura.toString())

                // Si los datos incluyen GPS, los guarda localmente
                if (datos.lat != 0.0 && datos.lon != 0.0) {
                    saveLocation(datos.adultoId, datos.lat, datos.lon) // Guardado local para el mapa
                }
            }
        }
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
            val hrText = heartRate.toFloatOrNull()?.takeIf { it > 0 }?.toString() ?: "--"
            val tempText = temperatura.toFloatOrNull()?.takeIf { it > 0 }?.toString() ?: "--"

            card.findViewById<TextView>(R.id.temperaturaActual).text = "Temp: $tempText °C"
            card.findViewById<TextView>(R.id.presionActual).text = "Ritmo: $hrText bpm"
            Log.d(TAG, "📝 Tarjeta actualizada para $adultoId")
        } ?: Log.w(TAG, "⚠️ Tarjeta no encontrada para $adultoId")
    }

    /**
     * Esta función ahora solo carga la lista de adultos.
     */
    private fun loadUserData() {
        if (userId == null) {
            Log.e(TAG, "Error: No hay userId para cargar datos.")
            return
        }
        val adultosContainer = findViewById<LinearLayout>(R.id.adultosContainer)

        ApiClient.instance.getAdultosPorUsuario(userId!!).enqueue(object : Callback<List<AdultoList>> {
            override fun onResponse(call: Call<List<AdultoList>>, response: Response<List<AdultoList>>) {
                if (response.isSuccessful) {
                    response.body()?.forEach { adulto ->
                        // Llama a la versión simple que solo crea la tarjeta
                        loadAdultData(adulto, adultosContainer)
                    }
                }
            }
            override fun onFailure(call: Call<List<AdultoList>>, t: Throwable) {
                showError("Error al cargar adultos: ${t.message}")
            }
        })
    }

    /**
     * Esta función ahora es MÁS SIMPLE. Solo jala los datos para la tarjeta INICIAL.
     * El ApiPollingService se encargará de las actualizaciones periódicas.
     */
    private fun loadAdultData(adulto: AdultoList, container: LinearLayout) {
        ApiClient.instance.getInfoAdulto(adulto._id).enqueue(object : Callback<DashboardData> {
            override fun onResponse(call: Call<DashboardData>, response: Response<DashboardData>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        // 1. Creamos la tarjeta (como ya lo hacías)
                        createAdultCard(data, adulto._id, container)

                        // 2. Publicamos los datos históricos INICIALES en el repositorio
                        // para que las otras pantallas los vean al abrirse.
                        val ultimaTemp = data.temperaturas.lastOrNull()?.temp?.toFloat() ?: 0f
                        val ultimoRitmo = data.presiones.lastOrNull()?.pres_sistolica?.toFloat() ?: 0f

                        if (ultimaTemp != 0f || ultimoRitmo != 0f) {
                            val datosHistoricos = DatosSensores(
                                adultoId = adulto._id,
                                heartRate = ultimoRitmo,
                                temperatura = ultimaTemp,
                                lat = 0.0,
                                lon = 0.0
                            )
                            WearableDataRepository.actualizarDatos(datosHistoricos)
                        }
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
}