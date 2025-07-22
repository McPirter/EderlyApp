package com.example.elderly.models

data class Adulto(
    val nombre: String,
    val edad: Int,
    val lim_presion: Int,
    val lim_tiempo_cuarto: Int,
    val userId: String // será el ID que devuelva el registro
)
