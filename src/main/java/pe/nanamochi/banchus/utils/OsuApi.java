package pe.nanamochi.banchus.utils;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pe.nanamochi.banchus.entities.osuapi.Beatmap;

@Component
public class OsuApi {
  private static final String BASE_URL = "https://osu.ppy.sh";
  private final RestTemplate restTemplate;
  private final String apiKey;

  public OsuApi(@Value("${banchus.osu-api.v1.key}") String apiKey) {
    this.apiKey = apiKey;
    this.restTemplate = new RestTemplate();
  }

  public byte[] getOsuFile(int beatmapId) {
    try {
      ResponseEntity<byte[]> response =
          new RestTemplate().getForEntity(BASE_URL + "/osu/" + beatmapId, byte[].class);
      return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    } catch (Exception e) {
      return null;
    }
  }

  public List<Beatmap> getBeatmaps(String beatmapMd5) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(BASE_URL + "/api/get_beatmaps")
            .queryParam("h", beatmapMd5)
            .queryParam("k", apiKey);

    ResponseEntity<List<Beatmap>> response =
        restTemplate.exchange(
            builder.toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    return response.getBody();
  }
}
