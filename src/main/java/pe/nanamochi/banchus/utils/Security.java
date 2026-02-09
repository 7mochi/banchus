package pe.nanamochi.banchus.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Security {
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

  public static String getMd5(final String plaintext) throws NoSuchAlgorithmException {
    MessageDigest m = MessageDigest.getInstance("MD5");
    m.update(plaintext.getBytes(), 0, plaintext.length());
    StringBuilder sb = new StringBuilder();
    for (byte b : m.digest()) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static String generateToken(int byteLength) {
    byte[] bytes = new byte[byteLength];
    secureRandom.nextBytes(bytes);
    return urlEncoder.encodeToString(bytes);
  }
}
