package com.example.elderly.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.elderly.R
import com.example.elderly.presentation.network.ApiClient
import com.example.elderly.presentation.network.LoginRequest
import com.example.elderly.presentation.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val inputUser = findViewById<EditText>(R.id.editUsuario)
        val inputPass = findViewById<EditText>(R.id.editPassword)

        btnLogin.setOnClickListener {
            val user = inputUser.text.toString()
            val pass = inputPass.text.toString()

            if (user.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Campos vacíos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(user, pass, recordarDisp = false)

            ApiClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginData = response.body()!!
                        val intent = Intent(this@LoginActivity, SeleccionarAdultoActivity::class.java).apply {
                            putExtra("userId", loginData.userId)
                            putExtra("token", loginData.tempToken)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Login", "Error HTTP ${response.code()}: $errorBody")

                        Toast.makeText(
                            this@LoginActivity,
                            "Error al iniciar sesión: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("Login", "Fallo en login", t)
                    Toast.makeText(this@LoginActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
