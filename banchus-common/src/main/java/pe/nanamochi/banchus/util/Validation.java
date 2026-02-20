package pe.nanamochi.banchus.util;

import java.util.regex.Pattern;

public final class Validation {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\w \\[\\]-]{2,15}$");
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile(
          "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
              + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");

  private Validation() {}

  public static boolean isValidUsername(String username) {
    if (username == null || username.isBlank()) {
      return false;
    }
    if (username.contains(" ") && username.contains("_")) {
      return false;
    }
    return USERNAME_PATTERN.matcher(username).matches();
  }

  public static boolean isValidEmail(String email) {
    if (email == null || email.isBlank()) {
      return false;
    }
    return EMAIL_PATTERN.matcher(email).matches();
  }

  public static boolean isValidPassword(String password) {
    if (password == null) {
      return false;
    }
    if (password.length() < 8 || password.length() > 32) {
      return false;
    }

    long uniqueChars = password.chars().distinct().count();
    return uniqueChars > 3;
  }
}
