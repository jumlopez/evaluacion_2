package com.rinconsur.conteo

import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Movimiento
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
        movimiento = Movimiento.ENTRADA,
        conteoPorGenero = mapOf(Genero.FEMENINO to 2, Genero.MASCULINO to 1),
        ninosAcompanantes = 1,
        rangoEtario = "18-29",
        compro = true
    )

    @Test
    fun `estado inicial sin personas marca error de conteo`() {
        val errores = validarRegistro(RegistroUiState())
        assertNotNull(errores.conteo)
        assertNull(errores.acceso)
        assertNull(errores.rango)
        assertNull(errores.ninos)
        assertTrue(errores.hayErrores())
    }

    @Test
    fun `registro con personas es valido`() {
        assertFalse(validarRegistro(valido).hayErrores())
    }

    @Test
    fun `acceso y rango invalidos se rechazan`() {
        assertNotNull(validarRegistro(valido.copy(accesoId = 9)).acceso)
        assertNotNull(validarRegistro(valido.copy(rangoEtario = "99")).rango)
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
    fun `conteo por genero tiene tope de 500`() {
        val muchos = valido.copy(conteoPorGenero = mapOf(Genero.MASCULINO to 501))
        assertEquals("Máximo 500 personas por registro", validarRegistro(muchos).conteo)
    }

    @Test
    fun `la compra es opcional en la entrada`() {
        assertFalse(validarRegistro(valido.copy(compro = null)).hayErrores())
    }

    @Test
    fun `construir registros crea uno por genero y adjunta ninos una vez`() {
        val registros = construirRegistros(valido, fechaHora = 123L)
        assertEquals(2, registros.size)
        assertEquals(3, registros.sumOf { it.cantidad })
        assertEquals(1, registros.sumOf { it.ninosAcompanantes })
        assertTrue(registros.all { it.accesoId == 2 && it.fechaHora == 123L && it.compro == true })
        assertTrue(registros.all { it.rangoEtario == "18-29" })
    }

    @Test
    fun `la salida nunca guarda compra`() {
        val registros = construirRegistros(valido.copy(movimiento = Movimiento.SALIDA), 1L)
        assertTrue(registros.all { it.movimiento == Movimiento.SALIDA.name })
        assertTrue(registros.all { it.compro == null })
    }

    @Test
    fun `sin personas no se construyen registros`() {
        assertTrue(construirRegistros(RegistroUiState(), 1L).isEmpty())
    }

    @Test
    fun `cantidad por genero no baja de cero`() {
        val estado = RegistroUiState().conCantidad(Genero.FEMENINO, -3)
        assertEquals(0, estado.cantidad(Genero.FEMENINO))
    }
}
