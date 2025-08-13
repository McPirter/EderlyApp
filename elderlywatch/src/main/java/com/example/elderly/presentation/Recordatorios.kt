package com.example.elderly.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elderly.R
import com.example.elderly.presentation.network.ApiClient
import com.example.elderly.presentation.network.Medicamento
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Recordatorios : ComponentActivity() {

    private lateinit var recordatorioAdapter: RecordatorioAdapter
    private lateinit var rvRecordatorios: RecyclerView
    private val adultoId = "6892f9b224fdadade0948683" // reemplazar con el id real
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 60 * 1000 // cada 1 minuto para revisión
    private val shownNotifications = mutableSetOf<String>() // evitar duplicados

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recordatorio_layout)

        rvRecordatorios = findViewById(R.id.rvRecordatorios)
        rvRecordatorios.layoutManager = LinearLayoutManager(this)

        recordatorioAdapter = RecordatorioAdapter(listOf())
        rvRecordatorios.adapter = recordatorioAdapter

        createNotificationChannel()
        checkNotificationPermission()

        startCheckingMedicamentos()
    }

    private fun startCheckingMedicamentos() {
        handler.post(object : Runnable {
            override fun run() {
                loadRecordatoriosFromApi()
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun loadRecordatoriosFromApi() {
        ApiClient.instance.getMedicamentosPorAdulto(adultoId)
            .enqueue(object : Callback<List<Medicamento>> {
                override fun onResponse(
                    call: Call<List<Medicamento>>,
                    response: Response<List<Medicamento>>
                ) {
                    if (response.isSuccessful) {
                        val medicamentos = response.body() ?: emptyList()
                        val fechaActual = System.currentTimeMillis()

                        val recordatoriosFiltrados = medicamentos
                            .filter {
                                val fechaRecordatorio = it.fecha.toLongOrNull() ?: 0
                                fechaRecordatorio >= fechaActual - 86400000 // últimas 24h
                            }
                            .sortedBy { it.fecha.toLongOrNull() ?: 0 }

                        recordatorioAdapter.updateData(recordatoriosFiltrados)

                        recordatoriosFiltrados.forEach { medicamento ->
                            val fechaRecordatorio = medicamento.fecha.toLongOrNull() ?: 0
                            val tiempoRestante = fechaRecordatorio - fechaActual

                            if (tiempoRestante <= 0 && !shownNotifications.contains(medicamento.id)) {
                                showNotification(medicamento)
                                shownNotifications.add(medicamento.id)
                            }
                        }

                        if (recordatoriosFiltrados.isEmpty()) {
                            Toast.makeText(
                                this@Recordatorios,
                                "No hay recordatorios recientes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e("API Error", "Error: ${response.code()}")
                        Toast.makeText(
                            this@Recordatorios,
                            "Error al cargar recordatorios",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<Medicamento>>, t: Throwable) {
                    Log.e("API Error", t.message ?: "Error desconocido")
                    Toast.makeText(
                        this@Recordatorios,
                        "Error al conectar con el servidor",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showNotification(medicamento: Medicamento) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Notification", "No se puede enviar la notificación: permiso denegado")
            return
        }

        val intent = Intent(this, Recordatorios::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, medicamento.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "med_channel")
            .setSmallIcon(R.drawable.ic_account)
            .setContentTitle("Hora de tomar tu medicamento")
            .setContentText("${medicamento.medicina}: ${medicamento.descripcion ?: ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(medicamento.id.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("Notification", "Error enviando la notificación", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "med_channel",
                "Recordatorios de Medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifica cuando es hora de tomar el medicamento"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
