// Archivo: app/ApiPollingService.kt
package com.example.elderly

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.elderly.models.AdultoList
import com.example.elderly.models.DashboardData
import com.example.elderly.network.ApiClient
import com.example.elderly.network.Gps
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class ApiPollingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var userId: String? = null
    private val POLLING_INTERVAL = 30_000L // 30 segundos (puedes cambiarlo a 10_000L si prefieres)

    companion object {
        private const val TAG = "ApiPollingService"
        const val EXTRA_USER_ID = "extra_user_id"
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "🔄 Sincronizando datos (pull periódico) desde la API...")
            if (userId != null) {
                jalarDatosDeAPI(userId!!)
            }
            // Volvemos a programar la siguiente ejecución
            handler.postDelayed(this, POLLING_INTERVAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Servicio de Polling iniciado.")

        // Obtenemos el userId la primera vez que se inicia
        if (intent?.hasExtra(EXTRA_USER_ID) == true) {
            userId = intent.getStringExtra(EXTRA_USER_ID)
        }

        // Empezamos el bucle de sincronización
        handler.removeCallbacks(syncRunnable) // Quitamos cualquier bucle anterior
        handler.post(syncRunnable) // Empezamos el nuevo

        return START_STICKY // El servicio intentará reiniciarse si el sistema lo mata
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detenemos el bucle cuando el servicio se destruye
        handler.removeCallbacks(syncRunnable)
        Log.d(TAG, "Servicio de Polling detenido.")
    }

    private fun jalarDatosDeAPI(userId: String) {
        ApiClient.instance.getAdultosPorUsuario(userId).enqueue(object : Callback<List<AdultoList>> {
            override fun onResponse(call: Call<List<AdultoList>>, response: Response<List<AdultoList>>) {
                if (response.isSuccessful) {
                    response.body()?.forEach { adulto ->
                        // Para cada adulto, jalamos sus datos de sensores
                        jalarDatosDeAdulto(adulto._id)
                        jalarDatosGps(adulto._id)
                    }
                }
            }
            override fun onFailure(call: Call<List<AdultoList>>, t: Throwable) {
                Log.e(TAG, "Error al jalar lista de adultos", t)
            }
        })
    }

    private fun jalarDatosDeAdulto(adultoId: String) {
        ApiClient.instance.getInfoAdulto(adultoId).enqueue(object : Callback<DashboardData> {
            override fun onResponse(call: Call<DashboardData>, response: Response<DashboardData>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        val ultimaTemp = data.temperaturas.lastOrNull()?.temp?.toFloat() ?: 0f
                        val ultimoRitmo = data.presiones.lastOrNull()?.pres_sistolica?.toFloat() ?: 0f

                        if (ultimaTemp != 0f || ultimoRitmo != 0f) {
                            val datosHistoricos = DatosSensores(
                                adultoId = adultoId,
                                heartRate = ultimoRitmo,
                                temperatura = ultimaTemp,
                                lat = 0.0,
                                lon = 0.0
                            )
                            // Publicamos en el "pizarrón" para que las Activities se enteren
                            WearableDataRepository.actualizarDatos(datosHistoricos)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<DashboardData>, t: Throwable) {
                Log.e(TAG, "Error al jalar datos de adulto $adultoId", t)
            }
        })
    }

    private fun jalarDatosGps(adultoId: String) {
        ApiClient.instance.getInfoGps(adultoId).enqueue(object : Callback<List<Gps>> {
            override fun onResponse(call: Call<List<Gps>>, response: Response<List<Gps>>) {
                if (response.isSuccessful) {
                    response.body()?.lastOrNull()?.let { ultimoGps ->
                        val lon = ultimoGps.coordenadas.getOrNull(0) ?: 0.0
                        val lat = ultimoGps.coordenadas.getOrNull(1) ?: 0.0

                        if (lat != 0.0 && lon != 0.0) {
                            // Publicamos también los datos de GPS
                            val datosGps = DatosSensores(
                                adultoId = adultoId,
                                heartRate = 0f, // No nos importa el HR en este paquete
                                temperatura = 0f, // No nos importa la Temp en este paquete
                                lat = lat,
                                lon = lon
                            )
                            WearableDataRepository.actualizarDatos(datosGps)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<Gps>>, t: Throwable) {
                Log.e(TAG, "Error al jalar GPS de $adultoId", t)
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
}