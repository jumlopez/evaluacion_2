package com.rinconsur.conteo

import android.app.Application
import com.rinconsur.conteo.data.local.AppDatabase
import com.rinconsur.conteo.data.repository.FlujoRepository

class RinconApp : Application() {
    private val baseDatos: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repositorio: FlujoRepository by lazy {
        FlujoRepository(baseDatos.registroDao(), baseDatos.conteoPorticoDao())
    }
}
