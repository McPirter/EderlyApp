package com.example.elderly.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.example.elderly.R // Asegúrate de que esta importación esté presente
import java.util.*

class ForegroundService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var syncService: SyncService
    private lateinit var gpsService: GPSService // Añadimos el servicio de GPS

    private var heartRate: Float = 0f
    private var temperaturaEstimada: Double = 0.0 // Usaremos la temperatura estimada

    // Datos simulados para la estimación de temperatura
    private val edad = 70
    private val presionSistolica = 135
    private val presionDiastolica = 85

    private val timer = Timer()

    override fun onCreate() {
        super.onCreate()
        syncService = SyncService(this)
        gpsService = GPSService(this) // Inicializamos el servicio de GPS
        startForegroundServiceNotification()
        iniciarSensores()
        iniciarEnvioPeriodico()
    }

    private fun iniciarSensores() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        gpsService.startLocationUpdates() // Iniciamos la escucha del GPS
    }

    private fun iniciarEnvioPeriodico() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Obtenemos una instancia de SharedPreferences dentro del hilo
                val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)

                // Leemos el ID y el nombre guardados
                val adultoId = sharedPref.getString("adultoId", null)
                val nombre = sharedPref.getString("adultoNombre", null)

                // Si no hay datos de login, no enviamos nada
                if (adultoId == null || nombre == null) {
                    Log.w("ForegroundService", "Envío cancelado, no hay datos de login en SharedPreferences.")
                    return
                }

                // Obtenemos la última ubicación conocida
                val location = gpsService.getLastKnownLocation()
                val latitud = location?.latitude ?: 0.0
                val longitud = location?.longitude ?: 0.0

                // Llamamos al servicio con los DATOS REALES
                syncService.enviarDatosAlTelefono(
                    adultoId,
                    heartRate,
                    temperaturaEstimada,
                    latitud,
                    longitud,
                    nombre
                )
            }
        }, 5000, 10_000) // Espera 5 segundos para empezar y luego cada 1 minuto
    }

    private fun startForegroundServiceNotification() {
        val channelId = "monitor_channel"
        val channel = NotificationChannel(
            channelId,
            "Monitoreo en segundo plano",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Monitoreo activo")
            .setContentText("Enviando datos de salud.")
            .setSmallIcon(android.R.drawable.ic_media_play) // Usa un ícono de tu app
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val hr = event.values[0]
            if (hr > 0) {
                heartRate = hr
                // Calculamos la temperatura estimada cada vez que el HR cambia
                temperaturaEstimada = estimarTemperaturaDinamica(hr, edad, presionSistolica, presionDiastolica)
                Log.d("ForegroundService", "HR: $heartRate, Temp Estimada: $temperaturaEstimada")
            }
        }
    }

    // Función para estimar la temperatura (la movemos aquí)
    private fun estimarTemperaturaDinamica(
        heartRate: Float,
        edad: Int,
        sistolica: Int,
        diastolica: Int
    ): Double {
        return 36.8 - (0.005 * (edad - 30)) - (0.002 * (sistolica - 120)) +
                (0.0015 * (diastolica - 80)) + (0.01 * (heartRate - 60))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        sensorManager.unregisterListener(this)
        gpsService.stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}