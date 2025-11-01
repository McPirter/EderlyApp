package com.example.elderly

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class LoginUtilsTest {

    @Test
    fun `retorna false si usuario esta vacio`() {
        println("Test: Usuario vacío")
        val resultado = validarCredenciales("", "1234")
        println("Resultado esperado: false, Resultado real: $resultado")
        assertFalse(resultado)
    }

    @Test
    fun `retorna false si contraseña esta vacia`() {
        println("Test: Contraseña vacía")
        val resultado = validarCredenciales("user", "")
        println("Resultado esperado: false, Resultado real: $resultado")
        assertFalse(resultado)
    }

    @Test //hola
    fun `retorna true si ambos campos llenos`() {
        println("Test: Usuario y contraseña llenos")
        val resultado = validarCredenciales("user", "1234")
        println("Resultado esperado: true, Resultado real: $resultado")
        assertTrue(resultado)
    }
}
