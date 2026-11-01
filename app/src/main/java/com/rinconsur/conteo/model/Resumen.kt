package com.rinconsur.conteo.model

data class ResumenAcceso(
    val accesoId: Int,
    val personasEntradas: Int,
    val personasSalidas: Int,
    val compradores: Int,
    val noCompradores: Int,
    val salidasConDatoCompra: Int,
    val salidasSinDatoCompra: Int,
    val ninosEntradas: Int,
    val ninosSalidas: Int
) {
    /**
     * Aforo neto del acceso: personas que entraron menos las que salieron. Nunca es
     * negativo: si las salidas superan a las entradas el aforo se informa como 0.
     */
    val aforoNeto: Int get() = (personasEntradas - personasSalidas).coerceAtLeast(0)

    /** Proporción (0..1) de las salidas con dato de compra que efectivamente compró. */
    val pctCompra: Float
        get() = if (salidasConDatoCompra == 0) 0f else compradores.toFloat() / salidasConDatoCompra
}

data class ResumenGeneral(
    val porAcceso: List<ResumenAcceso>,
    val generoEntradas: Map<String, Int>,
    val edadEntradas: Map<String, Int>,
    val personasConNinos: Int
) {
    val totalPersonasEntradas: Int get() = porAcceso.sumOf { it.personasEntradas }
    val totalPersonasSalidas: Int get() = porAcceso.sumOf { it.personasSalidas }

    /** Aforo neto de la tienda; nunca negativo, igual que en cada acceso. */
    val aforoNeto: Int get() = (totalPersonasEntradas - totalPersonasSalidas).coerceAtLeast(0)
    val totalCompradores: Int get() = porAcceso.sumOf { it.compradores }
    val totalNoCompradores: Int get() = porAcceso.sumOf { it.noCompradores }
    val totalConDatoCompra: Int get() = porAcceso.sumOf { it.salidasConDatoCompra }
    val totalSinDatoCompra: Int get() = porAcceso.sumOf { it.salidasSinDatoCompra }
    val totalNinos: Int get() = porAcceso.sumOf { it.ninosEntradas }
    val totalNinosSalidas: Int get() = porAcceso.sumOf { it.ninosSalidas }

    /** Personas adultas que entraron (las que puede observar el operador de cámaras). */
    val totalAdultosEntradas: Int get() = (totalPersonasEntradas - totalNinos).coerceAtLeast(0)

    /** Conversión: compradores sobre salidas con dato de compra. */
    val pctCompra: Float
        get() = if (totalConDatoCompra == 0) 0f else totalCompradores.toFloat() / totalConDatoCompra

    /** Proporción de personas adultas que entraron acompañadas por al menos un niño. */
    val pctConNinos: Float
        get() = if (totalAdultosEntradas == 0) 0f else personasConNinos.toFloat() / totalAdultosEntradas

    val sinDatos: Boolean get() = totalPersonasEntradas == 0 && totalPersonasSalidas == 0
}

/**
 * Calcula el resumen por acceso, la conversión de compra y la segmentación (género y
 * característica etaria) de las entradas. La compra se registra en la salida, cuando
 * ya se sabe si la persona compró. Las personas incluyen a los niños acompañantes
 * porque el pórtico también los cuenta.
 */
fun calcularResumen(registros: List<RegistroFlujo>): ResumenGeneral {
    val entradas = registros.filter { it.movimiento == Movimiento.ENTRADA.name }
    val salidas = registros.filter { it.movimiento == Movimiento.SALIDA.name }

    val porAcceso = Accesos.ids.map { id ->
        val entradasAcceso = entradas.filter { it.accesoId == id }
        val salidasAcceso = salidas.filter { it.accesoId == id }
        ResumenAcceso(
            accesoId = id,
            personasEntradas = entradasAcceso.sumOf { it.cantidad + it.ninosAcompanantes },
            personasSalidas = salidasAcceso.sumOf { it.cantidad + it.ninosAcompanantes },
            compradores = salidasAcceso.filter { it.compro == true }.sumOf { it.cantidad },
            noCompradores = salidasAcceso.filter { it.compro == false }.sumOf { it.cantidad },
            salidasConDatoCompra = salidasAcceso.filter { it.compro != null }.sumOf { it.cantidad },
            salidasSinDatoCompra = salidasAcceso.filter { it.compro == null }.sumOf { it.cantidad },
            ninosEntradas = entradasAcceso.sumOf { it.ninosAcompanantes },
            ninosSalidas = salidasAcceso.sumOf { it.ninosAcompanantes }
        )
    }

    val porGenero = Genero.entries.associate { genero ->
        genero.etiqueta to entradas.filter { it.genero == genero.name }.sumOf { it.cantidad }
    }
    // Solo se agregan las tres características etarias: los rangos de edad de
    // versiones anteriores se convierten a su característica equivalente y los
    // registros sin característica ("No declara") no se segmentan.
    val porEdad = RangosEtarios.opciones.associateWith { caracteristica ->
        entradas
            .filter { RangosEtarios.normalizar(it.rangoEtario) == caracteristica }
            .sumOf { it.cantidad }
    }
    val personasConNinos = entradas.filter { it.ninosAcompanantes > 0 }.sumOf { it.cantidad }

    return ResumenGeneral(porAcceso, porGenero, porEdad, personasConNinos)
}

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
    val totalRegistrado: Int get() = filas.sumOf { it.registradoTotal }

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
