package com.rinconsur.conteo.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lectura diaria de los pórticos de cada acceso. Se guarda una fila por acceso y día,
 * con las entradas y salidas que marca el pórtico.
 */
@Entity(
    tableName = "conteos_portico",
    indices = [Index(value = ["accesoId", "fechaMillis"], unique = true)]
)
data class ConteoPortico(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accesoId: Int,
    val fechaMillis: Long,
    val entradas: Int,
    val salidas: Int,
    val actualizadoEn: Long
)
