package com.example.elderly.presentation.network

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

interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("por-usuario/{userId}")
    fun obtenerAdultos(@Path("userId") userId: String): Call<List<Adulto>>
}
