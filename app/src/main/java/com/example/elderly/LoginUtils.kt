package com.example.elderly

// Función pura que valida si los campos de login están llenos
fun validarCredenciales(username: String, password: String): Boolean {
    return username.isNotBlank() && password.isNotBlank()
}
