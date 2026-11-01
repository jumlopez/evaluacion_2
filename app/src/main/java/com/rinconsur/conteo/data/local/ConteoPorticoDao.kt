package com.rinconsur.conteo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rinconsur.conteo.model.ConteoPortico
import kotlinx.coroutines.flow.Flow

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
