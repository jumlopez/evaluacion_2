package com.rinconsur.conteo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.Periodo
import com.rinconsur.conteo.model.RegistroFlujo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface RegistroDao {

    @Insert
    suspend fun insertarTodos(registros: List<RegistroFlujo>)

    @Query("SELECT * FROM registros_flujo WHERE fechaHora >= :desde ORDER BY fechaHora DESC")
    fun observarDesde(desde: Long): Flow<List<RegistroFlujo>>
}

@Dao
interface ConteoPorticoDao {

    @Query("SELECT * FROM conteos_portico WHERE fechaMillis >= :desde ORDER BY fechaMillis DESC, accesoId")
    fun observarDesde(desde: Long): Flow<List<ConteoPortico>>

    @Query("SELECT * FROM conteos_portico WHERE fechaMillis = :dia ORDER BY accesoId")
    suspend fun obtenerDia(dia: Long): List<ConteoPortico>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(conteo: ConteoPortico): Long

    @Query("DELETE FROM conteos_portico WHERE accesoId = :accesoId AND fechaMillis = :dia")
    suspend fun eliminar(accesoId: Int, dia: Long)
}

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

    suspend fun guardarTodos(registros: List<RegistroFlujo>) = registroDao.insertarTodos(registros)

    suspend fun porticosDelDia(dia: Long): List<ConteoPortico> = conteoPorticoDao.obtenerDia(dia)

    suspend fun guardarPortico(conteo: ConteoPortico): Long = conteoPorticoDao.guardar(conteo)

    suspend fun eliminarPortico(accesoId: Int, dia: Long) = conteoPorticoDao.eliminar(accesoId, dia)
}

// DataStore con nombre 'preferencias_rincon' (Guía 12). La clave persistida no cambia.
private val Context.dataStore by preferencesDataStore(name = "preferencias_rincon")

private val CLAVE_MODO_ANALISIS = booleanPreferencesKey("modo_analisis_transito")

class ModoDataStore(private val context: Context) {

    suspend fun guardarEstado(valor: Boolean) {
        context.dataStore.edit { preferencias -> preferencias[CLAVE_MODO_ANALISIS] = valor }
    }

    fun obtenerEstado(): Flow<Boolean?> =
        context.dataStore.data.map { preferencias -> preferencias[CLAVE_MODO_ANALISIS] }
}

@Database(entities = [RegistroFlujo::class, ConteoPortico::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun registroDao(): RegistroDao

    abstract fun conteoPorticoDao(): ConteoPorticoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rincon_flujo.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
