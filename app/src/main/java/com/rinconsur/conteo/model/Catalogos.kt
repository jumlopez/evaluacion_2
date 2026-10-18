package com.rinconsur.conteo.model

/** Movimiento observado en un acceso: la tienda necesita entradas y salidas. */
enum class Movimiento(val etiqueta: String) {
    ENTRADA("Entrada"),
    SALIDA("Salida")
}

/** Género declarado u observado de las personas del registro. */
enum class Genero(val etiqueta: String) {
    FEMENINO("Femenino"),
    MASCULINO("Masculino"),
    OTRO("Otro"),
    NO_DECLARA("No declara")
}

/** Rangos etarios usados para segmentar el flujo. */
object RangosEtarios {
    /** Valor por defecto cuando el operador no alcanza a estimar la edad. */
    const val NO_DECLARA = "No declara"

    val opciones = listOf("<18", "18-29", "30-44", "45-59", "60+", NO_DECLARA)
}

data class Acceso(val id: Int, val piso: Int, val nombre: String)

/**
 * La tienda tiene cuatro accesos: 1, 2 y 3 en el primer piso; 4 en el segundo piso.
 */
object Accesos {
    val lista = listOf(
        Acceso(1, 1, "Acceso 1"),
        Acceso(2, 1, "Acceso 2"),
        Acceso(3, 1, "Acceso 3"),
        Acceso(4, 2, "Acceso 4")
    )

    val ids = lista.map { it.id }

    fun nombre(id: Int) = lista.firstOrNull { it.id == id }?.nombre ?: "Acceso $id"

    fun piso(id: Int) = lista.firstOrNull { it.id == id }?.piso

    /** Etiqueta completa, por ejemplo "Acceso 3 · Piso 1". */
    fun etiqueta(id: Int) = piso(id)?.let { "${nombre(id)} · Piso $it" } ?: nombre(id)

    /** Etiqueta corta para los chips del formulario. */
    fun etiquetaCorta(id: Int) = piso(id)?.let { "Acceso $id · P$it" } ?: "Acceso $id"
}
