package com.rinconsur.conteo.model

data class ResumenAcceso(
    val accesoId: Int,
    val personasEntradas: Int,
    val personasSalidas: Int,
    val compradores: Int,
    val salidasConDatoCompra: Int,
    val ninosEntradas: Int,
    val ninosSalidas: Int
) {
    /** Aforo neto del acceso: personas que entraron menos las que salieron. */
    val aforoNeto: Int get() = personasEntradas - personasSalidas

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
    val aforoNeto: Int get() = totalPersonasEntradas - totalPersonasSalidas
    val totalCompradores: Int get() = porAcceso.sumOf { it.compradores }
    val totalConDatoCompra: Int get() = porAcceso.sumOf { it.salidasConDatoCompra }
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
 * Calcula el resumen por acceso, la conversión de compra y la segmentación
 * (género y edad) de las entradas. La compra se registra en la salida, cuando ya se
 * sabe si la persona compró. Las personas incluyen a los niños acompañantes porque
 * el pórtico también los cuenta.
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
            salidasConDatoCompra = salidasAcceso.filter { it.compro != null }.sumOf { it.cantidad },
            ninosEntradas = entradasAcceso.sumOf { it.ninosAcompanantes },
            ninosSalidas = salidasAcceso.sumOf { it.ninosAcompanantes }
        )
    }

    val porGenero = Genero.entries.associate { genero ->
        genero.etiqueta to entradas.filter { it.genero == genero.name }.sumOf { it.cantidad }
    }
    // Se conservan visibles las etiquetas antiguas que todavía existan en la base.
    val etiquetasEdad = RangosEtarios.opciones +
        entradas.map { it.rangoEtario }
            .filter { it !in RangosEtarios.opciones }
            .distinct()
            .sorted()
    val porEdad = etiquetasEdad.associateWith { rango ->
        entradas.filter { it.rangoEtario == rango }.sumOf { it.cantidad }
    }
    val personasConNinos = entradas.filter { it.ninosAcompanantes > 0 }.sumOf { it.cantidad }

    return ResumenGeneral(porAcceso, porGenero, porEdad, personasConNinos)
}
