package com.rutaalacima.app.seguridad

import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Aprobar SuRoute Web con el teléfono (rutacima://finanzas?i=<id>&k=<llave pública de la web>).
 * Tras la huella, el teléfono cifra solo para esa web (ECDH P-256 + AES-GCM) la llave de datos del
 * cifrado de extremo a extremo (o un "ok" si no hay cifrado) y la deja en fin_aprobaciones.
 */
object FinAprobacion {
    /** Pedido escaneado con la cámara: (id, llave pública de la web). */
    val pendiente = MutableStateFlow<Pair<String, String>?>(null)

    fun desdeEnlace(u: android.net.Uri): Pair<String, String>? {
        if (u.scheme != "rutacima" || u.host != "finanzas") return null
        val i = u.getQueryParameter("i")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{16,64}")) } ?: return null
        val k = u.getQueryParameter("k")?.takeIf { it.length in 80..100 } ?: return null
        return i to k
    }

    private fun deb64u(t: String) = Base64.decode(t, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun fijo(b: ByteArray): ByteArray { val s = if (b.size > 32) b.copyOfRange(b.size - 32, b.size) else b; return ByteArray(32 - s.size) + s }

    /** El mensaje cifrado para la web: { p: llave pública del teléfono, c: iv + texto cifrado }. */
    fun mensaje(llaveWeb: String, contenido: String): String {
        val gen = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }
        val mio = gen.generateKeyPair()
        val params = (mio.public as ECPublicKey).params
        val raw = deb64u(llaveWeb); require(raw.size == 65 && raw[0] == 4.toByte())
        val suya = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(BigInteger(1, raw.copyOfRange(1, 33)), BigInteger(1, raw.copyOfRange(33, 65))), params))
        val secreto = KeyAgreement.getInstance("ECDH").apply { init(mio.private); doPhase(suya, true) }.generateSecret()
        val llave = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secreto), "AES")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val ct = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, llave, GCMParameterSpec(128, iv)) }.doFinal(contenido.toByteArray())
        val w = (mio.public as ECPublicKey).w
        val pub = byteArrayOf(4) + fijo(w.affineX.toByteArray()) + fijo(w.affineY.toByteArray())
        return JSONObject().put("p", b64(pub)).put("c", b64(iv + ct)).toString()
    }

    /** Aprueba (después de la huella): envía la llave abierta o un "ok". */
    suspend fun aprobar(supa: com.rutaalacima.app.data.remote.SupabaseClient, id: String, llaveWeb: String) {
        val dek = FinCifrado.dek
        val contenido = if (dek != null) JSONObject().put("t", "dek").put("k", b64(dek.encoded)).toString() else JSONObject().put("t", "ok").toString()
        supa.insert("fin_aprobaciones", buildJsonObject { put("id", id); put("datos", mensaje(llaveWeb, contenido)) }, devolver = false)
    }
}
