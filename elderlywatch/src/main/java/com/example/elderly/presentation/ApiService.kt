package com.example.elderly.presentation.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

data class LoginRequest(
    val usuario: String,
    val contra: String,
    val recordarDisp: Boolean = false
)

data class LoginResponse(
    val tempToken: String,
    val tokenPermanente: String?,
    val rol: String,
    val userId: String,
    val adultoId: String? // si ya tiene uno asignado
)

data class Adulto(
    val _id: String,
    val nombre: String,
    val edad: Int
)

data class MedicamentoResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Medicamento?
)

data class TempRequest(
    val adulto: String,
    val temp: Double,
    val fecha: Long
)

data class PresionRequest(
    val adulto: String,
    val pres_sistolica: Float, // Usaremos esto para guardar el heartRate
    val pres_diastolica: Float = 0f, // Valor de relleno
    val fecha: Long
)

data class GpsRequest(
    val adulto: String,
    val coordenadas: List<Double>, // [longitud, latitud]
    val fecha_salida: Long // Usaremos la misma fecha para todo
)

data class Medicamento(
    @SerializedName("_id") val id: String,
    @SerializedName("adulto") val adulto: AdultoMini,
    @SerializedName("medicina") val medicina: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("tiempo") val tiempo: Int,
    @SerializedName("fecha") val fecha: String
)

data class AdultoMini(
    @SerializedName("_id") val id: String,
    @SerializedName("nombre") val nombre: String
)





// En elderlywatch/presentation/network/ApiService.kt

interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("por-usuario/{userId}")
    fun obtenerAdultos(@Path("userId") userId: String): Call<List<Adulto>>

    @GET("info-medicamento-compat/{id}")
    fun getMedicamentosPorAdulto(@Path("id") id: String): Call<List<Medicamento>>

    // --- AÑADE ESTAS NUEVAS RUTAS ---

    @POST("registrar-temp")
    fun registrarTemp(@Body datos: TempRequest): Call<TempResponse> // Asumiendo que tu API celular tiene TempResponse

    @POST("registrar-presion")
    fun registrarPresion(@Body datos: PresionRequest): Call<Void> // Asumiendo que no devuelve nada

    @POST("registrar-gps")
    fun registrarGps(@Body datos: GpsRequest): Call<Void> // Asumiendo que no devuelve nada
}

// Necesitarás añadir esta data class también (cópiala de tu API celular)
data class TempResponse(
    val success: Boolean,
    val data: Temperatura?
)

data class Temperatura(
    @SerializedName("_id") val id: String,
    val temp: Double
)