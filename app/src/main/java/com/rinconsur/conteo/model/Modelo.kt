package com.rinconsur.conteo.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar

/** Movimiento observado en un acceso: la tienda necesita entradas y salidas. */
enum class Movimiento(val etiqueta: String) { ENTRADA("Entrada"), SALIDA("Salida") }

/** Género declarado u observado. OTRO y NO_DECLARA solo existen en registros históricos. */
enum class Genero(val etiqueta: String) {
    FEMENINO("Femenino"),
    MASCULINO("Masculino"),
    OTRO("Otro"),
    NO_DECLARA("No declara")
}

/** Géneros que el operador puede contar en la captura rápida. */
val generosCapturables = listOf(Genero.FEMENINO, Genero.MASCULINO)

/** Características etarias que el operador elige al contar cada persona. */
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

    /** Lleva cualquier valor guardado a una característica actual (null si no se puede). */
    fun normalizar(valor: String): String? = if (valor in opciones) valor else equivalencias[valor]
}

/** La tienda tiene cuatro accesos: 1, 2 y 3 en el primer piso; 4 en el segundo piso. */
object Accesos {
    private val pisos = mapOf(1 to 1, 2 to 1, 3 to 1, 4 to 2)

    val ids: List<Int> = pisos.keys.toList()

    fun nombre(id: Int) = "Acceso $id"

    /** Etiqueta completa, por ejemplo "Acceso 3 · Piso 1". */
    fun etiqueta(id: Int) = pisos[id]?.let { "${nombre(id)} · Piso $it" } ?: nombre(id)

    /** Etiqueta corta para los chips del formulario. */
    fun etiquetaCorta(id: Int) = pisos[id]?.let { "Acceso $id · P$it" } ?: nombre(id)
}

/**
 * Un registro de flujo: cuántas personas de un movimiento, género y característica
 * etaria cruzaron un acceso. `ninosAcompanantes` cuenta niños adicionales al grupo y
 * `compro` solo se informa en las salidas (null en las entradas).
 */
@Entity(tableName = "registros_flujo")
data class RegistroFlujo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accesoId: Int,
    val fechaHora: Long,
    val movimiento: String,
    val genero: String,
    val rangoEtario: String,
    val cantidad: Int,
    val ninosAcompanantes: Int = 0,
    val compro: Boolean? = null
)

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

const val DIA_MS = 24L * 60 * 60 * 1000

/** Periodos de consulta histórica del dashboard. */
enum class Periodo(val etiqueta: String) {
    HOY("Hoy"),
    SIETE_DIAS("7 días"),
    TODO("Todo");

    fun desdeMillis(ahora: Long = System.currentTimeMillis()): Long = when (this) {
        HOY -> inicioDelDia(ahora)
        SIETE_DIAS -> inicioDelDia(ahora) - 6 * DIA_MS
        TODO -> 0L
    }
}

fun inicioDelDia(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
