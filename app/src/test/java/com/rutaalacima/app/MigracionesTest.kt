package com.rutaalacima.app

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rutaalacima.app.data.local.RutaDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Las actualizaciones de la app no pueden borrar la ruta de nadie: sin cuenta, el teléfono es el único
 * lugar donde vive. Aquí se arma una base con la forma exacta de cada versión anterior (a partir del
 * esquema actual, quitando lo que cada migración agregó), se llena con datos y se abre con Room:
 * Room corre las migraciones, valida que el esquema final sea el correcto y los datos deben seguir ahí.
 *
 * Al subir la versión de RutaDatabase: agrega la migración a MIGRACIONES y aquí, en [quitarHasta], lo que
 * la nueva versión agrega (para poder reconstruir la anterior).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MigracionesTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val VERSION_ACTUAL = 6
    private val MARCA = "marca-de-prueba"

    /** Lo que agregó cada versión: tablas nuevas y columnas nuevas (tabla → columnas). */
    private data class Cambios(val tablas: Set<String> = emptySet(), val columnas: Map<String, Set<String>> = emptyMap())
    private val agregadoEn = mapOf(
        4 to Cambios(columnas = mapOf("perfil" to setOf("anioNacimiento", "mesNacimiento", "esperanzaVida"), "propositos" to setOf("horizonte"))),
        5 to Cambios(tablas = setOf("vision_casillas")),
        6 to Cambios(columnas = mapOf("publicaciones" to setOf("brujula"))),
    )

    private lateinit var esquemaActual: List<Pair<String, String>> // (nombre de tabla, CREATE TABLE …)
    private lateinit var indicesActuales: List<Pair<String, String>> // (tabla, CREATE INDEX …)

    @Before fun leerEsquemaActual() {
        val db = RutaDatabase.build(ctx, "referencia.db")
        val s = db.openHelper.writableDatabase
        assertEquals("Subiste la versión de la base: agrega su caso en MigracionesTest", VERSION_ACTUAL, s.version)
        esquemaActual = s.query("SELECT name, sql FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' " +
            "AND name NOT IN ('android_metadata', 'room_master_table')").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
        }
        indicesActuales = s.query("SELECT tbl_name, sql FROM sqlite_master WHERE type = 'index' AND sql IS NOT NULL").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
        }
        db.close()
        assertTrue(esquemaActual.size >= 10)
    }

    @After fun limpiar() { ctx.databaseList().forEach { ctx.deleteDatabase(it) } }

    /** Quita de un CREATE TABLE la definición de una columna (`col` TIPO …). */
    private fun sinColumna(sql: String, col: String): String =
        sql.replace(Regex(""",\s*`$col`[^,)]*"""), "").also { check(it != sql) { "No se encontró la columna $col" } }

    /** Esquema de la versión [v]: el actual sin lo que agregaron las versiones posteriores. */
    private fun esquemaDe(v: Int): List<Pair<String, String>> {
        val posteriores = agregadoEn.filterKeys { it > v }.values
        val tablasFuera = posteriores.flatMap { it.tablas }.toSet()
        return esquemaActual.filter { it.first !in tablasFuera }.map { (t, sql) ->
            t to posteriores.flatMap { it.columnas[t].orEmpty() }.fold(sql) { acc, c -> sinColumna(acc, c) }
        }
    }

    /** Crea la base vieja y le pone una fila en cada tabla, con la marca en cada columna de texto. */
    private fun crearVieja(v: Int, nombre: String): List<String> {
        val ruta = ctx.getDatabasePath(nombre).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(ruta, null)
        val tablas = esquemaDe(v)
        tablas.forEach { db.execSQL(it.second) }
        val nombres = tablas.map { it.first }.toSet()
        indicesActuales.filter { it.first in nombres }.forEach { db.execSQL(it.second) }
        tablas.forEach { (t, _) ->
            val cols = db.rawQuery("PRAGMA table_info(`$t`)", null).use { c ->
                buildList { while (c.moveToNext()) add(c.getString(1) to c.getString(2).uppercase()) }
            }
            val valores = cols.map { (_, tipo) -> if (tipo.contains("TEXT")) "'$MARCA'" else if (tipo.contains("REAL")) "1.5" else "1" }
            db.execSQL("INSERT INTO `$t` (${cols.joinToString { "`${it.first}`" }}) VALUES (${valores.joinToString()})")
        }
        db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        db.execSQL("INSERT INTO room_master_table VALUES (42, 'version-$v')")
        db.version = v
        db.close()
        return tablas.map { it.first }
    }

    private fun migrarYRevisar(desde: Int) {
        val nombre = "v$desde.db"
        val tablas = crearVieja(desde, nombre)
        val db = RutaDatabase.build(ctx, nombre)
        val s = db.openHelper.writableDatabase // corre las migraciones y valida el esquema
        assertEquals(VERSION_ACTUAL, s.version)
        tablas.forEach { t ->
            s.query("SELECT COUNT(*) FROM `$t`").use { c -> c.moveToFirst(); assertEquals("La tabla $t perdió datos desde la v$desde", 1, c.getInt(0)) }
        }
        // Lo que ya estaba conserva su texto
        s.query("SELECT COUNT(*) FROM perfil WHERE nombre = '$MARCA'").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        // Las columnas nuevas quedan con su valor por defecto
        if (desde < 6) s.query("SELECT brujula FROM publicaciones").use { c -> c.moveToFirst(); assertEquals("", c.getString(0)) }
        if (desde < 4) s.query("SELECT horizonte FROM propositos").use { c -> c.moveToFirst(); assertEquals(5, c.getInt(0)) }
        db.close()
    }

    @Test fun desdeLaVersion5() = migrarYRevisar(5)
    @Test fun desdeLaVersion4() = migrarYRevisar(4)
    @Test fun desdeLaVersion3() = migrarYRevisar(3)

    @Test fun lasInstalacionesMuyAntiguasEmpiezanDeCeroSinCerrarse() {
        val ruta = ctx.getDatabasePath("v2.db").apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(ruta, null).apply { execSQL("CREATE TABLE vieja (x TEXT)"); version = 2; close() }
        val db = RutaDatabase.build(ctx, "v2.db")
        assertEquals(VERSION_ACTUAL, db.openHelper.writableDatabase.version)
        db.close()
    }
}
