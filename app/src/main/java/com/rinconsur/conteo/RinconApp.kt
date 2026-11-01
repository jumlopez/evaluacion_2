package com.rinconsur.conteo

import android.app.Application
import com.rinconsur.conteo.data.AppDatabase
import com.rinconsur.conteo.data.FlujoRepository

class RinconApp : Application() {
    private val baseDatos: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repositorio: FlujoRepository by lazy {
        FlujoRepository(baseDatos.registroDao(), baseDatos.conteoPorticoDao())
    }
}
