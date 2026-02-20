package pe.nanamochi.banchus.util;

import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public final class Rijndael {

  private Rijndael() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
    return process(true, plaintext, key, iv);
  }

  public static byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws Exception {
    return process(false, ciphertext, key, iv);
  }

  private static byte[] process(boolean encrypt, byte[] data, byte[] key, byte[] iv)
      throws Exception {
    RijndaelEngine engine = new RijndaelEngine(256);

    PaddedBufferedBlockCipher cipher =
        new PaddedBufferedBlockCipher(CBCBlockCipher.newInstance(engine), new ZeroBytePadding());
    cipher.init(encrypt, new ParametersWithIV(new KeyParameter(key), iv));

    byte[] output = new byte[cipher.getOutputSize(data.length)];
    int len = cipher.processBytes(data, 0, data.length, output, 0);
    len += cipher.doFinal(output, len);

    byte[] result = new byte[len];
    System.arraycopy(output, 0, result, 0, len);
    return result;
  }
}
