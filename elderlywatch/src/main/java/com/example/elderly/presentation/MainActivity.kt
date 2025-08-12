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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.elderly.R
import com.example.elderly.presentation.theme.EderlyAppTheme

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null

    private lateinit var heartRateText: TextView
    private lateinit var estimatedTempText: TextView
    private lateinit var gpsService: GPSService

    private val REQUEST_PERMISSIONS_CODE = 100

    // Datos simulados
    private val edad = 70
    private val presionSistolica = 135
    private val presionDiastolica = 85

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        val adultoIdGuardado = sharedPref.getString("adultoId", null)

        val btnLogin = findViewById<ImageButton>(R.id.btnIrAlLogin)
        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val nombreTextView = findViewById<TextView>(R.id.nombreAdultoText)
        val nombreAdulto = intent.getStringExtra("adultoNombre") ?: sharedPref.getString("adultoNombre", null)
        if (!nombreAdulto.isNullOrEmpty()) {
            nombreTextView.text = nombreAdulto
            nombreTextView.visibility = TextView.VISIBLE
        } else {
            nombreTextView.visibility = TextView.GONE
        }

        heartRateText = findViewById(R.id.heartRateText)
        estimatedTempText = findViewById(R.id.estimatedTempText)

        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        gpsService = GPSService(this)

        checkAndRequestPermissions()

        val temperatura = estimarTemperatura(edad, presionSistolica, presionDiastolica)
        estimatedTempText.text = "Temperatura estimada: %.2f°C".format(temperatura)
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BODY_SENSORS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.HIGH_SAMPLING_RATE_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }


        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        } else {
            // Si ya tiene permisos, iniciar todo normalmente
            iniciarSensor()
            gpsService.startLocationUpdates()
            iniciarForegroundService()
        }
    }

    private fun iniciarForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun iniciarSensor() {
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            heartRateText.text = "Sensor de ritmo cardíaco no disponible"
        }
    }

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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0]
            if (heartRate > 0) {
                heartRateText.text = "Ritmo cardíaco: ${heartRate.toInt()} bpm"
                val temperatura = estimarTemperaturaDinamica(heartRate, edad, presionSistolica, presionDiastolica)
                estimatedTempText.text = "Temperatura estimada: %.2f°C".format(temperatura)

                val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
                val adultoId = sharedPref.getString("adultoId", null) ?: return
                val nombre = sharedPref.getString("adultoNombre", null) ?: return
                val location = gpsService.getLastKnownLocation() ?: return

                val syncService = SyncService(this)
                syncService.enviarDatosAlTelefono(
                    adultoId,
                    heartRate,
                    temperatura,
                    location.latitude,
                    location.longitude,
                    nombre

                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesario para esta app
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
            if (denied) {
                Toast.makeText(this, "Permisos necesarios para la app", Toast.LENGTH_LONG).show()
                heartRateText.text = "Permisos denegados"
            } else {
                iniciarSensor()
                gpsService.startLocationUpdates()
                iniciarForegroundService()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

@Composable
fun WearApp(greetingName: String) {
    EderlyAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}
