package pe.nanamochi.banchus.entities;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SlotStatus {
  OPEN(1),
  LOCKED(1 << 1),
  NOT_READY(1 << 2),
  READY(1 << 3),
  NO_BEATMAP(1 << 4),
  PLAYING(1 << 5),
  COMPLETE(1 << 6),
  QUIT(1 << 7),
  HAS_PLAYER(
      NOT_READY.getValue()
          | READY.getValue()
          | NO_BEATMAP.getValue()
          | PLAYING.getValue()
          | COMPLETE.getValue()),
  CAN_START(NOT_READY.getValue() | READY.getValue()),
  WAITING_FOR_END(PLAYING.getValue() | COMPLETE.getValue());

  private final int value;

  public static SlotStatus fromValue(int value) {
    for (SlotStatus status : SlotStatus.values()) {
      if (status.getValue() == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid SlotStatus value: " + value);
  }

  public static List<SlotStatus> fromBitmask(int bitmask) {
    return Arrays.stream(SlotStatus.values()).filter(p -> (bitmask & p.getValue()) != 0).toList();
  }

  public static int toBitmask(List<SlotStatus> statuses) {
    int bitmask = 0;
    for (SlotStatus status : statuses) {
      bitmask |= status.getValue();
    }
    return bitmask;
  }
}
