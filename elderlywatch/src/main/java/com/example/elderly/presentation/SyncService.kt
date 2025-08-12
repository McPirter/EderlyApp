package com.example.elderly.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

class SyncService(private val context: Context) {

    fun enviarDatosAlTelefono(
        adultoId: String,
        heartRate: Float,
        temperatura: Double,
        latitud: Double,
        longitud: Double,
        nombre: String
    ) {
        val mensaje = "datos|$adultoId|$heartRate|$temperatura|$latitud|$longitud|$nombre|${System.currentTimeMillis()}"
        val client = Wearable.getMessageClient(context)

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    client.sendMessage(
                        node.id,
                        "/sync_datos",
                        mensaje.toByteArray()
                    ).addOnSuccessListener {
                        Log.d("SyncService", "✅ Datos enviados al nodo ${node.displayName}")
                    }.addOnFailureListener {
                        Log.e("SyncService", "❌ Error al enviar datos", it)
                    }
                }
            }.addOnFailureListener {
                Log.e("SyncService", "❌ No se pudieron obtener los nodos", it)
            }
    }
}
