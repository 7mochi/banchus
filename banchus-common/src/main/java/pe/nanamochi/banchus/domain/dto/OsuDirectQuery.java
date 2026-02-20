package pe.nanamochi.banchus.domain.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OsuDirectQuery {
  NEWEST("Newest", "ranked_date:desc"),
  TOP_RATED("Top Rated", "favourite_count:desc"),
  MOST_PLAYED("Most Played", "play_count:desc");

  private final String query;
  private final String sort;

  private static final Map<String, OsuDirectQuery> LOOKUP = new HashMap<>();

  static {
    for (OsuDirectQuery q : values()) {
      LOOKUP.put(q.query.toLowerCase(), q);
    }
  }

  public static OsuDirectQuery fromQuery(String query) {
    if (query == null) return null;
    return LOOKUP.get(query.trim().toLowerCase());
  }
}
