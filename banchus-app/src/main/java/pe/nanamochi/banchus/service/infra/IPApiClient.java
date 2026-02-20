package pe.nanamochi.banchus.service.infra;

import java.net.InetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pe.nanamochi.banchus.domain.dto.Geolocation;

@Slf4j
@Service
@RequiredArgsConstructor
public class IPApiClient {
  private final RestTemplate restTemplate;

  public Geolocation fetchFromIP(InetAddress ip) {
    try {
      String url = "https://ip-api.com/json/" + ip.getHostAddress();
      Geolocation geo = restTemplate.getForObject(url, Geolocation.class);

      if (geo != null && "success".equals(geo.status())) {
        return geo;
      }

      log.warn("IP-API returned non-success status for IP: {}", ip.getHostAddress());
    } catch (Exception e) {
      log.error("Failed to fetch geolocation for IP: {}", ip.getHostAddress(), e);
    }

    return Geolocation.local();
  }
}
