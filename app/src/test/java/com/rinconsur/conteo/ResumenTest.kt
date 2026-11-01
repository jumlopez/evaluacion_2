package com.rinconsur.conteo

import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RangosEtarios
import com.rinconsur.conteo.model.RegistroFlujo
import com.rinconsur.conteo.model.calcularResumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumenTest {

    private fun reg(
        acceso: Int,
        movimiento: Movimiento = Movimiento.ENTRADA,
        genero: Genero = Genero.FEMENINO,
        rango: String = RangosEtarios.ADULTO,
        cantidad: Int,
        ninos: Int = 0,
        compro: Boolean? = null
    ) = RegistroFlujo(
        accesoId = acceso,
        fechaHora = 0L,
        movimiento = movimiento.name,
        genero = genero.name,
        rangoEtario = rango,
        cantidad = cantidad,
        ninosAcompanantes = ninos,
        compro = compro
    )

    @Test
    fun `separa entradas y salidas por acceso y calcula aforo`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, cantidad = 30),
                reg(1, cantidad = 10),
                reg(1, Movimiento.SALIDA, cantidad = 25, compro = true),
                reg(1, Movimiento.SALIDA, cantidad = 15, compro = false),
                reg(2, cantidad = 5)
            )
        )
        val a1 = resumen.porAcceso.first { it.accesoId == 1 }
        assertEquals(40, a1.personasEntradas)
        assertEquals(40, a1.personasSalidas)
        assertEquals(25, a1.compradores)
        assertEquals(40, a1.salidasConDatoCompra)
        assertEquals(0, a1.aforoNeto)
        assertEquals(0.625f, a1.pctCompra, 0.001f)
        assertEquals(45, resumen.totalPersonasEntradas)
        assertEquals(40, resumen.totalPersonasSalidas)
        assertEquals(5, resumen.aforoNeto)
    }

    @Test
    fun `el aforo neto nunca es negativo`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, cantidad = 4),
                reg(1, Movimiento.SALIDA, cantidad = 30, compro = false),
                reg(2, Movimiento.SALIDA, cantidad = 7, compro = true)
            )
        )
        val a1 = resumen.porAcceso.first { it.accesoId == 1 }
        assertEquals(4, a1.personasEntradas)
        assertEquals(30, a1.personasSalidas)
        assertEquals(0, a1.aforoNeto)
        assertEquals(0, resumen.porAcceso.first { it.accesoId == 2 }.aforoNeto)
        assertEquals(0, resumen.aforoNeto)
    }

    @Test
    fun `siempre devuelve los cuatro accesos aunque no haya datos`() {
        val resumen = calcularResumen(emptyList())
        assertEquals(4, resumen.porAcceso.size)
        assertEquals(0f, resumen.porAcceso[0].pctCompra, 0f)
        assertTrue(resumen.sinDatos)
    }

    @Test
    fun `segmentacion por genero y edad solo cuenta entradas`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, genero = Genero.FEMENINO, rango = RangosEtarios.JOVEN, cantidad = 12),
                reg(1, genero = Genero.MASCULINO, rango = RangosEtarios.JOVEN, cantidad = 5),
                reg(
                    3,
                    Movimiento.SALIDA,
                    genero = Genero.MASCULINO,
                    rango = RangosEtarios.JOVEN,
                    cantidad = 99
                )
            )
        )
        assertEquals(12, resumen.generoEntradas.getValue("Femenino"))
        assertEquals(5, resumen.generoEntradas.getValue("Masculino"))
        assertEquals(17, resumen.edadEntradas.getValue(RangosEtarios.JOVEN))
        assertEquals(0, resumen.edadEntradas.getValue(RangosEtarios.ADULTO_MAYOR))
    }

    @Test
    fun `los rangos de edad antiguos se agrupan en la caracteristica etaria`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, rango = "<18", cantidad = 2),
                reg(1, rango = "18-29", cantidad = 4),
                reg(1, rango = "30-44", cantidad = 3),
                reg(1, rango = "45-59", cantidad = 1),
                reg(1, rango = "60+", cantidad = 5)
            )
        )
        assertEquals(listOf("Joven", "Adulto", "Adulto mayor"), resumen.edadEntradas.keys.toList())
        assertEquals(6, resumen.edadEntradas.getValue(RangosEtarios.JOVEN))
        assertEquals(4, resumen.edadEntradas.getValue(RangosEtarios.ADULTO))
        assertEquals(5, resumen.edadEntradas.getValue(RangosEtarios.ADULTO_MAYOR))
    }

    @Test
    fun `las etiquetas antiguas de edad ya no se muestran`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, rango = "18-29", cantidad = 4),
                reg(1, rango = "No declara", cantidad = 3)
            )
        )
        assertTrue("18-29" !in resumen.edadEntradas)
        assertTrue("No declara" !in resumen.edadEntradas)
        assertEquals(4, resumen.edadEntradas.getValue(RangosEtarios.JOVEN))
        assertEquals(3, resumen.edadEntradas.size)
    }

    @Test
    fun `los ninos acompanantes suman personas y se resumen solo en entradas`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, cantidad = 10, ninos = 2),
                reg(1, cantidad = 5, ninos = 0),
                reg(1, Movimiento.SALIDA, cantidad = 8, ninos = 1, compro = true)
            )
        )
        assertEquals(2, resumen.totalNinos)
        assertEquals(10, resumen.personasConNinos)
        assertEquals(17, resumen.totalPersonasEntradas)
        assertEquals(9, resumen.totalPersonasSalidas)
        assertEquals(15, resumen.totalAdultosEntradas)
        assertEquals(10f / 15f, resumen.pctConNinos, 0.001f)
        assertEquals(8, resumen.totalCompradores)
        assertEquals(8, resumen.totalConDatoCompra)
    }

    @Test
    fun `la compra sin dato no baja la conversion`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, Movimiento.SALIDA, cantidad = 4, compro = true),
                reg(1, Movimiento.SALIDA, cantidad = 6, compro = null)
            )
        )
        assertEquals(10, resumen.totalPersonasSalidas)
        assertEquals(4, resumen.totalConDatoCompra)
        assertEquals(0, resumen.totalNoCompradores)
        assertEquals(6, resumen.totalSinDatoCompra)
        assertEquals(1f, resumen.pctCompra, 0.001f)
    }

    @Test
    fun `separa compradores no compradores y salidas sin dato`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, Movimiento.SALIDA, cantidad = 7, compro = true),
                reg(1, Movimiento.SALIDA, cantidad = 3, compro = false),
                reg(2, Movimiento.SALIDA, cantidad = 5, compro = null)
            )
        )
        assertEquals(7, resumen.totalCompradores)
        assertEquals(3, resumen.totalNoCompradores)
        assertEquals(10, resumen.totalConDatoCompra)
        assertEquals(5, resumen.totalSinDatoCompra)
        assertEquals(15, resumen.totalPersonasSalidas)
    }

    @Test
    fun `la compra registrada en una entrada se ignora`() {
        val resumen = calcularResumen(
            listOf(
                reg(1, Movimiento.ENTRADA, cantidad = 100, compro = true),
                reg(1, Movimiento.SALIDA, cantidad = 5, compro = false)
            )
        )
        assertEquals(0, resumen.totalCompradores)
        assertEquals(5, resumen.totalConDatoCompra)
        assertEquals(0f, resumen.pctCompra, 0.001f)
    }
}
