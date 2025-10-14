// Archivo: DataLayerListenerService.kt
package com.example.elderly

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Este método se llama automáticamente en segundo plano cuando llega un mensaje
        if (messageEvent.path == "/sync_datos") {
            val rawData = String(messageEvent.data)
            Log.d("DataLayerService", "✅ Mensaje recibido en segundo plano: $rawData")
            processWearableData(rawData)
        }
    }

    private fun processWearableData(rawData: String) {
        try {
            if (!rawData.startsWith("datos|")) return

            val partes = rawData.split("|")
            if (partes.size >= 7) {
                val adultoId = partes[1]
                val heartRate = partes[2].toFloatOrNull() ?: 0.0f
                val temperatura = partes[3].toFloatOrNull() ?: 0.0f
                val lat = partes[4].toDoubleOrNull() ?: 0.0
                val lon = partes[5].toDoubleOrNull() ?: 0.0

                // Creamos el objeto y lo publicamos en el repositorio central
                val datos = DatosSensores(adultoId, heartRate, temperatura, lat, lon)
                WearableDataRepository.actualizarDatos(datos)
            }
        } catch (e: Exception) {
            Log.e("DataLayerService", "❌ Error al procesar datos", e)
        }
    }
}