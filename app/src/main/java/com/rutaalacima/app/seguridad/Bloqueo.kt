package com.rutaalacima.app.seguridad

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Cómo se rompe el sello de la frase del día (y se abre la app). */
enum class ModoBloqueo { BIOMETRIA, CLAVE, TOQUE }

/**
 * Bloqueo de la app con la frase del día:
 * - BIOMETRIA: rostro o huella del teléfono (o su PIN/patrón como respaldo). Lo hace el sistema
 *   Android con BiometricPrompt; la app nunca ve ni guarda la cara.
 * - CLAVE: una clave propia de 4 a 6 dígitos. Solo se guarda su hash PBKDF2 con sal aleatoria.
 * - TOQUE: basta con tocar el sello.
 */
object Bloqueo {
    private const val PREFS = "seguridad"
    private const val ITERACIONES = 120_000
    const val MAX_INTENTOS = 5
    const val ESPERA_MS = 30_000L

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Modo elegido; si nunca se eligió, rostro/huella cuando el teléfono lo permite. */
    fun modo(c: Context): ModoBloqueo {
        val guardado = prefs(c).getString("modo", null)?.let { runCatching { ModoBloqueo.valueOf(it) }.getOrNull() }
        return when {
            guardado == ModoBloqueo.CLAVE && !tieneClave(c) -> ModoBloqueo.TOQUE
            guardado != null -> guardado
            biometriaDisponible(c) -> ModoBloqueo.BIOMETRIA
            else -> ModoBloqueo.TOQUE
        }
    }

    fun elegir(c: Context, modo: ModoBloqueo) = prefs(c).edit().putString("modo", modo.name).apply()

    // ---------------------------------------------------------------- Rostro / huella

    fun biometriaDisponible(c: Context): Boolean =
        BiometricManager.from(c).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    /** Muestra el diálogo del sistema (rostro, huella o PIN del teléfono). */
    fun pedirBiometria(
        actividad: FragmentActivity,
        titulo: String,
        subtitulo: String,
        onExito: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            actividad, ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onExito()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelar no es un error: no se muestra mensaje
                    val cancelado = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED
                    onError(if (cancelado) "" else errString.toString())
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    /** Busca la FragmentActivity detrás de un Context de Compose. */
    fun actividad(c: Context): FragmentActivity? {
        var ctx = c
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    // ---------------------------------------------------------------- Clave propia

    fun tieneClave(c: Context): Boolean = prefs(c).getString("hash", null) != null

    fun guardarClave(c: Context, clave: String) {
        require(clave.length in 4..6 && clave.all { it.isDigit() })
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(c).edit()
            .putString("sal", sal.toHex())
            .putString("hash", hash(clave, sal).toHex())
            .putInt("fallos", 0).putLong("bloqueado_hasta", 0)
            .apply()
    }

    fun borrarClave(c: Context) = prefs(c).edit().remove("sal").remove("hash").apply()

    /** Milisegundos que faltan para poder intentar de nuevo (tras [MAX_INTENTOS] fallos). */
    fun esperaRestante(c: Context): Long = (prefs(c).getLong("bloqueado_hasta", 0) - System.currentTimeMillis()).coerceAtLeast(0)

    fun verificarClave(c: Context, clave: String): Boolean {
        if (esperaRestante(c) > 0) return false
        val p = prefs(c)
        val sal = p.getString("sal", null)?.fromHex() ?: return false
        val guardado = p.getString("hash", null) ?: return false
        val ok = java.security.MessageDigest.isEqual(hash(clave, sal), guardado.fromHex())
        if (ok) p.edit().putInt("fallos", 0).apply()
        else {
            val fallos = p.getInt("fallos", 0) + 1
            p.edit().putInt("fallos", if (fallos >= MAX_INTENTOS) 0 else fallos)
                .putLong("bloqueado_hasta", if (fallos >= MAX_INTENTOS) System.currentTimeMillis() + ESPERA_MS else 0)
                .apply()
        }
        return ok
    }

    private fun hash(clave: String, sal: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, 256)).encoded

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.fromHex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
