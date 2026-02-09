package pe.nanamochi.banchus.entities.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScoringType {
  SCORE(0),
  ACCURACY(1),
  COMBO(2),
  SCORE_V2(3);

  private final int value;

  public static ScoringType fromValue(int value) {
    for (ScoringType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    return SCORE;
  }
}
