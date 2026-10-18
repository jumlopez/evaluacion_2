package com.rinconsur.conteo.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un registro de flujo: cuántas personas de un movimiento, género y rango etario
 * cruzaron un acceso. `ninosAcompanantes` cuenta niños adicionales al grupo y
 * `compro` solo se informa en las entradas (null en las salidas).
 */
@Entity(tableName = "registros_flujo")
data class RegistroFlujo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accesoId: Int,
    val fechaHora: Long,          // epoch en milisegundos
    val movimiento: String,       // Movimiento.name
    val genero: String,           // Genero.name
    val rangoEtario: String,
    val cantidad: Int,
    val ninosAcompanantes: Int = 0,
    val compro: Boolean? = null
)
