package pe.nanamochi.banchus.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BeatmapDirectDisplayMode {
  RANKED(0, 1),
  RANKED_STRICT(1, 1),
  PENDING(2, 0),
  QUALIFIED(3, 3),
  ALL(4, -1),
  GRAVEYARD(5, -2),
  APPROVED(6, 2),
  RANKED_PLAYED(7, 1),
  LOVED(8, 4);

  private final int value;
  private final int apiStatus;

  public static BeatmapDirectDisplayMode fromValue(int value) {
    for (BeatmapDirectDisplayMode mode : values()) {
      if (mode.value == value) return mode;
    }
    return ALL;
  }
}
