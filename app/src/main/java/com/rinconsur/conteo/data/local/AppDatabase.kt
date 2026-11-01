package com.rinconsur.conteo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.RegistroFlujo

@Database(entities = [RegistroFlujo::class, ConteoPortico::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun registroDao(): RegistroDao

    abstract fun conteoPorticoDao(): ConteoPorticoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        /**
         * Migra los registros del MVP anterior: los antiguos "COMPRA" pasan a ser
         * entradas con compra, y los "TRANSITO" entradas sin compra.
         */
        private val migracion1a2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS registros_flujo_nuevo (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accesoId INTEGER NOT NULL,
                        fechaHora INTEGER NOT NULL,
                        movimiento TEXT NOT NULL,
                        genero TEXT NOT NULL,
                        rangoEtario TEXT NOT NULL,
                        cantidad INTEGER NOT NULL,
                        ninosAcompanantes INTEGER NOT NULL,
                        compro INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO registros_flujo_nuevo
                        (id, accesoId, fechaHora, movimiento, genero, rangoEtario,
                         cantidad, ninosAcompanantes, compro)
                    SELECT id, accesoId, fechaHora, 'ENTRADA', 'NO_DECLARA', rangoEtario,
                           cantidad, 0, CASE WHEN tipo = 'COMPRA' THEN 1 ELSE 0 END
                    FROM registros_flujo
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE registros_flujo")
                db.execSQL("ALTER TABLE registros_flujo_nuevo RENAME TO registros_flujo")
            }
        }

        /** Agrega la tabla donde se anotan las lecturas diarias de los pórticos. */
        private val migracion2a3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conteos_portico` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accesoId` INTEGER NOT NULL,
                        `fechaMillis` INTEGER NOT NULL,
                        `entradas` INTEGER NOT NULL,
                        `salidas` INTEGER NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_conteos_portico_accesoId_fechaMillis` " +
                        "ON `conteos_portico` (`accesoId`, `fechaMillis`)"
                )
            }
        }

        /**
         * Los pórticos solo entregan personas que transitan, sin separar entrada de
         * salida: las lecturas antiguas se consolidan en un único total por acceso.
         */
        private val migracion3a4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conteos_portico_nuevo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accesoId` INTEGER NOT NULL,
                        `fechaMillis` INTEGER NOT NULL,
                        `personas` INTEGER NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `conteos_portico_nuevo`
                        (`id`, `accesoId`, `fechaMillis`, `personas`, `actualizadoEn`)
                    SELECT `id`, `accesoId`, `fechaMillis`, `entradas` + `salidas`, `actualizadoEn`
                    FROM `conteos_portico`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `conteos_portico`")
                db.execSQL("ALTER TABLE `conteos_portico_nuevo` RENAME TO `conteos_portico`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_conteos_portico_accesoId_fechaMillis` " +
                        "ON `conteos_portico` (`accesoId`, `fechaMillis`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rincon_flujo.db"
                ).addMigrations(migracion1a2, migracion2a3, migracion3a4)
                    .build()
                    .also { instancia = it }
            }
    }
}
