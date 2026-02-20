package pe.nanamochi.banchus.service.infra;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pe.nanamochi.banchus.domain.dto.OsuApiBeatmap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsuApiClient {
  private static final String BASE_URL = "https://osu.ppy.sh";

  private final RestTemplate restTemplate;

  @Value("${banchus.osu-api.v1.key}")
  private String apiKey;

  public Optional<byte[]> getOsuFile(int beatmapId) {
    try {
      ResponseEntity<byte[]> response =
          restTemplate.getForEntity(BASE_URL + "/osu/" + beatmapId, byte[].class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return Optional.of(response.getBody());
      }
    } catch (Exception e) {
      log.error("Error downloading .osu file for beatmap {}: {}", beatmapId, e.getMessage());
    }
    return Optional.empty();
  }

  public Optional<OsuApiBeatmap> getBeatmap(String beatmapMd5) {
    return callApi("h", beatmapMd5).stream().findFirst();
  }

  public Optional<OsuApiBeatmap> getBeatmap(int beatmapId) {
    return callApi("b", String.valueOf(beatmapId)).stream().findFirst();
  }

  public List<OsuApiBeatmap> getBeatmaps(int beatmapSetId) {
    return callApi("s", String.valueOf(beatmapSetId));
  }

  private List<OsuApiBeatmap> callApi(String paramName, String paramValue) {
    try {
      UriComponentsBuilder builder =
          UriComponentsBuilder.fromUriString(BASE_URL + "/api/get_beatmaps")
              .queryParam(paramName, paramValue)
              .queryParam("k", apiKey);

      ResponseEntity<List<OsuApiBeatmap>> response =
          restTemplate.exchange(
              builder.toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      List<OsuApiBeatmap> body = response.getBody();
      return (body != null) ? body : Collections.emptyList();

    } catch (Exception e) {
      log.error("Error calling osu!api ({}={}): {}", paramName, paramValue, e.getMessage());
      return Collections.emptyList();
    }
  }
}
