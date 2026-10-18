package com.rinconsur.conteo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Declaramos el DataStore con nombre 'preferencias_rincon' (Guía 12)
val Context.dataStore by preferencesDataStore(name = "preferencias_rincon")

class ModoDataStore(private val context: Context) {

    private val MODO_ANALISIS = booleanPreferencesKey("modo_analisis_transito")

    suspend fun guardarEstado(valor: Boolean) {
        context.dataStore.edit { preferencias -> preferencias[MODO_ANALISIS] = valor }
    }

    fun obtenerEstado(): Flow<Boolean?> =
        context.dataStore.data.map { preferencias -> preferencias[MODO_ANALISIS] }
}
