package com.rinconsur.conteo.data.repository

import com.rinconsur.conteo.data.local.ConteoPorticoDao
import com.rinconsur.conteo.data.local.RegistroDao
import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.Periodo
import com.rinconsur.conteo.model.RegistroFlujo
import kotlinx.coroutines.flow.Flow

/**
 * Room/SQLite es el único almacén de la app: no hay backend ni sincronización.
 * Solo se guardan datos reales ingresados por el operador.
 */
class FlujoRepository(
    private val registroDao: RegistroDao,
    private val conteoPorticoDao: ConteoPorticoDao
) {

    fun observar(periodo: Periodo): Flow<List<RegistroFlujo>> =
        registroDao.observarDesde(periodo.desdeMillis())

    fun observarPorticos(periodo: Periodo): Flow<List<ConteoPortico>> =
        conteoPorticoDao.observarDesde(periodo.desdeMillis())

    suspend fun guardar(registro: RegistroFlujo): Long = registroDao.insertar(registro)

    suspend fun guardarTodos(registros: List<RegistroFlujo>) = registroDao.insertarTodos(registros)

    suspend fun porticosDelDia(dia: Long): List<ConteoPortico> = conteoPorticoDao.obtenerDia(dia)

    suspend fun guardarPortico(conteo: ConteoPortico): Long = conteoPorticoDao.guardar(conteo)

    suspend fun eliminarPortico(accesoId: Int, dia: Long) = conteoPorticoDao.eliminar(accesoId, dia)
}
