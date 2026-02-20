package pe.nanamochi.banchus.service.infra;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pe.nanamochi.banchus.domain.dto.OsuDirectQuery;
import pe.nanamochi.banchus.domain.enums.BeatmapDirectDisplayMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsuDirectApiClient {
  private final RestTemplate restTemplate;

  private static final String BASE_URL = "https://osu.direct/api";

  public Optional<String> search(
      String query, int mode, BeatmapDirectDisplayMode displayMode, int pageOffset) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(BASE_URL + "/v2/search")
            .queryParam("amount", 100)
            .queryParam("offset", pageOffset * 100);

    OsuDirectQuery osuDirectQuery = OsuDirectQuery.fromQuery(query);

    if (osuDirectQuery != null) {
      builder.queryParam("sort", osuDirectQuery.getSort());
    } else {
      builder.queryParam("q", query);
    }

    if (mode != -1) {
      builder.queryParam("mode", mode);
    }

    if (displayMode != BeatmapDirectDisplayMode.ALL) {
      builder.queryParam("status", displayMode.getApiStatus());
    }

    builder.queryParam("osudirect", "true");

    try {
      return Optional.ofNullable(restTemplate.getForObject(builder.toUriString(), String.class))
          .map(this::formatResponse);
    } catch (Exception e) {
      log.error("Error connecting to osu.direct API: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private String formatResponse(String result) {
    if (result == null || result.isBlank()) return result;

    String[] parts = result.split("\\R", 2);
    try {
      int length = Integer.parseInt(parts[0]);
      if (length == 100) {
        return "101\n" + (parts.length > 1 ? parts[1] : "");
      }
    } catch (NumberFormatException e) {
      log.warn("Unexpected response format from osu.direct (not a number)");
    }
    return result;
  }
}
