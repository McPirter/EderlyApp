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
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.elderly.R
import java.util.concurrent.TimeUnit

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private lateinit var heartRateText: TextView
    private lateinit var estimatedTempText: TextView
    private lateinit var gpsService: GPSService
    private lateinit var syncService: SyncService

    private val SEND_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1L)
    private var lastSendTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val sendRunnable = object : Runnable {
        override fun run() {
            checkAndSendData()
            handler.postDelayed(this, SEND_INTERVAL_MS)
        }
    }

    private val edad = 70
    private val presionSistolica = 135
    private val presionDiastolica = 85

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtenemos SharedPreferences UNA SOLA VEZ al principio
        val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)

        // --- Log de Verificación ---
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
        val nombreTextView = findViewById<TextView>(R.id.nombreAdultoText)
        val boton = findViewById<Button>(R.id.btnRec)
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
            nombreTextView.visibility = TextView.GONE
        }

        // Iniciar procesos
        checkAndRequestPermissions()
        val temperatura = estimarTemperatura(edad, presionSistolica, presionDiastolica)
        estimatedTempText.text = "Temperatura estimada: %.2f°C".format(temperatura)
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
        val intent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
        //iniciarEnvioPeriodico()
    }

    private fun iniciarEnvioPe3riodico() {
        handler.removeCallbacks(sendRunnable) // Limpiamos cualquier runnable anterior
        handler.post(sendRunnable) // Iniciamos inmediatamente y luego se programa el siguiente
    }

    private fun checkAndSendData() {
        enviarDatosActuales()
    }

    private fun enviarDatosActuales() {
        val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        val adultoId = sharedPref.getString("adultoId", "ID_NO_ENCONTRADO")
        val nombre = sharedPref.getString("adultoNombre", "NOMBRE_NO_ENCONTRADO")
        val location = gpsService.getLastKnownLocation()

        val heartRate = heartRateText.text.toString().filter { it.isDigit() }.toFloatOrNull() ?: 0.0f
        val temperatura = estimatedTempText.text.toString().substringAfter(": ").substringBefore("°C").toDoubleOrNull() ?: 0.0

        Log.d("DATOS_A_ENVIAR", """
            --- Intentando enviar datos ---
            - ID Leído:          $adultoId
            - Nombre Leído:      $nombre
            - Ritmo Cardíaco:    $heartRate
            - Temperatura:       $temperatura
            - Latitud:           ${location?.latitude ?: "No disponible"}
            - Longitud:          ${location?.longitude ?: "No disponible"}
            -------------------------------
        """)

        // Solo cancelamos si faltan los datos del login
        if (adultoId == "ID_NO_ENCONTRADO" || nombre == "NOMBRE_NO_ENCONTRADO") {
            Log.w("DATOS_A_ENVIAR", "Envío cancelado: Faltan datos del login.")
            return
        }

        val latitudParaEnviar = location?.latitude ?: 0.0
        val longitudParaEnviar = location?.longitude ?: 0.0

        syncService.enviarDatosAlTelefono(
            adultoId,
            heartRate,
            temperatura,
            latitudParaEnviar,
            longitudParaEnviar,
            nombre
        )
    }

    private fun iniciarSensor() {
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            heartRateText.text = "Sensor de ritmo cardíaco no disponible"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val hr = event.values[0]
            if (hr > 0) {
                heartRateText.text = "Ritmo cardíaco: ${hr.toInt()} bpm"
                val temp = estimarTemperaturaDinamica(hr, edad, presionSistolica, presionDiastolica)
                estimatedTempText.text = "Temperatura estimada: %.2f°C".format(temp)
            }
        }
    }

    // Funciones de estimación de temperatura, onAccuracyChanged, etc. se mantienen igual
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
        handler.removeCallbacks(sendRunnable)
        gpsService.stopLocationUpdates()
    }
}