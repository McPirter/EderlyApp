package com.example.elderly

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.elderly.models.Usuario
import com.example.elderly.models.Adulto
import com.example.elderly.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnSiguiente = findViewById<Button>(R.id.btnSiguiente)

        btnSiguiente.setOnClickListener {
            val nombreFamiliar = findViewById<EditText>(R.id.etNombreFamiliar).text.toString()
            val telefono = findViewById<EditText>(R.id.etTelefono).text.toString()
            val correo = findViewById<EditText>(R.id.etCorreo).text.toString()
            val contrasena = findViewById<EditText>(R.id.etContrasena).text.toString()

            val nombreAdulto = findViewById<EditText>(R.id.etNombreAdulto).text.toString()
            val dia = findViewById<EditText>(R.id.etDia).text.toString()
            val mes = findViewById<EditText>(R.id.etMes).text.toString()
            val anio = findViewById<EditText>(R.id.etAnio).text.toString()
            val condiciones = findViewById<EditText>(R.id.etCondiciones).text.toString()
            val peso = findViewById<EditText>(R.id.etPeso).text.toString()
            val altura = findViewById<EditText>(R.id.etAltura).text.toString()

            if (nombreFamiliar.isEmpty() || telefono.isEmpty() || correo.isEmpty() || contrasena.isEmpty() ||
                nombreAdulto.isEmpty() || dia.isEmpty() || mes.isEmpty() || anio.isEmpty() || peso.isEmpty() || altura.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usuario = Usuario(
                usuario = nombreFamiliar,
                correo = correo,
                contra = contrasena,
                telefono = telefono,
                roluser = "Usuario"
            )

            ApiClient.instance.registrarUsuario(usuario).enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val userId = body?.get("userId")

                        if (!userId.isNullOrEmpty()) {
                            Toast.makeText(this@RegisterActivity, "Usuario registrado", Toast.LENGTH_SHORT).show()

                            val edadCalculada = 2025 - anio.toIntOrNull()!!

                            val adulto = Adulto(
                                nombre = nombreAdulto,
                                edad = edadCalculada,
                                lim_presion = 120,
                                lim_tiempo_cuarto = 10,
                                userId = userId
                            )

                            ApiClient.instance.registrarAdulto(adulto).enqueue(object : Callback<Map<String, String>> {
                                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(this@RegisterActivity, "Adulto registrado correctamente", Toast.LENGTH_LONG).show()
                                    } else {
                                        Log.e("RegisterActivity", "Error al registrar adulto")
                                        Toast.makeText(this@RegisterActivity, "Error al registrar adulto", Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                                    Log.e("RegisterActivity", "Fallo en adulto: ${t.message}", t)
                                    Toast.makeText(this@RegisterActivity, "Fallo en adulto: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                        } else {
                            Toast.makeText(this@RegisterActivity, "No se obtuvo ID del usuario", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error al registrar usuario", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Fallo en usuario: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("RegisterActivity", "Fallo en usuario: ${t.message}", t)
                }
            })
        }
    }
}
