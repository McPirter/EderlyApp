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
import java.util.*

class ForegroundService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRate: Float = 0f
    private var temperatura: Double = 0.0
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private lateinit var syncService: SyncService

    private val timer = Timer()

    // Límites
    private val TEMP_MAX = 36.0         // Temperatura en °C
    private val HR_MAX = 70f            // Frecuencia cardiaca en bpm
    private val TIEMPO_ENTRE_ALERTAS = 5_000L // 5 segundos

    // Última vez que se notificó
    private var ultimaNotificacionTemp: Long = 0
    private var ultimaNotificacionHR: Long = 0

    override fun onCreate() {
        super.onCreate()
        syncService = SyncService(this)
        startForegroundServiceNotification()
        iniciarSensores()
        iniciarEnvioPeriodico()
    }

    private fun iniciarSensores() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Ritmo cardiaco
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Temperatura (si el reloj tiene este sensor)
        val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun iniciarEnvioPeriodico() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                syncService.enviarDatosAlTelefono(
                    "adultoId",
                    heartRate,
                    temperatura,
                    latitud,
                    longitud,
                    "nombre"
                )
            }
        }, 0, 10_000) // cada 10 segundos
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
            .setContentText("Enviando datos del usuario")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                heartRate = event.values[0]
                Log.d("SensorService", "HR: $heartRate")

                if (heartRate > HR_MAX &&
                    System.currentTimeMillis() - ultimaNotificacionHR > TIEMPO_ENTRE_ALERTAS
                ) {
                    mostrarNotificacionAlerta(
                        "Frecuencia cardiaca alta",
                        "Se detectó un ritmo cardiaco de $heartRate bpm"
                    )
                    ultimaNotificacionHR = System.currentTimeMillis()
                }
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                temperatura = event.values[0].toDouble()
                Log.d("SensorService", "Temp: $temperatura")

                if (temperatura > TEMP_MAX &&
                    System.currentTimeMillis() - ultimaNotificacionTemp > TIEMPO_ENTRE_ALERTAS
                ) {
                    mostrarNotificacionAlerta(
                        "Temperatura elevada",
                        "La temperatura es de $temperatura °C"
                    )
                    ultimaNotificacionTemp = System.currentTimeMillis()
                }
            }
        }
    }

    private fun mostrarNotificacionAlerta(titulo: String, mensaje: String) {
        val channelId = "alert_channel"
        val channel = NotificationChannel(
            channelId,
            "Alertas de Monitoreo",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()

        // ID único para cada alerta
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}