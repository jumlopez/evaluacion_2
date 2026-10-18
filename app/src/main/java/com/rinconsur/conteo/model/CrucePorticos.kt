package com.rinconsur.conteo.model

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

    val coberturaEntradas: Float?
        get() = if (porticoEntradas > 0) registradoEntradas.toFloat() / porticoEntradas else null

    val coberturaSalidas: Float?
        get() = if (porticoSalidas > 0) registradoSalidas.toFloat() / porticoSalidas else null
}

data class CrucePorticos(val filas: List<FilaCruce>) {
    val totalPorticoEntradas: Int get() = filas.sumOf { it.porticoEntradas }
    val totalPorticoSalidas: Int get() = filas.sumOf { it.porticoSalidas }
    val totalRegistradoEntradas: Int get() = filas.sumOf { it.registradoEntradas }
    val totalRegistradoSalidas: Int get() = filas.sumOf { it.registradoSalidas }

    val hayDatosPortico: Boolean
        get() = filas.any { it.porticoEntradas > 0 || it.porticoSalidas > 0 }

    val coberturaEntradas: Float?
        get() = if (totalPorticoEntradas > 0) {
            totalRegistradoEntradas.toFloat() / totalPorticoEntradas
        } else {
            null
        }

    val coberturaSalidas: Float?
        get() = if (totalPorticoSalidas > 0) {
            totalRegistradoSalidas.toFloat() / totalPorticoSalidas
        } else {
            null
        }

    companion object {
        fun vacio(): CrucePorticos =
            CrucePorticos(Accesos.ids.map { FilaCruce(it, 0, 0, 0, 0) })
    }
}

/**
 * Cruza el conteo de pórticos con las personas registradas en la app. Los niños
 * acompañantes suman porque el pórtico también los cuenta.
 */
fun calcularCruce(
    registros: List<RegistroFlujo>,
    porticos: List<ConteoPortico>
): CrucePorticos {
    val resumen = calcularResumen(registros)
    val filas = resumen.porAcceso.map { acceso ->
        val lecturas = porticos.filter { it.accesoId == acceso.accesoId }
        FilaCruce(
            accesoId = acceso.accesoId,
            porticoEntradas = lecturas.sumOf { it.entradas },
            porticoSalidas = lecturas.sumOf { it.salidas },
            registradoEntradas = acceso.personasEntradas,
            registradoSalidas = acceso.personasSalidas
        )
    }
    return CrucePorticos(filas)
}
