package com.rutaalacima.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        AgendaDiaEntity::class,
        MesEntity::class,
        MetaMensualEntity::class,
        PublicacionEntity::class,
        ComentarioLocalEntity::class,
        CoachMensajeEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class RutaDatabase : RoomDatabase() {
    abstract fun perfilDao(): PerfilDao
    abstract fun respuestaDao(): RespuestaDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun planificadorDao(): PlanificadorDao
    abstract fun agendaDao(): AgendaDao
    abstract fun planAnualDao(): PlanAnualDao
    abstract fun socialDao(): SocialDao
    abstract fun coachDao(): CoachDao
    abstract fun sincroniaDao(): SincroniaDao

    companion object {
        /** v4: calendario de vida (nacimiento y esperanza de vida) y propósitos a 5, 10, 15 o 20 años. */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE perfil ADD COLUMN anioNacimiento INTEGER")
                db.execSQL("ALTER TABLE perfil ADD COLUMN mesNacimiento INTEGER")
                db.execSQL("ALTER TABLE perfil ADD COLUMN esperanzaVida INTEGER")
                db.execSQL("ALTER TABLE propositos ADD COLUMN horizonte INTEGER NOT NULL DEFAULT 5")
            }
        }

        fun build(context: Context): RutaDatabase =
            Room.databaseBuilder(context, RutaDatabase::class.java, "rutacima.db")
                // Desde la v4 los datos se conservan al actualizar la app.
                .addMigrations(MIGRACION_3_4)
                // Solo para instalaciones de prueba muy antiguas (v1-v2).
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
    }
}
