package com.example.elderly

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elderly.models.Medicamento
import com.example.elderly.network.ApiClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MonitoreoActivity : AppCompatActivity() {

    private lateinit var messageClient: MessageClient
    private lateinit var sharedPrefs: SharedPreferences
    private val heartRates = mutableListOf<Float>()
    private val temperatures = mutableListOf<Float>()
    private lateinit var adultoId: String
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
        val filtrados = mutableListOf<Pair<Float, Float>>()

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

                if (id == adultoId && temperatura != null && frecuencia != null) {
                    if (heartRates.size >= MAX_DATA_POINTS) heartRates.removeAt(0)
                    if (temperatures.size >= MAX_DATA_POINTS) temperatures.removeAt(0)

                    heartRates.add(frecuencia)
                    temperatures.add(temperatura)

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

    private fun setupCharts() {
        updateCharts()
    }

    private fun updateCharts() {
        lineChart.setData(temperatures) // Línea: temperatura
        barChart.setData(heartRates)    // Barras: ritmo cardíaco
    }

    private fun setupRecordatorios() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvRecordatorios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(this@MonitoreoActivity, R.drawable.divider)!!)
            }
        )

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
                                fechaRecordatorio >= fechaActual - 86400000
                            }
                            .sortedBy { it.fecha.toLongOrNull() ?: 0 }

                        runOnUiThread {
                            recordatorioAdapter.updateData(recordatoriosFiltrados)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<Medicamento>>, t: Throwable) {
                Log.e("API Failure", "Error de conexión: ${t.message}", t)
            }
        })
    }

    private fun setupFloatingActionButton() {
        findViewById<FloatingActionButton>(R.id.fabAddRecordatorio).setOnClickListener {
            // Aquí abrirías tu diálogo para agregar recordatorio
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            messageClient.removeListener(messageListener)
        } catch (e: Exception) {
            Log.e("Monitoreo", "Error al remover listener", e)
        }
    }
}