package pe.nanamochi.banchus.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class Security {

  private Security() {}

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

  public static String getMd5(final String plaintext) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      byte[] digest = m.digest(plaintext.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("CRITICAL: MD5 algorithm not found in JVM", e);
    }
  }

  public static String getMd5FromBytes(byte[] data) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      byte[] digest = m.digest(data);
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String generateToken(int byteLength) {
    byte[] bytes = new byte[byteLength];
    secureRandom.nextBytes(bytes);
    return urlEncoder.encodeToString(bytes);
  }
}
