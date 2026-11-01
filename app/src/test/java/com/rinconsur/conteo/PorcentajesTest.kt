package com.rinconsur.conteo

import com.rinconsur.conteo.ui.comoPorcentaje
import org.junit.Assert.assertEquals
import org.junit.Test

class PorcentajesTest {

    @Test
    fun `las proporciones conocidas se redondean al entero mas cercano`() {
        assertEquals(0, 0f.comoPorcentaje())
        assertEquals(50, 0.5f.comoPorcentaje())
        assertEquals(100, 1f.comoPorcentaje())
        assertEquals(33, (1f / 3f).comoPorcentaje())
        assertEquals(67, (2f / 3f).comoPorcentaje())
    }

    @Test
    fun `los porcentajes con decimales se redondean hacia arriba`() {
        assertEquals(13, 0.125f.comoPorcentaje())
        assertEquals(88, 0.875f.comoPorcentaje())
    }

    @Test
    fun `las proporciones fuera de rango no se recortan`() {
        assertEquals(0, 0f.comoPorcentaje())
        assertEquals(150, 1.5f.comoPorcentaje())
    }
}
