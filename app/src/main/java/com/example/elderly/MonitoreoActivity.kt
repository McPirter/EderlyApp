package com.example.elderly

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elderly.models.Medicamento
import com.example.elderly.network.ApiClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MonitoreoActivity : AppCompatActivity() {

    private val heartRates = mutableListOf<Float>()
    private val temperatures = mutableListOf<Float>()
    private lateinit var adultoId: String
    private lateinit var recordatorioAdapter: RecordatorioAdapter
    private lateinit var lineChart: LineChartView
    private lateinit var barChart: BarChartView
    private val MAX_DATA_POINTS = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoreo)

        adultoId = intent.getStringExtra("ADULTO_ID") ?: run {
            finish()
            return
        }

        lineChart = findViewById(R.id.lineChart)
        barChart = findViewById(R.id.barChart)

        setupRecordatorios()
        setupFloatingActionButton()

        // --- LA MAGIA DEL TIEMPO REAL ---
        // Nos suscribimos a los cambios en el repositorio central.
        WearableDataRepository.nuevosDatos.observe(this) { datos ->
            // Este bloque se ejecuta CADA VEZ que llegan datos nuevos,
            // sin importar qué pantalla esté abierta.

            // Verificamos que los datos sean para el adulto que estamos viendo
            if (datos.adultoId == adultoId) {
                Log.d("MonitoreoActivity", "¡Gráficas actualizadas en tiempo real! HR: ${datos.heartRate}")

                // Actualizamos las listas para las gráficas
                if (heartRates.size >= MAX_DATA_POINTS) heartRates.removeAt(0)
                if (temperatures.size >= MAX_DATA_POINTS) temperatures.removeAt(0)

                heartRates.add(datos.heartRate)
                temperatures.add(datos.temperatura)

                // Actualizamos las gráficas con los nuevos datos
                updateCharts()
            }
        }
    }

    private fun updateCharts() {
        // Gráficas invertidas y correctas
        lineChart.setData(heartRates)    // Línea para ritmo cardíaco
        barChart.setData(temperatures)   // Barras para temperatura
    }

    // El resto de tus funciones (setupRecordatorios, etc.) se quedan exactamente igual.
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
                        val recordatoriosFiltrados = medicamentos.filter {
                            (it.fecha.toLongOrNull() ?: 0) >= System.currentTimeMillis() - 86400000
                        }.sortedBy { it.fecha.toLongOrNull() ?: 0 }
                        runOnUiThread { recordatorioAdapter.updateData(recordatoriosFiltrados) }
                    }
                }
            }
            override fun onFailure(call: Call<List<Medicamento>>, t: Throwable) {
                Log.e("API Failure", "Error: ${t.message}", t)
            }
        })
    }

    private fun setupFloatingActionButton() {
        findViewById<FloatingActionButton>(R.id.fabAddRecordatorio).setOnClickListener {
            // Tu lógica para añadir recordatorios
        }
    }

    // Ya no son necesarios los métodos onResume, onPause, ni onDestroy para el listener.
}