package com.example.elderly


import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.elderly.models.Adulto
import com.example.elderly.network.ApiClient
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class RegistrarAdulto : AppCompatActivity() {
    override fun onCreate (savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nuevo_adulto)

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val nombreAdulto = findViewById<EditText>(R.id.etNombreAdulto).text.toString()
            val dia = findViewById<EditText>(R.id.etDia).text.toString()
            val mes = findViewById<EditText>(R.id.etMes).text.toString()
            val anio = findViewById<EditText>(R.id.etAnio).text.toString()
            val condiciones = findViewById<EditText>(R.id.etCondiciones).text.toString()
            val peso = findViewById<EditText>(R.id.etPeso).text.toString()
            val altura = findViewById<EditText>(R.id.etAltura).text.toString()

            val tilNombreAdulto = findViewById<TextInputLayout>(R.id.tilNombreAdulto)
            val tilDia = findViewById<TextInputLayout>(R.id.tilDia)
            val tilMes = findViewById<TextInputLayout>(R.id.tilMes)
            val tilAnio = findViewById<TextInputLayout>(R.id.tilAnio)
            val tilPeso = findViewById<TextInputLayout>(R.id.tilPeso)
            val tilAltura = findViewById<TextInputLayout>(R.id.tilAltura)

            val valido = validarCampos(
                nombreAdulto, dia, mes, anio, peso, altura,
                tilNombreAdulto, tilDia, tilMes, tilAnio, tilPeso, tilAltura
            )

            if (!valido) return@setOnClickListener

            val sharedPreferences = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
            val userId = sharedPreferences.getString("userId", null)!!

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
                        Toast.makeText(this@RegistrarAdulto, "Adulto registrado correctamente", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e("RegistrarAdulto", "Error al registrar adulto")
                        Toast.makeText(this@RegistrarAdulto, "Error al registrar adulto", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Log.e("RegistrarAdulto", "Fallo en adulto: ${t.message}", t)
                    Toast.makeText(this@RegistrarAdulto, "Fallo en adulto: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }

        }

    private fun validarCampos(

        nombreAdulto: String,
        dia: String,
        mes: String,
        anio: String,
        peso: String,
        altura: String,
        tilNombreAdulto: TextInputLayout,
        tilDia: TextInputLayout,
        tilMes: TextInputLayout,
        tilAnio: TextInputLayout,
        tilPeso: TextInputLayout,
        tilAltura: TextInputLayout
    ): Boolean {
        val allTil = listOf(
            tilNombreAdulto, tilDia, tilMes, tilAnio, tilPeso, tilAltura
        )
        allTil.forEach { it.error = null }

        if (nombreAdulto.isEmpty() && dia.isEmpty() && mes.isEmpty()
            && anio.isEmpty() && peso.isEmpty() && altura.isEmpty()) {
            Toast.makeText(this, "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }
        var valido = true

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