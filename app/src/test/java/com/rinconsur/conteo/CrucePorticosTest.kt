package com.rinconsur.conteo

import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RegistroFlujo
import com.rinconsur.conteo.model.calcularCruce
import com.rinconsur.conteo.model.calcularResumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrucePorticosTest {

    private fun reg(
        acceso: Int,
        movimiento: Movimiento = Movimiento.ENTRADA,
        cantidad: Int,
        ninos: Int = 0
    ) = RegistroFlujo(
        accesoId = acceso,
        fechaHora = 0L,
        movimiento = movimiento.name,
        genero = Genero.FEMENINO.name,
        rangoEtario = "18-29",
        cantidad = cantidad,
        ninosAcompanantes = ninos
    )

    private fun portico(acceso: Int, entradas: Int, salidas: Int) = ConteoPortico(
        accesoId = acceso,
        fechaMillis = 0L,
        entradas = entradas,
        salidas = salidas,
        actualizadoEn = 0L
    )

    @Test
    fun `cruza porticos con lo registrado y calcula cobertura`() {
        val registros = listOf(
            reg(1, cantidad = 8, ninos = 2),
            reg(2, cantidad = 5),
            reg(1, Movimiento.SALIDA, cantidad = 4)
        )
        val porticos = listOf(portico(1, entradas = 20, salidas = 10), portico(2, entradas = 5, salidas = 0))

        val cruce = calcularCruce(calcularResumen(registros), porticos)

        assertEquals(4, cruce.filas.size)
        val a1 = cruce.filas.first { it.accesoId == 1 }
        assertEquals(20, a1.porticoEntradas)
        assertEquals(10, a1.registradoEntradas)
        assertEquals(10, a1.diferenciaEntradas)
        assertEquals(0.5f, a1.coberturaEntradas!!, 0.001f)
        assertEquals(4, a1.registradoSalidas)
        assertEquals(6, a1.diferenciaSalidas)

        assertEquals(25, cruce.totalPorticoEntradas)
        assertEquals(15, cruce.totalRegistradoEntradas)
        assertEquals(0.6f, cruce.coberturaEntradas!!, 0.001f)
        assertTrue(cruce.hayDatosPortico)
    }

    @Test
    fun `sin conteos de portico no hay cobertura`() {
        val cruce = calcularCruce(calcularResumen(emptyList()), emptyList())
        assertFalse(cruce.hayDatosPortico)
        assertNull(cruce.coberturaEntradas)
        assertEquals(4, cruce.filas.size)
        assertTrue(cruce.filas.all { it.coberturaEntradas == null })
    }

    @Test
    fun `varias lecturas del mismo acceso se suman`() {
        val cruce = calcularCruce(
            calcularResumen(listOf(reg(3, cantidad = 10))),
            listOf(portico(3, entradas = 6, salidas = 1), portico(3, entradas = 4, salidas = 2))
        )
        val a3 = cruce.filas.first { it.accesoId == 3 }
        assertEquals(10, a3.porticoEntradas)
        assertEquals(3, a3.porticoSalidas)
        assertEquals(1f, a3.coberturaEntradas!!, 0.001f)
    }

    @Test
    fun `la cobertura puede superar el cien por ciento`() {
        val cruce = calcularCruce(
            calcularResumen(listOf(reg(4, cantidad = 12))),
            listOf(portico(4, entradas = 10, salidas = 0))
        )
        assertEquals(1.2f, cruce.coberturaEntradas!!, 0.001f)
        assertEquals(-2, cruce.filas.first { it.accesoId == 4 }.diferenciaEntradas)
    }
}
