package pe.nanamochi.banchus.entities.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LeaderboardType {
  LOCAL(0),
  GLOBAL(1),
  MODS(2),
  FRIENDS(3),
  COUNTRY(4);

  private final int value;

  public static LeaderboardType fromValue(int value) {
    for (LeaderboardType type : LeaderboardType.values()) {
      if (type.getValue() == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid LeaderboardType value: " + value);
  }
}
