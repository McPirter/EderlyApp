package com.example.elderly.models

data class Usuario(
    val usuario: String,
    val correo: String,
    val contra: String,
    val telefono: String,
    val roluser: String = "familiar" // fijo para familiares
)
