package com.example.elderly

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import org.json.JSONObject

class WearMessageReceiver : WearableListenerService() {

    data class DatoSensor(
        val adultoId: String,
        val heartRate: Float,
        val temperatura: Float,
        val lat: Double,
        val lon: Double,
        val timestamp: Long
    )

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/sync_datos") {
            val data = String(event.data)
            Log.d("WearReceiver", "📩 Datos recibidos: $data")

            val partes = data.split("|")
            if (partes.size >= 7) {
                val dato = DatoSensor(
                    adultoId = partes[1],
                    heartRate = partes[2].toFloatOrNull() ?: -1f,
                    temperatura = partes[3].toFloatOrNull() ?: -1f,
                    lat = partes[4].toDoubleOrNull() ?: 0.0,
                    lon = partes[5].toDoubleOrNull() ?: 0.0,
                    timestamp = partes[6].toLongOrNull() ?: System.currentTimeMillis()
                )

                Log.d("WearReceiver", "🧠 ID=${dato.adultoId} HR=${dato.heartRate} Temp=${dato.temperatura} Lat=${dato.lat} Lon=${dato.lon} Time=${dato.timestamp}")

                guardarDatoLocal(applicationContext, dato)
            }
        }
    }

    private fun guardarDatoLocal(context: Context, dato: DatoSensor) {
        val prefs = context.getSharedPreferences("datos_recibidos", Context.MODE_PRIVATE)
        val jsonPrevio = prefs.getString("datos", "[]")
        val lista = JSONArray(jsonPrevio)

        val obj = JSONObject().apply {
            put("adultoId", dato.adultoId)
            put("heartRate", dato.heartRate)
            put("temperatura", dato.temperatura)
            put("lat", dato.lat)
            put("lon", dato.lon)
            put("timestamp", dato.timestamp)
        }

        lista.put(obj)
        prefs.edit().putString("datos", lista.toString()).apply()
        Log.d("WearReceiver", "✅ Dato guardado en SharedPreferences")
    }
}
