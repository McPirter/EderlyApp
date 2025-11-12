package com.example.elderly.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.elderly.R
import java.util.concurrent.TimeUnit
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator


class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null

    // Referencias a la UI
    private lateinit var heartRateText: TextView
    private lateinit var estimatedTempText: TextView
    private lateinit var progressHeartRate: ProgressBar
    private lateinit var progressTemperature: ProgressBar

    // Servicios (se mantienen)
    private lateinit var gpsService: GPSService
    private lateinit var syncService: SyncService

    // Datos simulados (se mantienen)
    private val edad = 70
    private val presionSistolica = 135
    private val presionDiastolica = 85

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establece el nuevo layout que creamos
        setContentView(R.layout.activity_main)

        // Obtenemos SharedPreferences UNA SOLA VEZ al principio
        val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)

        // --- Log de Verificación (se mantiene) ---
        val idGuardado = sharedPref.getString("adultoId", "NADA GUARDADO")
        val nombreGuardado = sharedPref.getString("adultoNombre", "NADA GUARDADO")
        Log.d("LOGIN_CHECK", "--- Verificando datos al crear MainActivity ---")
        Log.d("LOGIN_CHECK", "ID en SharedPreferences: $idGuardado")
        Log.d("LOGIN_CHECK", "Nombre en SharedPreferences: $nombreGuardado")
        Log.d("LOGIN_CHECK", "------------------------------------------")

        // Inicialización de componentes
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        syncService = SyncService(this)
        gpsService = GPSService(this)
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        // Referencias a la UI
        heartRateText = findViewById(R.id.heartRateText)
        estimatedTempText = findViewById(R.id.estimatedTempText)
        progressHeartRate = findViewById(R.id.progress_heart_rate)
        progressTemperature = findViewById(R.id.progress_temperature)
        val nombreTextView = findViewById<TextView>(R.id.nombreAdultoText)

        // Botones (ahora ambos son ImageButton)
        val boton = findViewById<ImageButton>(R.id.btnRec) // <-- Cambio aquí
        val btnLogin = findViewById<ImageButton>(R.id.btnIrAlLogin)

        // Configuración de listeners
        boton.setOnClickListener {
            startActivity(Intent(this, Recordatorios::class.java))
        }
        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Mostrar nombre en la UI
        val nombreAdulto = intent.getStringExtra("adultoNombre") ?: sharedPref.getString("adultoNombre", null)
        if (!nombreAdulto.isNullOrEmpty()) {
            nombreTextView.text = nombreAdulto
            nombreTextView.visibility = TextView.VISIBLE
        } else {
            nombreTextView.visibility = TextView.GONE // Ocultar si no hay nombre
        }

        // Iniciar procesos
        checkAndRequestPermissions()
        val temperatura = estimarTemperatura(edad, presionSistolica, presionDiastolica)

        // Establecer un valor inicial en la UI
        actualizarMedidores(0f, temperatura) // Mostramos 0 bpm y la temp base
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = listOfNotNull(
            Manifest.permission.BODY_SENSORS.takeIf { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED },
            Manifest.permission.ACTIVITY_RECOGNITION.takeIf { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED },
            Manifest.permission.ACCESS_FINE_LOCATION.takeIf { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED },
            Manifest.permission.ACCESS_COARSE_LOCATION.takeIf { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        )

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 100)
        } else {
            iniciarMonitoreo()
        }
    }

    private fun iniciarMonitoreo() {
        iniciarSensor()
        gpsService.startLocationUpdates()
        // El servicio en segundo plano sigue siendo el responsable de ENVIAR datos
        val intent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    // Ya no necesitamos iniciarEnvioPeriodico(), checkAndSendData(), ni enviarDatosActuales() aquí.
    // Fueron eliminados.

    private fun iniciarSensor() {
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            heartRateText.text = "N/A"
            Toast.makeText(this, "Sensor de ritmo no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val hr = event.values[0]
            if (hr > 0) {
                val temp = estimarTemperaturaDinamica(hr, edad, presionSistolica, presionDiastolica)

                // --- CORRECCIÓN CLAVE ---
                // Ya no actualizamos los TextViews aquí.
                // Llamamos a la función centralizada para que actualice TODO.
                actualizarMedidores(hr, temp)
            }
        }
    }

    /**
     * Nueva función para actualizar todos los elementos de la UI a la vez.
     */
    private fun actualizarMedidores(ritmo: Float, temp: Double) {

        // --- 1. Actualizar Texto ---
        heartRateText.text = "${ritmo.toInt()} bpm"
        estimatedTempText.text = "%.1f°C".format(temp)

        // --- 2. Actualizar Medidor de Ritmo Cardíaco ---
        progressHeartRate.progress = ritmo.toInt().coerceAtMost(150)

        // --- 3. Actualizar Medidor de Temperatura ---
        val tempProgress = ((temp - 30) * 10).toInt() // De 30-40 a 0-100
        progressTemperature.progress = tempProgress.coerceIn(0, 100)
    }

    // Funciones de estimación de temperatura (se mantienen)
    private fun estimarTemperatura(edad: Int, sistolica: Int, diastolica: Int): Double {
        return 36.8 - (0.005 * (edad - 30)) - (0.002 * (sistolica - 120)) + (0.0015 * (diastolica - 80))
    }

    private fun estimarTemperaturaDinamica(
        heartRate: Float,
        edad: Int,
        sistolica: Int,
        diastolica: Int
    ): Double {
        return 36.8 - (0.005 * (edad - 30)) - (0.002 * (sistolica - 120)) +
                (0.0015 * (diastolica - 80)) + (0.01 * (heartRate - 60))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            iniciarMonitoreo()
        } else {
            Toast.makeText(this, "Permisos necesarios para la app", Toast.LENGTH_LONG).show()
            heartRateText.text = "Permisos denegados"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        gpsService.stopLocationUpdates()
        // 'handler' ya no existe, así que no es necesario limpiar 'sendRunnable'
    }
}