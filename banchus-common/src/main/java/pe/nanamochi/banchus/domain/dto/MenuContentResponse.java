package pe.nanamochi.banchus.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MenuContentResponse(List<MenuImage> images) {
  public record MenuImage(
      String image,
      String url,
      @JsonProperty("IsCurrent") boolean isCurrent,
      String begins,
      String expires) {}
}
