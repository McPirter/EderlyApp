// Archivo: WearableDataRepository.kt
package com.example.elderly

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class DatosSensores(
    val adultoId: String,
    val heartRate: Float,
    val temperatura: Float,
    val lat: Double,
    val lon: Double
)

object WearableDataRepository {
    private val _nuevosDatos = MutableLiveData<DatosSensores>()
    val nuevosDatos: LiveData<DatosSensores> = _nuevosDatos

    fun actualizarDatos(datos: DatosSensores) {
        // Usamos postValue porque el servicio puede correr en un hilo diferente
        _nuevosDatos.postValue(datos)
    }
}