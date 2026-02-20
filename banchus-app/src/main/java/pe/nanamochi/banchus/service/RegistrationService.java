package pe.nanamochi.banchus.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.domain.enums.CountryCode;
import pe.nanamochi.banchus.domain.enums.ServerPrivileges;
import pe.nanamochi.banchus.util.Security;
import pe.nanamochi.banchus.util.Validation;

@Service
@RequiredArgsConstructor
public class RegistrationService {
  private final UserService userService;
  private final StatService statService;

  @Transactional
  public Map<String, List<String>> registerUser(
      String username, String email, String passwordPlainText, int check) {
    Map<String, List<String>> errors = new HashMap<>();

    if (check == 0) {
      if (!Validation.isValidUsername(username)) {
        addError(errors, "username", "Invalid username.");
      }
      if (!Validation.isValidEmail(email)) {
        addError(errors, "user_email", "Invalid email syntax.");
      }
      if (!Validation.isValidPassword(passwordPlainText)) {
        addError(
            errors,
            "password",
            "Password must be between 8 and 32 characters and contain more than 3 unique"
                + " characters.");
      }

      userService
          .findByUsername(username)
          .ifPresent(
              u -> addError(errors, "username", "Username already taken by another player."));

      userService
          .findByEmail(email)
          .ifPresent(u -> addError(errors, "user_email", "Email already taken by another player."));

      if (!errors.isEmpty()) return errors;

      User createdUser =
          userService.create(
              User.builder()
                  .username(username)
                  .email(email)
                  .passwordMd5(Security.getMd5(passwordPlainText))
                  .country(CountryCode.KP) // TODO: Default to North Korea for now
                  .privileges(ServerPrivileges.UNRESTRICTED.getValue())
                  .build());
      statService.createAllGamemodes(createdUser);
    }

    return Map.of();
  }

  private void addError(Map<String, List<String>> errors, String field, String message) {
    errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
  }
}
