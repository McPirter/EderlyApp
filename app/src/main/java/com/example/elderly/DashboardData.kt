// models/DashboardData.kt
package com.example.elderly.models

import com.squareup.moshi.Json

data class DashboardData(
    val adulto: AdultoData,
    val temperaturas: List<Temperatura>,
    val presiones: List<Presion>,
    val ubicaciones: List<Ubicacion> = emptyList() // Nueva lista de ubicaciones
)

data class AdultoData(
    val id: String,  // Asegúrate de que este campo existe
    val nombre: String,
    val edad: Int
)

data class Ubicacion(
    val latitud: Double,
    val longitud: Double,
    val fecha: String
)

data class Temperatura(
    @field: Json (name = "_id") val adulto: AdultoMini,
    val fecha: String,
    val temp: Float
)

data class TempResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Temperatura?
)

data class Presion(
    val fecha: String,
    val pres_sistolica: Int,
    val pres_diastolica: Int
)

data class MedicamentoResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Medicamento?
)

data class Medicamento(
    @field:Json(name = "_id") val id: String,
    @field:Json(name = "adulto") val adulto: AdultoMini,
    @field:Json(name = "medicina") val medicina: String,
    @field:Json(name = "descripcion") val descripcion: String?,
    @field:Json(name = "tiempo") val tiempo: Int,
    @field:Json(name = "fecha") val fecha: String
)

data class AdultoMini(
    @field:Json(name = "_id") val id: String,
    @field:Json(name = "nombre") val nombre: String
)