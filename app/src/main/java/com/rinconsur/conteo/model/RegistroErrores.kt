package com.rinconsur.conteo.model

/** Tope de personas adultas que se pueden capturar en un mismo registro. */
const val MAX_PERSONAS_POR_REGISTRO = 500

/** Tope de niños acompañantes por registro (también limita el botón "+1"). */
const val MAX_NINOS_ACOMPANANTES = 20

/** Errores de validación del registro rápido. */
data class RegistroErrores(
    val acceso: String? = null,
    val conteo: String? = null,
    val ninos: String? = null
) {
    fun hayErrores(): Boolean = listOfNotNull(acceso, conteo, ninos).isNotEmpty()
}

/**
 * Validación pura (sin Android) para poder probarla con tests unitarios. Los textos
 * se mantienen en el modelo porque son parte del contrato verificado por los tests.
 */
fun validarRegistro(estado: RegistroUiState): RegistroErrores = RegistroErrores(
    acceso = if (estado.accesoId in Accesos.ids) null else "Selecciona un acceso",
    conteo = when {
        estado.totalPersonas == 0 -> "Registra al menos una persona"
        estado.totalPersonas > MAX_PERSONAS_POR_REGISTRO ->
            "Máximo $MAX_PERSONAS_POR_REGISTRO personas por registro"

        else -> null
    },
    ninos = if (estado.ninosAcompanantes !in 0..MAX_NINOS_ACOMPANANTES) {
        "Debe estar entre 0 y $MAX_NINOS_ACOMPANANTES"
    } else {
        null
    }
)
