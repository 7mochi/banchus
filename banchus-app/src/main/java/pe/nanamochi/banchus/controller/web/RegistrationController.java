package pe.nanamochi.banchus.controller.web;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.nanamochi.banchus.service.RegistrationService;

@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class RegistrationController {
  private final RegistrationService registrationService;

  @PostMapping(value = "/users")
  public ResponseEntity<?> registerAccount(
      @RequestParam("user[username]") String username,
      @RequestParam("user[user_email]") String email,
      @RequestParam("user[password]") String password,
      @RequestParam("check") int check) {
    log.debug(
        "Received registration request for username: {}, email: {}, check: {}",
        username,
        email,
        check);

    Map<String, List<String>> errors =
        registrationService.registerUser(username, email, password, check);

    if (!errors.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("form_error", Map.of("user", errors)));
    }

    return ResponseEntity.ok("ok");
  }
}
