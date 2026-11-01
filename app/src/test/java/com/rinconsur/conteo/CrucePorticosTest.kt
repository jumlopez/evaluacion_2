package com.rinconsur.conteo

import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RangosEtarios
import com.rinconsur.conteo.model.RegistroFlujo
import com.rinconsur.conteo.model.calcularCruce
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
        rangoEtario = RangosEtarios.ADULTO,
        cantidad = cantidad,
        ninosAcompanantes = ninos
    )

    private fun portico(acceso: Int, personas: Int) = ConteoPortico(
        accesoId = acceso,
        fechaMillis = 0L,
        personas = personas,
        actualizadoEn = 0L
    )

    @Test
    fun `compara el total del portico con entradas mas salidas registradas`() {
        val registros = listOf(
            reg(1, cantidad = 8, ninos = 2),
            reg(2, cantidad = 5),
            reg(1, Movimiento.SALIDA, cantidad = 4)
        )
        val porticos = listOf(portico(1, personas = 30), portico(2, personas = 5))

        val cruce = calcularCruce(registros, porticos)

        assertEquals(4, cruce.filas.size)
        val a1 = cruce.filas.first { it.accesoId == 1 }
        assertEquals(30, a1.porticoPersonas)
        assertEquals(10, a1.registradoEntradas)
        assertEquals(4, a1.registradoSalidas)
        assertEquals(14, a1.registradoTotal)
        assertEquals(16, a1.diferencia)
        assertEquals(14f / 30f, a1.cobertura!!, 0.001f)

        assertEquals(35, cruce.totalPorticoPersonas)
        assertEquals(19, cruce.totalRegistrado)
        assertEquals(16, cruce.diferencia)
        assertEquals(19f / 35f, cruce.cobertura!!, 0.001f)
        assertTrue(cruce.hayDatosPortico)
    }

    @Test
    fun `sin conteos de portico no hay cobertura`() {
        val cruce = calcularCruce(emptyList(), emptyList())
        assertFalse(cruce.hayDatosPortico)
        assertNull(cruce.cobertura)
        assertEquals(4, cruce.filas.size)
        assertTrue(cruce.filas.all { it.cobertura == null && it.diferencia == 0 })
    }

    @Test
    fun `varias lecturas del mismo acceso se suman`() {
        val cruce = calcularCruce(
            listOf(reg(3, cantidad = 10)),
            listOf(portico(3, personas = 6), portico(3, personas = 4))
        )
        val a3 = cruce.filas.first { it.accesoId == 3 }
        assertEquals(10, a3.porticoPersonas)
        assertEquals(10, a3.registradoTotal)
        assertEquals(0, a3.diferencia)
        assertEquals(1f, a3.cobertura!!, 0.001f)
    }

    @Test
    fun `la cobertura puede superar el cien por ciento`() {
        val cruce = calcularCruce(
            listOf(reg(4, cantidad = 8), reg(4, Movimiento.SALIDA, cantidad = 4)),
            listOf(portico(4, personas = 10))
        )
        val a4 = cruce.filas.first { it.accesoId == 4 }
        assertEquals(12, a4.registradoTotal)
        assertEquals(1.2f, a4.cobertura!!, 0.001f)
        assertEquals(-2, a4.diferencia)
    }

    @Test
    fun `las entradas y salidas no se comparan por separado`() {
        val soloEntradas = calcularCruce(
            listOf(reg(1, cantidad = 10)),
            listOf(portico(1, personas = 10))
        )
        val soloSalidas = calcularCruce(
            listOf(reg(1, Movimiento.SALIDA, cantidad = 10)),
            listOf(portico(1, personas = 10))
        )

        assertEquals(1f, soloEntradas.cobertura!!, 0.001f)
        assertEquals(1f, soloSalidas.cobertura!!, 0.001f)
        assertEquals(soloEntradas.totalRegistrado, soloSalidas.totalRegistrado)
    }
}
