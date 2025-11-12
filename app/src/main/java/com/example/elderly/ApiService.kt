package com.example.elderly.network

import com.example.elderly.models.Usuario
import com.example.elderly.models.Adulto
import com.example.elderly.models.AdultoList
import com.example.elderly.models.LoginRequest
import com.example.elderly.models.LoginResponse
import com.example.elderly.models.DashboardData
import com.example.elderly.models.Medicamento
import com.example.elderly.models.MedicamentoResponse
import com.example.elderly.models.TempResponse
import com.example.elderly.models.Temperatura
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path



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
    @GET("info-temp/{id}")
    fun getInfoTemp(@Path("id") id: String): Call<Temperatura>

}
