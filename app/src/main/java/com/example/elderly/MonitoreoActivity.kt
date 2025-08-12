package com.example.elderly

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elderly.models.Medicamento
import com.example.elderly.models.MedicamentoResponse
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import com.example.elderly.network.ApiClient

class MonitoreoActivity : AppCompatActivity() {

    private lateinit var messageClient: MessageClient
    private lateinit var sharedPrefs: SharedPreferences
    private val heartRates = mutableListOf<Float>()
    private val temperatures = mutableListOf<Float>()
    private lateinit var adultoId: String
    private lateinit var nombre: String
    private lateinit var recordatorioAdapter: RecordatorioAdapter
    private lateinit var lineChart: LineChartView
    private lateinit var barChart: BarChartView
    private val MAX_DATA_POINTS = 7



    private val messageListener = object : MessageClient.OnMessageReceivedListener {
        override fun onMessageReceived(event: MessageEvent) {
            if (event.path == "/sync_datos") {
                processWearableData(String(event.data))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoreo)

        adultoId = intent.getStringExtra("ADULTO_ID") ?: run {
            finish()
            return
        }

        lineChart = findViewById(R.id.lineChart)
        barChart = findViewById(R.id.barChart)


        sharedPrefs = getSharedPreferences("datos_recibidos", Context.MODE_PRIVATE)
        setupWearableConnection()
        setupCharts()
        setupRecordatorios()
        setupFloatingActionButton()
    }

    private fun setupWearableConnection() {
        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(messageListener)
        loadHistoricalData()
    }

    private fun loadHistoricalData() {
        val jsonArray = JSONArray(sharedPrefs.getString("datos", "[]"))

        // Filtrar los datos correspondientes al adulto
        val filtrados = mutableListOf<Pair<Float, Float>>() // (heartRate, temperature)
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            if (item.getString("adultoId") == adultoId) {
                val heartRate = item.optString("heartRate", "0").toFloatOrNull()
                val temperature = item.optString("temperatura", "0").toFloatOrNull()
                if (heartRate != null && temperature != null) {
                    filtrados.add(Pair(heartRate, temperature))
                }
            }
        }

        // Tomar solo los últimos MAX_DATA_POINTS elementos
        val ultimos = filtrados.takeLast(MAX_DATA_POINTS)

        heartRates.clear()
        temperatures.clear()
        for ((hr, temp) in ultimos) {
            heartRates.add(hr)
            temperatures.add(temp)
        }

        updateCharts()
    }


    private fun processWearableData(message: String) {
        try {
            val partes = message.split("|")
            if (partes.size >= 8 && partes[0] == "datos") {
                val id = partes[1]
                val temperatura = partes[2].toFloatOrNull()
                val frecuencia = partes[3].toFloatOrNull()
                val latitud = partes[4]
                val longitud = partes[5]
                val timestamp = partes[6]
                val nombre = partes[7]

                if (id == adultoId && temperatura != null && frecuencia != null) {
                    if (heartRates.size >= MAX_DATA_POINTS) heartRates.removeAt(0)
                    if (temperatures.size >= MAX_DATA_POINTS) temperatures.removeAt(0)

                    heartRates.add(frecuencia)
                    temperatures.add(temperatura)

                    saveDataToPreferences(frecuencia, temperatura)

                    runOnUiThread {
                        updateCharts()
                    }
                }

            } else {
                Log.e("❌ WearableData", "Formato no reconocido o incompleto: $message")
            }
        } catch (e: Exception) {
            Log.e("❌ Error procesando datos", e.toString())
        }
    }




    private fun saveDataToPreferences(heartRate: Float, temperature: Float) {
        val jsonArray = JSONArray(sharedPrefs.getString("datos", "[]"))
        JSONObject().apply {
            put("adultoId", adultoId)
            put("heartRate", heartRate)
            put("temperatura", temperature)
            put("timestamp", System.currentTimeMillis())
            put("nombre", nombre)
        }.let { jsonArray.put(it) }
        sharedPrefs.edit().putString("datos", jsonArray.toString()).apply()
    }

    private fun setupCharts() {
        // Implementa tu lógica de gráficos aquí
        updateCharts()
    }

    private fun updateCharts() {
        lineChart.setData(temperatures)  // Línea: temperatura
        barChart.setData(heartRates)     // Barras: ritmo cardíaco o presión
    }


