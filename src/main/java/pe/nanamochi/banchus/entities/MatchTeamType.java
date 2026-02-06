package pe.nanamochi.banchus.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MatchTeamType {
  HEAD_TO_HEAD(0),
  TAG_COOP(1),
  TEAM_VS(2),
  TAG_TEAM_VS(3);

  private final int value;

  public static MatchTeamType fromValue(int value) {
    for (MatchTeamType teamType : values()) {
      if (teamType.value == value) {
        return teamType;
      }
    }
    return HEAD_TO_HEAD;
  }
}
