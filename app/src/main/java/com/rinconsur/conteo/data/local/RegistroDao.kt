package com.rinconsur.conteo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rinconsur.conteo.model.RegistroFlujo
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {

    @Insert
    suspend fun insertarTodos(registros: List<RegistroFlujo>)

    @Query("SELECT * FROM registros_flujo WHERE fechaHora >= :desde ORDER BY fechaHora DESC")
    fun observarDesde(desde: Long): Flow<List<RegistroFlujo>>
}
