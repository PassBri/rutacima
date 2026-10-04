package com.rutaalacima.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PerfilEntity::class,
        RespuestaEntity::class,
        EvaluacionEjesEntity::class,
        ChecklistDiarioEntity::class,
        PropositoEntity::class,
        AccionEntity::class,
        MetaAnualEntity::class,
        BalanceAnualEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RutaDatabase : RoomDatabase() {
    abstract fun perfilDao(): PerfilDao
    abstract fun respuestaDao(): RespuestaDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun planificadorDao(): PlanificadorDao

    companion object {
        fun build(context: Context): RutaDatabase =
            Room.databaseBuilder(context, RutaDatabase::class.java, "ruta_a_la_cima.db")
                // Mientras la app está en versión 0.x: si cambia el esquema se recrea la base.
                // Antes de publicar, reemplazar por migraciones reales.
                .fallbackToDestructiveMigration()
                .build()
    }
}
