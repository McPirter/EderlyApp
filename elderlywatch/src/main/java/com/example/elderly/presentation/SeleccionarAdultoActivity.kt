package com.example.elderly.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.elderly.R
import com.example.elderly.presentation.network.ApiClient
import com.example.elderly.presentation.network.Adulto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context


class SeleccionarAdultoActivity : AppCompatActivity() {

    private lateinit var adultosListView: ListView
    private var adultoSeleccionadoId: String? = null
    private lateinit var adultos: List<Adulto>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_adulto)

        adultosListView = findViewById(R.id.listViewAdultos)
        val userId = intent.getStringExtra("userId") ?: return

        ApiClient.instance.obtenerAdultos(userId).enqueue(object : Callback<List<Adulto>> {
            override fun onResponse(call: Call<List<Adulto>>, response: Response<List<Adulto>>) {
                if (response.isSuccessful) {
                    adultos = response.body() ?: emptyList()
                    val nombres = adultos.map { it.nombre }
                    val adapter = ArrayAdapter(this@SeleccionarAdultoActivity, android.R.layout.simple_list_item_1, nombres)
                    adultosListView.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<Adulto>>, t: Throwable) {
                Log.e("Adulto", "Error al cargar adultos", t)
            }
        })

        adultosListView.setOnItemClickListener { _, _, position, _ ->
            adultoSeleccionadoId = adultos[position]._id
            val adultoNombre = adultos[position].nombre

// Guardar también en SharedPreferences si lo deseas
            val sharedPref = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
            sharedPref.edit().putString("adultoId", adultoSeleccionadoId)
                .putString("adultoNombre", adultoNombre)
                .apply()

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("adultoId", adultoSeleccionadoId)
                putExtra("adultoNombre", adultoNombre)
            }
            startActivity(intent)
            finish()

        }
    }
}
