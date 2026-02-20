package pe.nanamochi.banchus.components;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mode {
  OSU(0),
  TAIKO(1),
  CATCH(2),
  MANIA(3);

  private final int value;

  public static Mode fromValue(int value) {
    for (Mode mode : values()) {
      if (mode.value == value) {
        return mode;
      }
    }
    return OSU;
  }
}
