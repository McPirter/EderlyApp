package com.example.elderly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import com.example.elderly.models.*
import com.example.elderly.network.ApiClient
import com.google.android.gms.wearable.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : BaseActivity() {

    private val cardMap = mutableMapOf<String, View>()
    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient

    companion object {
        private const val TAG = "DashboardActivity"
        private const val SYNC_DATA_PATH = "/sync_datos"
    }

    // Listener como variable miembro
    private val messageListener = object : MessageClient.OnMessageReceivedListener {
        override fun onMessageReceived(messageEvent: MessageEvent) {
            Log.d(TAG, "📩 onMessageReceived() llamado")
            Log.d(TAG, "🔍 Path recibido: ${messageEvent.path}")

            if (messageEvent.data == null || messageEvent.data.isEmpty()) {
                Log.e(TAG, "❌ El mensaje recibido está vacío o nulo")
                return
            }

            when (messageEvent.path) {
                SYNC_DATA_PATH -> {
                    val rawData = String(messageEvent.data)
                    Log.d(TAG, "✅ Mensaje recibido correctamente")
                    Log.d(TAG, "Contenido RAW (bytes): ${messageEvent.data.contentToString()}")
                    Log.d(TAG, "Contenido del mensaje: $rawData")
                    processWearableData(rawData)
                }
                else -> {
                    Log.w(TAG, "⚠️ Mensaje ignorado en path no reconocido: ${messageEvent.path}")
                }
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_dashboard
    override fun getNavItemId(): Int = R.id.nav_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        initWearableComponents()
        loadUserData()
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.welcomeMessage).text =
            "Bienvenido, ${intent.getStringExtra("nombreUsuario") ?: "Usuario"}"
    }

    private fun initWearableComponents() {
        messageClient = Wearable.getMessageClient(this)
        nodeClient = Wearable.getNodeClient(this)

        // Registrar listener
        messageClient.addListener(messageListener)
        Log.d(TAG, "📡 Listener de mensajes registrado")

        // Verificar conexión inicial
        checkWearableConnection()
    }

    private fun checkWearableConnection() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isNotEmpty()) {
                val nodeNames = nodes.joinToString { it.displayName }
                Log.d(TAG, "🔗 Dispositivos conectados: $nodeNames")
                showToast("Reloj conectado: ${nodes[0].displayName}")

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        "/ping",
                        "Test connection".toByteArray()
                    ).addOnSuccessListener {
                        Log.d(TAG, "✅ Prueba de conexión exitosa con ${node.displayName}")
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error en prueba de conexión con ${node.displayName}", e)
                    }
                }
            } else {
                Log.w(TAG, "🚫 No hay wearables conectados")
                showToast("No se detectó el reloj")
            }
        }.addOnFailureListener {
            Log.e(TAG, "❌ Error al verificar conexión con wearable", it)
        }
    }

    private fun processWearableData(rawData: String) {
        try {
            if (!rawData.startsWith("datos|")) {
                Log.w(TAG, "❗ Formato de mensaje incorrecto. Debe comenzar con 'datos|'")
                return
            }

            val partes = rawData.split("|")
            if (partes.size >= 7) {
                val adultoId = partes[1]
                val heartRate = partes[2]
                val temperatura = partes[3]
                val lat = partes[4].toDoubleOrNull() ?: 0.0
                val lon = partes[5].toDoubleOrNull() ?: 0.0

                Log.d(TAG, """
                    ✅ Datos procesados:
                    - ID: $adultoId
                    - HR: $heartRate
                    - Temp: $temperatura
                    - Lat: $lat
                    - Lon: $lon
                """.trimIndent())

                runOnUiThread {
                    updateCard(adultoId, heartRate, temperatura)
                    if (lat != 0.0 && lon != 0.0) {
                        saveLocation(adultoId, lat, lon)
                        showToast("📥 Datos recibidos de $adultoId")
                    }
                }
            } else {
                Log.w(TAG, "❌ Mensaje incompleto. Partes recibidas: ${partes.size}, esperadas: 7")
                Log.w(TAG, "Contenido recibido: $rawData")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar datos del wearable", e)
            showToast("Error al procesar datos")
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
            card.findViewById<TextView>(R.id.temperaturaActual).text = "Temp: $temperatura °C"
            card.findViewById<TextView>(R.id.presionActual).text = "Ritmo: $heartRate bpm"
            Log.d(TAG, "📝 Tarjeta actualizada para $adultoId")
        } ?: Log.w(TAG, "⚠️ Tarjeta no encontrada para $adultoId")
    }

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
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        Log.e(TAG, message)
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(messageListener)
        Log.d(TAG, "📶 Listener re-registrado en onResume()")
    }

    override fun onPause() {
        super.onPause()
        messageClient.removeListener(messageListener)
        Log.d(TAG, "📴 Listener eliminado en onPause()")
    }

    override fun onDestroy() {
        super.onDestroy()
        messageClient.removeListener(messageListener)
        Log.d(TAG, "🧹 Listener eliminado en onDestroy()")
    }
}
