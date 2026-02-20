package pe.nanamochi.banchus.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BeatmapWebRankedStatus {
  NOT_SUBMITTED(-1),
  PENDING(0),
  UPDATE_AVAILABLE(1),
  RANKED(2),
  APPROVED(3),
  QUALIFIED(4),
  LOVED(5);

  private final int value;

  public static BeatmapWebRankedStatus fromValue(int value) {
    for (BeatmapWebRankedStatus status : values()) {
      if (status.value == value) {
        return status;
      }
    }
    return NOT_SUBMITTED;
  }

  public static int convertToWebStatus(BeatmapRankedStatus rankedStatus) {
    if (rankedStatus == null) return NOT_SUBMITTED.getValue();

    return switch (rankedStatus) {
      case GRAVEYARD, WIP, PENDING -> PENDING.getValue();
      case RANKED -> RANKED.getValue();
      case APPROVED -> APPROVED.getValue();
      case QUALIFIED -> QUALIFIED.getValue();
      case LOVED -> LOVED.getValue();
    };
  }
}
