package com.rinconsur.conteo.model

/**
 * Comparación, por acceso, entre las personas que contó el pórtico y las que
 * registró el operador de cámaras.
 *
 * El pórtico solo informa cuántas personas transitaron, sin decir si entraron o
 * salieron, por lo que la comparación se hace sobre el total del acceso: las
 * entradas y salidas registradas se suman y se contrastan con ese total.
 */
data class FilaCruce(
    val accesoId: Int,
    val porticoPersonas: Int,
    val registradoEntradas: Int,
    val registradoSalidas: Int
) {
    /** Personas registradas en el acceso, sin separar entrada de salida. */
    val registradoTotal: Int get() = registradoEntradas + registradoSalidas

    /** Personas del pórtico que el operador no alcanzó a caracterizar. */
    val diferencia: Int get() = porticoPersonas - registradoTotal

    /** Proporción (0..1) del flujo del pórtico que quedó caracterizada. */
    val cobertura: Float?
        get() = if (porticoPersonas > 0) registradoTotal.toFloat() / porticoPersonas else null
}

data class CrucePorticos(val filas: List<FilaCruce>) {
    val totalPorticoPersonas: Int get() = filas.sumOf { it.porticoPersonas }
    val totalRegistradoEntradas: Int get() = filas.sumOf { it.registradoEntradas }
    val totalRegistradoSalidas: Int get() = filas.sumOf { it.registradoSalidas }
    val totalRegistrado: Int get() = totalRegistradoEntradas + totalRegistradoSalidas

    val hayDatosPortico: Boolean get() = filas.any { it.porticoPersonas > 0 }

    /** Diferencia total entre lo que marcaron los pórticos y lo registrado. */
    val diferencia: Int get() = totalPorticoPersonas - totalRegistrado

    val cobertura: Float?
        get() = if (totalPorticoPersonas > 0) {
            totalRegistrado.toFloat() / totalPorticoPersonas
        } else {
            null
        }

    companion object {
        fun vacio(): CrucePorticos = CrucePorticos(Accesos.ids.map { FilaCruce(it, 0, 0, 0) })
    }
}

/**
 * Cruza el conteo de pórticos con las personas registradas en la app, acceso por
 * acceso. Los niños acompañantes suman porque el pórtico también los cuenta.
 */
fun calcularCruce(
    registros: List<RegistroFlujo>,
    porticos: List<ConteoPortico>
): CrucePorticos {
    val resumen = calcularResumen(registros)
    val filas = resumen.porAcceso.map { acceso ->
        FilaCruce(
            accesoId = acceso.accesoId,
            porticoPersonas = porticos.filter { it.accesoId == acceso.accesoId }
                .sumOf { it.personas },
            registradoEntradas = acceso.personasEntradas,
            registradoSalidas = acceso.personasSalidas
        )
    }
    return CrucePorticos(filas)
}
