package com.rinconsur.conteo

import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.GrupoConteo
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RangosEtarios
import com.rinconsur.conteo.model.RegistroUiState
import com.rinconsur.conteo.model.construirRegistros
import com.rinconsur.conteo.model.validarRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionesTest {

    private val valido = RegistroUiState(
        accesoId = 2,
        movimiento = Movimiento.SALIDA,
        conteo = mapOf(
            GrupoConteo(Genero.FEMENINO, RangosEtarios.ADULTO, compro = true) to 2,
            GrupoConteo(Genero.MASCULINO, RangosEtarios.JOVEN, compro = false) to 1
        ),
        ninosAcompanantes = 1
    )

    @Test
    fun `estado inicial sin personas marca error de conteo`() {
        val errores = validarRegistro(RegistroUiState())
        assertNotNull(errores.conteo)
        assertNull(errores.acceso)
        assertNull(errores.ninos)
        assertTrue(errores.hayErrores())
    }

    @Test
    fun `registro con personas es valido`() {
        assertFalse(validarRegistro(valido).hayErrores())
    }

    @Test
    fun `acceso invalido se rechaza`() {
        assertNotNull(validarRegistro(valido.copy(accesoId = 9)).acceso)
    }

    @Test
    fun `ninos acompanantes debe estar entre 0 y 20`() {
        assertNull(validarRegistro(valido.copy(ninosAcompanantes = 0)).ninos)
        assertEquals(
            "Debe estar entre 0 y 20",
            validarRegistro(valido.copy(ninosAcompanantes = 21)).ninos
        )
    }

    @Test
    fun `conteo tiene tope de 500 personas`() {
        val muchos = valido.copy(
            conteo = mapOf(GrupoConteo(Genero.MASCULINO, RangosEtarios.ADULTO) to 501)
        )
        assertEquals("Máximo 500 personas por registro", validarRegistro(muchos).conteo)
    }

    @Test
    fun `la compra es opcional en la salida`() {
        val sinDato = valido.copy(
            conteo = mapOf(GrupoConteo(Genero.FEMENINO, RangosEtarios.ADULTO, compro = null) to 3)
        )
        assertFalse(validarRegistro(sinDato).hayErrores())
    }

    @Test
    fun `cantidad por genero suma todas las caracteristicas y compras`() {
        val estado = RegistroUiState()
            .conCantidad(Genero.FEMENINO, RangosEtarios.JOVEN, 2)
            .conCantidad(Genero.FEMENINO, RangosEtarios.ADULTO_MAYOR, 3, compro = true)
            .conCantidad(Genero.MASCULINO, RangosEtarios.ADULTO, 1, compro = false)

        assertEquals(5, estado.cantidad(Genero.FEMENINO))
        assertEquals(1, estado.cantidad(Genero.MASCULINO))
        assertEquals(3, estado.cantidad(Genero.FEMENINO, RangosEtarios.ADULTO_MAYOR, true))
        assertEquals(6, estado.totalPersonas)
    }

    @Test
    fun `el detalle respeta el orden del catalogo`() {
        val estado = RegistroUiState()
            .conCantidad(Genero.MASCULINO, RangosEtarios.ADULTO_MAYOR, 1)
            .conCantidad(Genero.FEMENINO, RangosEtarios.JOVEN, 1)

        val etiquetas = estado.gruposRegistrados.map { (grupo, _) -> grupo.descripcion }
        assertEquals(listOf("Femenino · Joven", "Masculino · Adulto mayor"), etiquetas)
    }

    @Test
    fun `el detalle de la salida muestra la compra`() {
        val estado = RegistroUiState(movimiento = Movimiento.SALIDA)
            .conCantidad(Genero.FEMENINO, RangosEtarios.ADULTO, 1, compro = true)
            .conCantidad(Genero.FEMENINO, RangosEtarios.ADULTO, 1, compro = false)

        val etiquetas = estado.gruposRegistrados.map { (grupo, _) -> grupo.descripcion }
        assertEquals(
            listOf("Femenino · Adulto · Compró", "Femenino · Adulto · No compró"),
            etiquetas
        )
    }

    @Test
    fun `construir registros crea uno por combinacion y adjunta ninos una vez`() {
        val registros = construirRegistros(valido, fechaHora = 123L)

        assertEquals(2, registros.size)
        assertEquals(3, registros.sumOf { it.cantidad })
        assertEquals(1, registros.sumOf { it.ninosAcompanantes })
        assertTrue(registros.all { it.accesoId == 2 && it.fechaHora == 123L })
        assertTrue(registros.all { it.movimiento == Movimiento.SALIDA.name })
        assertEquals(
            setOf(RangosEtarios.ADULTO, RangosEtarios.JOVEN),
            registros.map { it.rangoEtario }.toSet()
        )
        assertEquals(
            true,
            registros.first { it.genero == Genero.FEMENINO.name }.compro
        )
        assertEquals(
            false,
            registros.first { it.genero == Genero.MASCULINO.name }.compro
        )
    }

    @Test
    fun `la compra se guarda por persona contada`() {
        val estado = RegistroUiState(movimiento = Movimiento.SALIDA)
            .conCantidad(Genero.MASCULINO, RangosEtarios.JOVEN, 1, compro = true)
            .conCantidad(Genero.MASCULINO, RangosEtarios.JOVEN, 1, compro = false)
            .conCantidad(Genero.MASCULINO, RangosEtarios.JOVEN, 1, compro = null)

        val registros = construirRegistros(estado, 1L)

        assertEquals(3, registros.size)
        assertEquals(setOf(true, false, null), registros.map { it.compro }.toSet())
        assertTrue(registros.all { it.cantidad == 1 })
    }

    @Test
    fun `la entrada nunca guarda compra`() {
        val estado = RegistroUiState(movimiento = Movimiento.ENTRADA)
            .conCantidad(Genero.FEMENINO, RangosEtarios.JOVEN, 2)
        val registros = construirRegistros(estado, 1L)
        assertEquals(1, registros.size)
        assertTrue(registros.all { it.movimiento == Movimiento.ENTRADA.name })
        assertTrue(registros.all { it.compro == null })
    }

    @Test
    fun `volver a entrada descarta la compra ya capturada`() {
        val salida = RegistroUiState(movimiento = Movimiento.SALIDA)
            .conCantidad(Genero.FEMENINO, RangosEtarios.ADULTO, 2, compro = true)
            .conCantidad(Genero.FEMENINO, RangosEtarios.ADULTO, 1, compro = false)

        val entrada = salida.conMovimiento(Movimiento.ENTRADA)

        assertEquals(3, entrada.totalPersonas)
        assertEquals(3, entrada.cantidad(Genero.FEMENINO, RangosEtarios.ADULTO))
        assertTrue(construirRegistros(entrada, 1L).all { it.compro == null })
    }

    @Test
    fun `sin personas no se construyen registros`() {
        assertTrue(construirRegistros(RegistroUiState(), 1L).isEmpty())
    }

    @Test
    fun `cantidad por grupo no baja de cero`() {
        val estado = RegistroUiState()
            .conCantidad(Genero.FEMENINO, RangosEtarios.JOVEN, -3)
        assertEquals(0, estado.cantidad(Genero.FEMENINO, RangosEtarios.JOVEN))
        assertTrue(estado.gruposRegistrados.isEmpty())
    }
}
