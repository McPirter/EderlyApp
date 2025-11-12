package com.example.elderly.network

import com.example.elderly.models.* // Asegúrate de que todos tus modelos estén aquí
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName // Asegúrate de tener esta importación

// --- NUEVA DATA CLASS: Presion (cópiala de tu API) ---
data class Presion(
    val _id: String,
    val fecha: String,
    val pres_sistolica: Float,
    val pres_diastolica: Float,
    val adulto: AdultoList
)

// --- NUEVA DATA CLASS: Gps (cópiala de tu API) ---
// Nota: Tu API devuelve 'coordenadas' como un Array de Números.
data class Gps(
    val _id: String,
    val coordenadas: List<Double>, // [lon, lat]
    val fecha_salida: String,
    val adulto: AdultoList
)

// --- NUEVA DATA CLASS: Temperatura (cópiala de tu API) ---
data class Temperatura(
    @SerializedName("_id") val _id: String,
    val temp: Double,
    val fecha: String,
    val adulto: AdultoList
)


interface ApiService {

    @POST("register")
    fun registrarUsuario(@Body usuario: Usuario): Call<Map<String, String>>
    @POST("registro-adulto")
    fun registrarAdulto(@Body adulto: Adulto): Call<Map<String, String>>
    @POST("login")
    fun loginUsuario(@Body loginRequest: LoginRequest): Call<LoginResponse>
    @GET("info-adulto/{id}")
    fun getInfoAdulto(@Path("id") id: String): Call<DashboardData>
    @GET("por-usuario/{userId}")
    fun getAdultosPorUsuario(@Path("userId") userId: String): Call<List<AdultoList>>
    @POST("registrar-medicamento")
    fun registrarMedicamento(@Body medicamento: RequestBody): Call<MedicamentoResponse>
    @GET("info-medicamento-compat/{id}")
    fun getMedicamentosPorAdulto(@Path("id") id: String): Call<List<Medicamento>>
    @POST("registrar-temp")
    fun registrarTemp(@Body temperatura: RequestBody): Call<TempResponse>

    // --- RUTAS PARA "JALAR" DATOS ---

    @GET("info-temp/{id}")
    fun getInfoTemp(@Path("id") id: String): Call<List<Temperatura>> // Cambiado a Lista

    @GET("info-presion/{id}")
    fun getInfoPresion(@Path("id") id: String): Call<List<Presion>>

    @GET("info-gps/{id}")
    fun getInfoGps(@Path("id") id: String): Call<List<Gps>>
}