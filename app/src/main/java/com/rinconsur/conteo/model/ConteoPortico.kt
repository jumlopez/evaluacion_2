package com.rinconsur.conteo.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lectura diaria del pórtico de un acceso. Los pórticos solo cuentan personas que
 * transitan: no distinguen si la persona entró o salió, por eso se guarda un único
 * total por acceso y día.
 */
@Entity(
    tableName = "conteos_portico",
    indices = [Index(value = ["accesoId", "fechaMillis"], unique = true)]
)
data class ConteoPortico(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accesoId: Int,
    val fechaMillis: Long,
    val personas: Int,
    val actualizadoEn: Long
)
