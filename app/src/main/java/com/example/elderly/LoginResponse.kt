package com.example.elderly.models

data class LoginResponse(
    val message: String,
    val tempToken: String?,
    val tokenPermanente: String?,
    val rol: String,
    val userId: String?,
    val adultoId: String?
)
