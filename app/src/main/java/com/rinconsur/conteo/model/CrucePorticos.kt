package com.rinconsur.conteo.model

/** Proporción registrado/pórtico; null si el pórtico no marcó nada. */
private fun cobertura(registrado: Int, portico: Int): Float? =
    if (portico > 0) registrado.toFloat() / portico else null

/** Comparación, por acceso, entre lo que marcó el pórtico y lo registrado en la app. */
data class FilaCruce(
    val accesoId: Int,
    val porticoEntradas: Int,
    val porticoSalidas: Int,
    val registradoEntradas: Int,
    val registradoSalidas: Int
) {
    val diferenciaEntradas: Int get() = porticoEntradas - registradoEntradas
    val diferenciaSalidas: Int get() = porticoSalidas - registradoSalidas
    val coberturaEntradas: Float? get() = cobertura(registradoEntradas, porticoEntradas)
}

data class CrucePorticos(val filas: List<FilaCruce>) {
    val totalPorticoEntradas: Int get() = filas.sumOf { it.porticoEntradas }
    val totalRegistradoEntradas: Int get() = filas.sumOf { it.registradoEntradas }

    val hayDatosPortico: Boolean
        get() = filas.any { it.porticoEntradas > 0 || it.porticoSalidas > 0 }

    val coberturaEntradas: Float? get() = cobertura(totalRegistradoEntradas, totalPorticoEntradas)
}

/**
 * Cruza el conteo de pórticos con las personas registradas en la app (ya resumidas).
 * Los niños acompañantes suman porque el pórtico también los cuenta.
 */
fun calcularCruce(resumen: ResumenGeneral, porticos: List<ConteoPortico>): CrucePorticos {
    val lecturas = porticos.groupBy { it.accesoId }
    return CrucePorticos(resumen.porAcceso.map { acceso ->
        val delAcceso = lecturas[acceso.accesoId].orEmpty()
        FilaCruce(
            accesoId = acceso.accesoId,
            porticoEntradas = delAcceso.sumOf { it.entradas },
            porticoSalidas = delAcceso.sumOf { it.salidas },
            registradoEntradas = acceso.personasEntradas,
            registradoSalidas = acceso.personasSalidas
        )
    })
}
