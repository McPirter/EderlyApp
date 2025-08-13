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





interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>


    @GET("por-usuario/{userId}")
    fun obtenerAdultos(@Path("userId") userId: String): Call<List<Adulto>>

    @GET("info-medicamento-compat/{id}")
    fun getMedicamentosPorAdulto(@Path("id") id: String): Call<List<Medicamento>>
}
