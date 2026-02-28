package pe.nanamochi.banchus.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object Security {
    private val secureRandom = SecureRandom()
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()

    fun generateToken(byteLength: Int): String =
        ByteArray(byteLength).also(secureRandom::nextBytes).let(urlEncoder::encodeToString)

    internal fun calculateMd5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

fun String.toMd5(): String = Security.calculateMd5(this.toByteArray())

fun ByteArray.toMd5(): String = Security.calculateMd5(this)
