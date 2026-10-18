package com.rinconsur.conteo.model

/** Errores de validación del registro rápido. */
data class RegistroErrores(
    val acceso: String? = null,
    val rango: String? = null,
    val conteo: String? = null,
    val ninos: String? = null
) {
    fun hayErrores(): Boolean = listOfNotNull(acceso, rango, conteo, ninos).isNotEmpty()
}

/**
 * Estado del registro rápido: el operador suma personas por género con un toque.
 * `compro` es opcional (null = sin dato) porque al momento de la entrada puede no
 * conocerse si la persona comprará.
 */
data class RegistroUiState(
    val accesoId: Int = Accesos.ids.first(),
    val movimiento: Movimiento = Movimiento.ENTRADA,
    val conteoPorGenero: Map<Genero, Int> = Genero.entries.associateWith { 0 },
    val ninosAcompanantes: Int = 0,
    val rangoEtario: String = RangosEtarios.NO_DECLARA,
    val compro: Boolean? = null,
    val errores: RegistroErrores = RegistroErrores()
) {
    val totalPersonas: Int get() = conteoPorGenero.values.sum()
    val totalConNinos: Int get() = totalPersonas + ninosAcompanantes

    fun cantidad(genero: Genero): Int = conteoPorGenero[genero] ?: 0

    fun conCantidad(genero: Genero, cantidad: Int): RegistroUiState = copy(
        conteoPorGenero = conteoPorGenero + (genero to cantidad.coerceAtLeast(0)),
        errores = errores.copy(conteo = null)
    )
}

/** Validación pura (sin Android) para poder probarla con tests unitarios. */
fun validarRegistro(estado: RegistroUiState): RegistroErrores = RegistroErrores(
    acceso = if (estado.accesoId in Accesos.ids) null else "Selecciona un acceso",
    rango = if (estado.rangoEtario in RangosEtarios.opciones) null else "Selecciona un rango etario",
    conteo = when {
        estado.totalPersonas == 0 -> "Registra al menos una persona"
        estado.totalPersonas > 500 -> "Máximo 500 personas por registro"
        else -> null
    },
    ninos = if (estado.ninosAcompanantes !in 0..20) "Debe estar entre 0 y 20" else null
)

/**
 * Convierte el estado en los registros que se guardan en Room. Se crea un registro
 * por género con personas; los niños se adjuntan una sola vez para no duplicarlos.
 */
fun construirRegistros(estado: RegistroUiState, fechaHora: Long): List<RegistroFlujo> {
    val grupos = Genero.entries.mapNotNull { genero ->
        val cantidad = estado.cantidad(genero)
        if (cantidad > 0) genero to cantidad else null
    }
    if (grupos.isEmpty()) return emptyList()

    val compro = if (estado.movimiento == Movimiento.ENTRADA) estado.compro else null
    return grupos.mapIndexed { indice, (genero, cantidad) ->
        RegistroFlujo(
            accesoId = estado.accesoId,
            fechaHora = fechaHora,
            movimiento = estado.movimiento.name,
            genero = genero.name,
            rangoEtario = estado.rangoEtario,
            cantidad = cantidad,
            ninosAcompanantes = if (indice == 0) estado.ninosAcompanantes else 0,
            compro = compro
        )
    }
}
