package pe.nanamochi.banchus.entities.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MatchStatus {
  WAITING(0),
  PLAYING(1);

  private final int value;

  public static MatchStatus fromValue(int value) {
    for (MatchStatus matchStatus : values()) {
      if (matchStatus.value == value) {
        return matchStatus;
      }
    }
    throw new IllegalArgumentException("Unknown status value: " + value);
  }
}
