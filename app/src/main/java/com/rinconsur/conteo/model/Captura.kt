package com.rinconsur.conteo.model

/** Tope de personas adultas que se pueden capturar en un mismo registro. */
const val MAX_PERSONAS_POR_REGISTRO = 500

/** Tope de niños acompañantes por registro (también limita el botón "+1"). */
const val MAX_NINOS_ACOMPANANTES = 20

/**
 * Combinación que el operador cuenta: género observado, característica etaria y, en
 * las salidas, si la persona compró (null = sin dato). En las entradas `compro`
 * siempre es null porque todavía no se sabe.
 */
data class GrupoConteo(val genero: Genero, val rango: String, val compro: Boolean? = null) {
    val descripcion: String
        get() = genero.etiqueta + " · " + rango +
            (compro?.let { if (it) " · Compró" else " · No compró" } ?: "")
}

/** Variantes de compra que se manejan para el movimiento actual. */
fun opcionesCompra(movimiento: Movimiento): List<Boolean?> =
    if (movimiento == Movimiento.SALIDA) listOf(true, false, null) else listOf(null)

/** Errores de validación del registro rápido. */
data class RegistroErrores(
    val acceso: String? = null,
    val conteo: String? = null,
    val ninos: String? = null
) {
    fun hayErrores(): Boolean = acceso != null || conteo != null || ninos != null
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

/**
 * Estado del registro rápido: el operador toca un género, elige la característica
 * etaria en el primer popup y, si es una salida, responde si compró en el segundo.
 */
data class RegistroUiState(
    val accesoId: Int = Accesos.ids.first(),
    val movimiento: Movimiento = Movimiento.ENTRADA,
    val conteo: Map<GrupoConteo, Int> = emptyMap(),
    val ninosAcompanantes: Int = 0,
    val errores: RegistroErrores = RegistroErrores()
) {
    val totalPersonas: Int get() = conteo.values.sum()
    val totalConNinos: Int get() = totalPersonas + ninosAcompanantes

    /** Personas contadas para un género, sumando todas las características etarias. */
    fun cantidad(genero: Genero): Int =
        conteo.entries.filter { it.key.genero == genero }.sumOf { it.value }

    fun cantidad(genero: Genero, rango: String, compro: Boolean? = null): Int =
        conteo[GrupoConteo(genero, rango, compro)] ?: 0

    fun conCantidad(
        genero: Genero,
        rango: String,
        cantidad: Int,
        compro: Boolean? = null
    ): RegistroUiState {
        val clave = GrupoConteo(genero, rango, compro)
        val nuevo = cantidad.coerceAtLeast(0)
        return copy(
            conteo = if (nuevo == 0) conteo - clave else conteo + (clave to nuevo),
            errores = errores.copy(conteo = null)
        )
    }

    /**
     * Cambia el movimiento. Al volver a entrada se descarta el dato de compra de las
     * personas ya contadas, porque la entrada no pregunta si compró.
     */
    fun conMovimiento(movimiento: Movimiento): RegistroUiState = when {
        movimiento == this.movimiento -> this
        movimiento == Movimiento.ENTRADA -> copy(movimiento = movimiento, conteo = sinCompra())
        else -> copy(movimiento = movimiento)
    }

    private fun sinCompra(): Map<GrupoConteo, Int> = conteo.entries
        .groupBy({ it.key.copy(compro = null) }) { it.value }
        .mapValues { (_, cantidades) -> cantidades.sum() }

    /** Deja el grupo en captura vacío sin cambiar el acceso ni el movimiento. */
    fun limpiarConteo(): RegistroUiState = copy(
        conteo = emptyMap(),
        ninosAcompanantes = 0,
        errores = RegistroErrores()
    )

    /** Grupos con al menos una persona, en el orden del catálogo. */
    val gruposRegistrados: List<Pair<GrupoConteo, Int>>
        get() = generosCapturables.flatMap { genero ->
            RangosEtarios.opciones.flatMap { rango ->
                opcionesCompra(movimiento).mapNotNull { compro ->
                    val cantidad = cantidad(genero, rango, compro)
                    if (cantidad > 0) GrupoConteo(genero, rango, compro) to cantidad else null
                }
            }
        }
}

/**
 * Convierte el estado en los registros que se guardan en Room. Se crea un registro
 * por combinación género/característica/compra con personas; los niños se adjuntan
 * una sola vez para no duplicarlos. La compra solo se guarda en las salidas.
 */
fun construirRegistros(estado: RegistroUiState, fechaHora: Long): List<RegistroFlujo> {
    val grupos = estado.gruposRegistrados
    if (grupos.isEmpty()) return emptyList()

    val esSalida = estado.movimiento == Movimiento.SALIDA
    return grupos.mapIndexed { indice, (grupo, cantidad) ->
        RegistroFlujo(
            accesoId = estado.accesoId,
            fechaHora = fechaHora,
            movimiento = estado.movimiento.name,
            genero = grupo.genero.name,
            rangoEtario = grupo.rango,
            cantidad = cantidad,
            ninosAcompanantes = if (indice == 0) estado.ninosAcompanantes else 0,
            compro = if (esSalida) grupo.compro else null
        )
    }
}