    private fun setupRecordatorios() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvRecordatorios)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            // Configuración adicional del layout manager
        }

        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(this@MonitoreoActivity, R.drawable.divider)!!)
            }
        )  // <-- Paréntesis de cierre añadido

        recordatorioAdapter = RecordatorioAdapter(emptyList())
        recyclerView.adapter = recordatorioAdapter

        loadRecordatoriosFromApi()
    }

    private fun loadRecordatoriosFromApi() {
        ApiClient.instance.getMedicamentosPorAdulto(adultoId).enqueue(object : Callback<List<Medicamento>> {
            override fun onResponse(call: Call<List<Medicamento>>, response: Response<List<Medicamento>>) {
                if (response.isSuccessful) {
                    response.body()?.let { medicamentos ->
                        val recordatoriosFiltrados = medicamentos
                            .filter {
                                val fechaRecordatorio = it.fecha.toLongOrNull() ?: 0
                                val fechaActual = System.currentTimeMillis()
                                fechaRecordatorio >= fechaActual - 86400000 // Últimas 24 horas
                            }
                            .sortedBy { it.fecha.toLongOrNull() ?: 0 } // Orden ascendente

                        runOnUiThread {
                            recordatorioAdapter.updateData(recordatoriosFiltrados)

                            if (recordatoriosFiltrados.isEmpty()) {
                                showToast("No hay recordatorios recientes")
                            }
                        }
                    }
                } else {
                    Log.e("API Error", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                    showToast("Error al cargar recordatorios")
                }
            }

            override fun onFailure(call: Call<List<Medicamento>>, t: Throwable) {
                Log.e("API Failure", "Error de conexión: ${t.message}", t)
                showToast("Error de conexión: ${t.message ?: "Desconocido"}")
            }
        })
    }

    private fun setupFloatingActionButton() {
        findViewById<FloatingActionButton>(R.id.fabAddRecordatorio).setOnClickListener {
            showAddRecordatorioDialog()
        }
    }

    private fun showAddRecordatorioDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_recordatorio, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Nuevo Recordatorio")
            .setView(dialogView)
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .create()

        dialogView.findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            val medicina = dialogView.findViewById<TextInputEditText>(R.id.etMedicamento).text.toString()
            val descripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcion).text.toString()
            val tiempo = dialogView.findViewById<TextInputEditText>(R.id.etTiempo).text.toString()

            when {
                medicina.isEmpty() -> showToast("Ingrese el nombre del medicamento")
                tiempo.isEmpty() -> showToast("Ingrese el tiempo de administración")
                else -> {
                    try {
                        agregarRecordatorio(medicina, descripcion, tiempo.toInt())
                        dialog.dismiss()
                    } catch (e: NumberFormatException) {
                        showToast("El tiempo debe ser un número válido")
                    }
                }
            }
        }

        dialog.show()
    }

    private fun createRequestBodyFromMap(map: Map<String, Any>): RequestBody {
        val json = Gson().toJson(map)
        return json.toRequestBody("application/json".toMediaType())
    }

    private fun agregarRecordatorio(medicina: String, descripcion: String?, tiempo: Int) {
        val recordatorioData = mutableMapOf<String, Any>(
            "adulto" to adultoId,
            "medicina" to medicina,
            "tiempo" to tiempo
        ).apply {
            descripcion?.takeIf { it.isNotEmpty() }?.let { put("descripcion", it) }
        }

        val requestBody = createRequestBodyFromMap(recordatorioData)

        ApiClient.instance.registrarMedicamento(requestBody).enqueue(
            object : Callback<MedicamentoResponse> {
                override fun onResponse(
                    call: Call<MedicamentoResponse>,
                    response: Response<MedicamentoResponse>
                ) {
                    when {
                        response.isSuccessful -> {
                            response.body()?.let { body ->
                                if (body.success) {
                                    showToast(body.message ?: "Recordatorio añadido")
                                    loadRecordatoriosFromApi()
                                } else {
                                    Log.e("API Error", "Success false: ${body.message}")
                                    showToast(body.message ?: "Error en el servidor")
                                }
                            } ?: run {
                                Log.e("API Error", "Response body is null")
                                showToast("Respuesta vacía del servidor")
                            }
                        }
                        else -> {
                            val errorBody = response.errorBody()?.string()
                            Log.e("API Error", """
                                Error ${response.code()}
                                URL: ${call.request().url}
                                Request: ${recordatorioData}
                                Response: $errorBody
                            """.trimIndent())
                            showToast("Error al guardar (código ${response.code()})")
                        }
                    }
                }

                override fun onFailure(call: Call<MedicamentoResponse>, t: Throwable) {
                    Log.e("API Failure", "Error de conexión: ${t.message}", t)
                    showToast("Error de conexión: ${t.message ?: "Desconocido"}")
                }
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            messageClient.removeListener(messageListener)
        } catch (e: Exception) {
            Log.e("Monitoreo", "Error al remover listener", e)
        }
    }

    // Clase Adapter dentro de la Activity
    inner class RecordatorioAdapter(private var recordatorios: List<Medicamento>) :
        RecyclerView.Adapter<RecordatorioAdapter.RecordatorioViewHolder>() {

        inner class RecordatorioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvMedicina: TextView = itemView.findViewById(R.id.tvMedicina)
            val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
            val tvTiempo: TextView = itemView.findViewById(R.id.tvTiempo)
            val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordatorioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recordatorio, parent, false)
            return RecordatorioViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecordatorioViewHolder, position: Int) {
            val recordatorio = recordatorios[position]

            holder.tvMedicina.text = recordatorio.medicina
            holder.tvDescripcion.text = recordatorio.descripcion ?: "Sin descripción"
            holder.tvTiempo.text = "Cada ${recordatorio.tiempo} horas"

            // Formatear fecha
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = Date(recordatorio.fecha.toLong())
                holder.tvFecha.text = sdf.format(date)
            } catch (e: Exception) {
                holder.tvFecha.text = recordatorio.fecha
                Log.e("DateError", "Error al formatear fecha: ${e.message}")
            }
        }

        override fun getItemCount() = recordatorios.size

        fun updateData(newData: List<Medicamento>) {
            recordatorios = newData
            notifyDataSetChanged()
        }
    }
}