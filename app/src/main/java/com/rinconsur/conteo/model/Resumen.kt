package com.rinconsur.conteo.model

/** Proporción 0..1 de `parte` sobre `total`; 0 cuando no hay total. */
private fun proporcion(parte: Int, total: Int): Float = if (total == 0) 0f else parte.toFloat() / total

data class ResumenAcceso(
    val accesoId: Int,
    val personasEntradas: Int,
    val personasSalidas: Int,
    val compradores: Int,
    val entradasConDatoCompra: Int,
    val ninosEntradas: Int
) {
    /** Aforo neto del acceso: personas que entraron menos las que salieron. */
    val aforoNeto: Int get() = personasEntradas - personasSalidas

    /** Proporción (0..1) de las entradas con dato de compra que terminó comprando. */
    val pctCompra: Float get() = proporcion(compradores, entradasConDatoCompra)
}

data class ResumenGeneral(
    val porAcceso: List<ResumenAcceso>,
    val generoEntradas: Map<String, Int>,
    val edadEntradas: Map<String, Int>,
    val personasConNinos: Int
) {
    val totalPersonasEntradas: Int get() = porAcceso.sumOf { it.personasEntradas }
    val totalPersonasSalidas: Int get() = porAcceso.sumOf { it.personasSalidas }
    val aforoNeto: Int get() = totalPersonasEntradas - totalPersonasSalidas
    val totalCompradores: Int get() = porAcceso.sumOf { it.compradores }
    val totalConDatoCompra: Int get() = porAcceso.sumOf { it.entradasConDatoCompra }
    val totalNinos: Int get() = porAcceso.sumOf { it.ninosEntradas }

    /** Personas adultas que entraron (las que puede observar el operador de cámaras). */
    val totalAdultosEntradas: Int get() = (totalPersonasEntradas - totalNinos).coerceAtLeast(0)

    /** Conversión: compradores sobre entradas con dato de compra. */
    val pctCompra: Float get() = proporcion(totalCompradores, totalConDatoCompra)

    /** Proporción de personas adultas que entraron acompañadas por al menos un niño. */
    val pctConNinos: Float get() = proporcion(personasConNinos, totalAdultosEntradas)

    val sinDatos: Boolean get() = totalPersonasEntradas == 0 && totalPersonasSalidas == 0
}

/**
 * Calcula el resumen por acceso, la conversión de compra y la segmentación
 * (género y edad) de las entradas. Las personas incluyen a los niños acompañantes
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
            compradores = entradasAcceso.filter { it.compro == true }.sumOf { it.cantidad },
            entradasConDatoCompra = entradasAcceso.filter { it.compro != null }.sumOf { it.cantidad },
            ninosEntradas = entradasAcceso.sumOf { it.ninosAcompanantes }
        )
    }

    val porGenero = Genero.entries.associate { genero ->
        genero.etiqueta to entradas.filter { it.genero == genero.name }.sumOf { it.cantidad }
    }
    val porEdad = RangosEtarios.opciones.associateWith { rango ->
        entradas.filter { it.rangoEtario == rango }.sumOf { it.cantidad }
    }
    val personasConNinos = entradas.filter { it.ninosAcompanantes > 0 }.sumOf { it.cantidad }

    return ResumenGeneral(porAcceso, porGenero, porEdad, personasConNinos)
}
