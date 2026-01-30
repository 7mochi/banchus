package pe.nanamochi.banchus.entities;

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
    throw new IllegalArgumentException("Unknown BeatmapWebRankedStatus value: " + value);
  }

  // TODO: Move this to a utility class like PrivilegesUtil
  public static int convertToWebStatus(BeatmapRankedStatus rankedStatus) {
    return switch (rankedStatus) {
      case GRAVEYARD, WIP, PENDING -> BeatmapWebRankedStatus.PENDING.getValue();
      case RANKED -> BeatmapWebRankedStatus.RANKED.getValue();
      case APPROVED -> BeatmapWebRankedStatus.APPROVED.getValue();
      case QUALIFIED -> BeatmapWebRankedStatus.QUALIFIED.getValue();
      case LOVED -> BeatmapWebRankedStatus.LOVED.getValue();
    };
  }
}
