package pe.nanamochi.banchus.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MatchType {
  STANDARD(0),
  POWERPLAY(1);

  private final int value;

  public static MatchType fromValue(int value) {
    for (MatchType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown match type value: " + value);
  }
}
