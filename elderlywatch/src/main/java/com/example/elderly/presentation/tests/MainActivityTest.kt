package com.example.elderly.presentation.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test



class MainActivityTest {
    private fun estimarTemperaturaDinamica(
        heartRate: Float,
        edad: Int,
        sistolica: Int,
        diastolica: Int
    ): Double {
        return 36.8 - (0.005 * (edad - 30)) - (0.002 * (sistolica - 120)) +
                (0.0015 * (diastolica - 80)) + (0.01 * (heartRate - 60))
    }

    @Test
    fun `debe calcular temperatura correctamente`() {
        val resultado = estimarTemperaturaDinamica(
            heartRate = 70f,
            edad = 40,
            sistolica = 120,
            diastolica = 80
        )
        assertEquals(36.75, resultado, 0.001)
    }
}