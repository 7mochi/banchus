package pe.nanamochi.banchus.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SlotTeam {
  NEUTRAL(0),
  BLUE(1),
  RED(2);

  private final int value;

  public static SlotTeam fromValue(int value) {
    for (SlotTeam team : values()) {
      if (team.value == value) {
        return team;
      }
    }
    throw new IllegalArgumentException("Unknown team value: " + value);
  }
}
