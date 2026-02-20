package pe.nanamochi.banchus.controller.bancho;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.nanamochi.banchus.domain.dto.LoginResponse;
import pe.nanamochi.banchus.service.BanchoService;
import pe.nanamochi.banchus.service.LoginService;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class LoginController {
  private final LoginService loginService;
  private final BanchoService banchoService;

  @PostMapping(value = "/", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> banchoHandler(
      @RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body) {
    byte[] requestBody = (body != null) ? body : new byte[0];

    if (!headers.containsHeader("osu-token")) {
      return handleLoginRequest(headers, requestBody);
    }

    return handleBanchoRequest(headers.getFirst("osu-token"), requestBody);
  }

  private ResponseEntity<byte[]> handleLoginRequest(HttpHeaders headers, byte[] body) {
    String rawData = new String(body, StandardCharsets.UTF_8);
    LoginResponse loginResponse = loginService.handleLogin(rawData, headers);

    return ResponseEntity.ok()
        .header("cho-token", loginResponse.success() ? loginResponse.token() : "no")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(loginResponse.payload());
  }

  private ResponseEntity<byte[]> handleBanchoRequest(String token, byte[] body) {
    byte[] responsePackets = banchoService.handlePackets(token, body);

    return ResponseEntity.ok()
        .header("cho-token", token)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(responsePackets);
  }
}
