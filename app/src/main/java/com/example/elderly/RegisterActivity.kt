package com.example.elderly

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.elderly.models.Usuario
import com.example.elderly.models.Adulto
import com.example.elderly.network.ApiClient
import com.google.android.material.textfield.TextInputLayout
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

            val tilNombreFamiliar = findViewById<TextInputLayout>(R.id.tilNombreFamiliar)
            val tilTelefono = findViewById<TextInputLayout>(R.id.tilTelefono)
            val tilCorreo = findViewById<TextInputLayout>(R.id.tilCorreo)
            val tilContrasena = findViewById<TextInputLayout>(R.id.tilContrasena)
            
            val tilNombreAdulto = findViewById<TextInputLayout>(R.id.tilNombreAdulto)
            val tilDia = findViewById<TextInputLayout>(R.id.tilDia)
            val tilMes = findViewById<TextInputLayout>(R.id.tilMes)
            val tilAnio = findViewById<TextInputLayout>(R.id.tilAnio)
            val tilPeso = findViewById<TextInputLayout>(R.id.tilPeso)
            val tilAltura = findViewById<TextInputLayout>(R.id.tilAltura)

            val valido = validarCampos(
                nombreFamiliar, telefono, correo, contrasena,
                nombreAdulto, dia, mes, anio, peso, altura,
                tilNombreFamiliar, tilTelefono, tilCorreo, tilContrasena,
                tilNombreAdulto, tilDia, tilMes, tilAnio, tilPeso, tilAltura
            )

            if (!valido) return@setOnClickListener




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
    private fun validarCampos(
        nombreFamiliar: String,
        telefono: String,
        correo: String,
        contrasena: String,
        nombreAdulto: String,
        dia: String,
        mes: String,
        anio: String,
        peso: String,
        altura: String,
        tilNombreFamiliar: TextInputLayout,
        tilTelefono: TextInputLayout,
        tilCorreo: TextInputLayout,
        tilContrasena: TextInputLayout,
        tilNombreAdulto: TextInputLayout,
        tilDia: TextInputLayout,
        tilMes: TextInputLayout,
        tilAnio: TextInputLayout,
        tilPeso: TextInputLayout,
        tilAltura: TextInputLayout
    ): Boolean {

        val allTil = listOf(
            tilNombreFamiliar, tilTelefono, tilCorreo, tilContrasena,
            tilNombreAdulto, tilDia, tilMes, tilAnio, tilPeso, tilAltura
        )
        allTil.forEach { it.error = null }

        // Verifica si todos están vacíos
        if (nombreFamiliar.isEmpty() && telefono.isEmpty() && correo.isEmpty() && contrasena.isEmpty() &&
            nombreAdulto.isEmpty() && dia.isEmpty() && mes.isEmpty() && anio.isEmpty() &&
            peso.isEmpty() && altura.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }

        var valido = true

        if (nombreFamiliar.isEmpty()) {
            tilNombreFamiliar.error = "Campo requerido"
            valido = false
        } else if (!nombreFamiliar.all { it.isLetter() || it.isWhitespace() }) {
            tilNombreFamiliar.error = "Nombre inválido"
            valido = false
        }

        if (telefono.isEmpty()) {
            tilTelefono.error = "Campo requerido"
            valido = false
        } else if (telefono.length != 10 || !telefono.all { it.isDigit() }) {
            tilTelefono.error = "Teléfono inválido"
            valido = false
        }

        if (correo.isEmpty()) {
            tilCorreo.error = "Campo requerido"
            valido = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.error = "Correo inválido"
            valido = false
        }

        if (contrasena.isEmpty()) {
            tilContrasena.error = "Campo requerido"
            valido = false
        } else if (contrasena.length < 6) {
            tilContrasena.error = "Mínimo 6 caracteres"
            valido = false
        }

        if (nombreAdulto.isEmpty()) {
            tilNombreAdulto.error = "Campo requerido"
            valido = false
        } else if (!nombreAdulto.all { it.isLetter() || it.isWhitespace() }) {
            tilNombreAdulto.error = "Nombre inválido"
            valido = false
        }

        val diaInt = dia.toIntOrNull()
        if (dia.isEmpty()) {
            tilDia.error = "Campo requerido"
            valido = false
        } else if (diaInt == null || diaInt !in 1..31) {
            tilDia.error = "Día inválido"
            valido = false
        }

        val mesInt = mes.toIntOrNull()
        if (mes.isEmpty()) {
            tilMes.error = "Campo requerido"
            valido = false
        } else if (mesInt == null || mesInt !in 1..12) {
            tilMes.error = "Mes inválido"
            valido = false
        }

        val anioInt = anio.toIntOrNull()
        if (anio.isEmpty()) {
            tilAnio.error = "Campo requerido"
            valido = false
        } else if (anioInt == null || anioInt !in 1900..2100) {
            tilAnio.error = "Año inválido"
            valido = false
        }

        val pesoFloat = peso.toFloatOrNull()
        if (peso.isEmpty()) {
            tilPeso.error = "Campo requerido"
            valido = false
        } else if (pesoFloat == null || pesoFloat <= 0) {
            tilPeso.error = "Peso inválido"
            valido = false
        }

        val alturaFloat = altura.toFloatOrNull()
        if (altura.isEmpty()) {
            tilAltura.error = "Campo requerido"
            valido = false
        } else if (alturaFloat == null || alturaFloat <= 0) {
            tilAltura.error = "Altura inválida"
            valido = false
        }

        return valido
    }
}

