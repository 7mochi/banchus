package pe.nanamochi.banchus.entities.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BeatmapRankedStatus {
  GRAVEYARD(-2),
  WIP(-1),
  PENDING(0),
  RANKED(1),
  APPROVED(2),
  QUALIFIED(3),
  LOVED(4);

  private final int value;

  public static BeatmapRankedStatus fromValue(int value) {
    for (BeatmapRankedStatus status : values()) {
      if (status.value == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown BeatmapRankedStatus value: " + value);
  }
}
