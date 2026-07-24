package pe.nanamochi.banchus.core.util

import org.bouncycastle.crypto.engines.RijndaelEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.paddings.ZeroBytePadding
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV

object Rijndael {
    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        process(encrypt = true, data, key, iv)

    fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        process(encrypt = false, data, key, iv)

    private fun process(
        encrypt: Boolean,
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
    ): ByteArray {
        val engine = RijndaelEngine(256)
        val cipher =
            PaddedBufferedBlockCipher(CBCBlockCipher.newInstance(engine), ZeroBytePadding())

        cipher.init(encrypt, ParametersWithIV(KeyParameter(key), iv))

        val output = ByteArray(cipher.getOutputSize(data.size))
        val processedLen = cipher.processBytes(data, 0, data.size, output, 0)
        val finalLen = processedLen + cipher.doFinal(output, processedLen)

        return output.copyOfRange(0, finalLen)
    }
}
