package com.example.elderly.models

data class LoginRequest(
    val usuario: String,
    val contra: String,
    val recordarDisp: Boolean = false
)
