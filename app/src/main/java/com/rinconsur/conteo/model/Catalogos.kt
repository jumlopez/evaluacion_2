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

/** Géneros que el operador puede contar en la captura rápida. */
val generosCapturables = listOf(Genero.FEMENINO, Genero.MASCULINO)

/**
 * Características etarias que el operador elige al contar cada persona.
 * Los rangos de edad de versiones anteriores ("<18", "18-29", "30-44", "45-59",
 * "60+") ya no se muestran: se agrupan en estas tres características.
 */
object RangosEtarios {
    const val JOVEN = "Joven"
    const val ADULTO = "Adulto"
    const val ADULTO_MAYOR = "Adulto mayor"

    val opciones = listOf(JOVEN, ADULTO, ADULTO_MAYOR)

    /** Equivalencia de los rangos de edad guardados por versiones anteriores. */
    private val equivalencias = mapOf(
        "<18" to JOVEN,
        "18-29" to JOVEN,
        "30-44" to ADULTO,
        "45-59" to ADULTO,
        "60+" to ADULTO_MAYOR
    )

    /**
     * Lleva cualquier valor guardado a una de las tres características actuales.
     * Devuelve null cuando el registro histórico no permite clasificarlo
     * (por ejemplo el antiguo "No declara").
     */
    fun normalizar(valor: String): String? =
        if (valor in opciones) valor else equivalencias[valor]
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
