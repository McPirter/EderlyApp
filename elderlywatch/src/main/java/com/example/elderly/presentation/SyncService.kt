package com.example.elderly.presentation

import android.content.Context
import android.util.Log
import com.example.elderly.presentation.network.* // Importa las nuevas data classes
import com.google.android.gms.wearable.Wearable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SyncService(private val context: Context) {

    // Inicializamos el ApiClient del reloj
    private val apiClient = ApiClient.instance

    fun enviarDatosAlTelefono(
        adultoId: String?,
        heartRate: Float,
        temperatura: Double,
        latitud: Double,
        longitud: Double,
        nombre: String?
    ) {
        if (adultoId == null || nombre == null) {
            Log.w("SyncService", "Datos de adulto nulos, envío cancelado.")
            return
        }

        val client = Wearable.getMessageClient(context)

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    // --- NODO NO ENCONTRADO -> ENVIAR A API ---
                    Log.d("SyncService", "📱 No se encontró el celular. Enviando a la API...")
                    enviarDatosALaApi(adultoId, heartRate, temperatura, latitud, longitud)
                } else {
                    // --- NODO ENCONTRADO -> ENVIAR AL CELULAR ---
                    Log.d("SyncService", "📱 Celular encontrado. Enviando por MessageClient...")
                    val mensaje = "datos|$adultoId|$heartRate|$temperatura|$latitud|$longitud|$nombre|${System.currentTimeMillis()}"
                    Log.d("SyncService", "📦 Contenido del mensaje: $mensaje")

                    for (node in nodes) {
                        client.sendMessage(
                            node.id,
                            "/sync_datos",
                            mensaje.toByteArray()
                        ).addOnSuccessListener {
                            Log.d("SyncService", "✅ Datos enviados al nodo ${node.displayName}")
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("SyncService", "❌ Error al buscar nodos: ${e.message}")
                Log.w("SyncService", "Fallback: Enviando a la API por fallo al buscar nodos.")
                enviarDatosALaApi(adultoId, heartRate, temperatura, latitud, longitud)
            }
    }

    /**
     * Sube los datos directamente a tu servidor web usando las rutas existentes.
     */
    private fun enviarDatosALaApi(adultoId: String, hr: Float, temp: Double, lat: Double, lon: Double) {
        val fechaActual = System.currentTimeMillis()
        Log.d("SyncService", "📤 Preparando subida a API para $adultoId")

        // 1. Subir Temperatura
        if (temp > 0) {
            val tempRequest = TempRequest(adulto = adultoId, temp = temp, fecha = fechaActual)
            apiClient.registrarTemp(tempRequest).enqueue(object : Callback<TempResponse> {
                override fun onResponse(call: Call<TempResponse>, response: Response<TempResponse>) {
                    if(response.isSuccessful) Log.d("SyncService", "✅ API: Temperatura subida.")
                    else Log.e("SyncService", "❌ API: Error al subir Temp: ${response.code()}")
                }
                override fun onFailure(call: Call<TempResponse>, t: Throwable) {
                    Log.e("SyncService", "❌ API: Fallo de red (Temp): ${t.message}")
                }
            })
        }

        // 2. Subir Ritmo Cardíaco (usando la ruta de Presión)
        if (hr > 0) {
            val presionRequest = PresionRequest(adulto = adultoId, pres_sistolica = hr, fecha = fechaActual)
            apiClient.registrarPresion(presionRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if(response.isSuccessful) Log.d("SyncService", "✅ API: Ritmo (Presión) subido.")
                    else Log.e("SyncService", "❌ API: Error al subir Ritmo: ${response.code()}")
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("SyncService", "❌ API: Fallo de red (Ritmo): ${t.message}")
                }
            })
        }

        // 3. Subir Ubicación
        if (lat != 0.0 || lon != 0.0) {
            val gpsRequest = GpsRequest(adulto = adultoId, coordenadas = listOf(lon, lat), fecha_salida = fechaActual)
            apiClient.registrarGps(gpsRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if(response.isSuccessful) Log.d("SyncService", "✅ API: GPS subido.")
                    else Log.e("SyncService", "❌ API: Error al subir GPS: ${response.code()}")
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("SyncService", "❌ API: Fallo de red (GPS): ${t.message}")
                }
            })
        }
    }
}