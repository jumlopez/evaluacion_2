package com.rinconsur.conteo.model

/**
 * Combinación que el operador cuenta: género observado, característica etaria y, en
 * las salidas, si la persona compró (null = sin dato). En las entradas `compro`
 * siempre es null porque todavía no se sabe.
 */
data class GrupoConteo(
    val genero: Genero,
    val rango: String,
    val compro: Boolean? = null
) {
    /** Etiqueta corta para el detalle del conteo. */
    val descripcion: String
        get() = buildString {
            append(genero.etiqueta)
            append(" · ")
            append(rango)
            if (compro != null) {
                append(" · ")
                append(if (compro) "Compró" else "No compró")
            }
        }
}
