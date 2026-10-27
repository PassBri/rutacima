package com.rutaalacima.app.seguridad

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado de extremo a extremo del Ascenso financiero (igual que la web, web/js/35-fin-cifrado.js).
 * - Una llave de datos AES-GCM de 256 bits cifra cada respuesta "fin#…" (menos los ajustes).
 *   Valor guardado: "e1:" + base64(iv de 12 bytes + texto cifrado con su etiqueta).
 * - En la nube solo va la llave envuelta con el código de recuperación de 12 palabras (fin#llave):
 *   { v: 1, s: sal, r: base64(iv + llave cifrada) } con PBKDF2-SHA256 de 200.000 vueltas.
 * - En el teléfono la llave queda envuelta con una llave del Android Keystore (hardware) y se abre
 *   con la huella o el rostro. Al cerrar el módulo se olvida de la memoria.
 */
object FinCifrado {
    val PALABRAS = "agua aire alba alce alma alto amigo ancla anillo arbol arco arena arroz astro atlas aura avena ave azul bahia balsa banco barco barro base baya bello beso bosque bota brasa brazo brisa brote buho cabra cacao cafe caja cala calma cama camino campo canoa canto capa cara carta casa cedro cerro cielo cima cinta circo ciudad clave clima cobre cofre cola color cometa concha copa coral cuarzo cuerda cueva cumbre dalia danza dedo delta diente disco dulce duna eco eje elfo enero espiga estrella faro fiesta flor foco fruta fuego fuente gacela gaita gallo garza gato globo gota grano grillo gruta guante haba hada hielo hierba higo hoja horno huella huerto idea isla jade jarra jazmin joya juego junco lago lana lapiz laurel leche lento leon libro lima limon lince lino llama llave lluvia lobo loma luna luz madera maiz mango manto mapa mar marea mesa miel mina mirlo monte mora mundo musgo nido niebla nieve noche norte nube nuez ola oliva onda oro oso pacto paja palma pan papel pasto paz pecho perla pez piedra pino pista plata playa pluma polen pomelo poncho prado puente puerto pulpo puma queso radio raiz rama rana rayo red reloj rio roble roca rosa rueda ruta sabana sal salto selva semilla senda sierra sol sombra sonido suelo sur taza techo tejo tela tierra tigre tinta toro torre trigo trueno tucan uva valle vapor vela verano viento vino violin volcan yegua yerba yeso zorro abeja acero alero arpa asado bambu bisonte bolsa borde cancion cascada castor cisne cobija colina".split(" ")
    private val META = setOf("fin#pin", "fin#bloqueo", "fin#ocultar", "fin#cierre", "fin#visibilidad", "fin#moneda", "fin#llave", "fin#cifrado")
    private const val PREFS = "fin_cifrado"
    private const val ALIAS = "suroute_fin_envoltura"
    private val azar = SecureRandom()

    @Volatile var dek: SecretKey? = null; private set
    /** Cambia cada vez que se abre o se cierra la llave: las pantallas vuelven a leer los datos. */
    val version = MutableStateFlow(0)
    private val claro = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun cifrable(clave: String) = clave.startsWith("fin#") && clave !in META
    fun abierta() = dek != null
    fun cerrar() { dek = null; claro.clear(); version.value++ }

    fun normalizar(t: String): String = Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z\\s]"), " ").trim().split(Regex("\\s+")).joinToString(" ")
    fun nuevoCodigo(): String = ByteArray(12).also { azar.nextBytes(it) }.joinToString(" ") { PALABRAS[it.toInt() and 0xFF] }

    // ---------------------------------------------------------------- Primitivas (mismo formato que la web)
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun deb64(t: String) = Base64.decode(t, Base64.NO_WRAP)
    private fun kek(texto: String, sal: ByteArray, vueltas: Int): SecretKey =
        SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(texto.toCharArray(), sal, vueltas, 256)).encoded, "AES")
    private fun cifrar(llave: SecretKey, datos: ByteArray): String {
        val iv = ByteArray(12).also { azar.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, llave, GCMParameterSpec(128, iv)) }
        return b64(iv + c.doFinal(datos))
    }
    private fun descifrar(llave: SecretKey, t: String): ByteArray {
        val b = deb64(t)
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, llave, GCMParameterSpec(128, b, 0, 12)) }.doFinal(b, 12, b.size - 12)
    }

    fun cifrarValor(texto: String): String = "e1:" + cifrar(dek ?: error("cerrada"), texto.toByteArray(Charsets.UTF_8))
    /** El texto claro de un valor guardado: si está cifrado y la llave está cerrada, nada. */
    fun leer(clave: String, v: String): String {
        if (!v.startsWith("e1:") || !cifrable(clave)) return v
        claro[v]?.let { return it }
        val llave = dek ?: return ""
        return runCatching { String(descifrar(llave, v.substring(3)), Charsets.UTF_8) }.getOrDefault("").also { claro[v] = it }
    }

    // ---------------------------------------------------------------- La llave en el teléfono (Android Keystore)
    private fun envoltura(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    private fun guardarLocal(ctx: Context, raw: ByteArray) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, envoltura()) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("dek", b64(c.iv + c.doFinal(raw))).apply()
    }
    fun hayLlaveLocal(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("dek")
    fun borrarLocal(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("dek").apply()

    /** Tras la huella o el rostro: abre la llave guardada en este teléfono. false si aún no la tiene. */
    fun abrirConTelefono(ctx: Context): Boolean {
        val t = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("dek", null) ?: return false
        return runCatching {
            val b = deb64(t)
            val raw = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, envoltura(), GCMParameterSpec(128, b, 0, 12)) }.doFinal(b, 12, b.size - 12)
            dek = SecretKeySpec(raw, "AES"); claro.clear(); version.value++; true
        }.getOrDefault(false)
    }

    /** En un teléfono nuevo: abre con el código de recuperación y deja la llave guardada aquí. */
    fun abrirConCodigo(ctx: Context, codigo: String, llaveJson: String): Boolean = runCatching {
        val j = JSONObject(llaveJson)
        val raw = descifrar(kek(normalizar(codigo), deb64(j.getString("s")), 200_000), j.getString("r"))
        dek = SecretKeySpec(raw, "AES"); guardarLocal(ctx, raw); claro.clear(); version.value++; true
    }.getOrDefault(false)

    /** Crea la llave y devuelve el valor de fin#llave (envuelta con el código). */
    fun crear(ctx: Context, codigo: String): String {
        val raw = ByteArray(32).also { azar.nextBytes(it) }; val sal = ByteArray(16).also { azar.nextBytes(it) }
        dek = SecretKeySpec(raw, "AES"); guardarLocal(ctx, raw); claro.clear()
        return JSONObject().put("v", 1).put("s", b64(sal)).put("r", cifrar(kek(normalizar(codigo), sal, 200_000), raw)).toString()
    }
    fun avisarCambio() { version.value++ }
}
