package pe.nanamochi.banchus.components;

import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mods {
  NO_MOD(0),
  NO_FAIL(1),
  EASY(1 << 1),
  TOUCHSCREEN(1 << 2),
  HIDDEN(1 << 3),
  HARD_ROCK(1 << 4),
  SUDDEN_DEATH(1 << 5),
  DOUBLE_TIME(1 << 6),
  RELAX(1 << 7),
  HALF_TIME(1 << 8),
  NIGHTCORE(1 << 9),
  FLASHLIGHT(1 << 10),
  AUTOPLAY(1 << 11),
  SPUN_OUT(1 << 12),
  AUTOPILOT(1 << 13),
  PERFECT(1 << 14),
  KEY4(1 << 15),
  KEY5(1 << 16),
  KEY6(1 << 17),
  KEY7(1 << 18),
  KEY8(1 << 19),
  FADE_IN(1 << 20),
  RANDOM(1 << 21),
  CINEMA(1 << 22),
  TARGET(1 << 23),
  KEY9(1 << 24),
  KEY_COOP(1 << 25),
  KEY1(1 << 26),
  KEY3(1 << 27),
  KEY2(1 << 28),
  SCORE_V2(1 << 29),
  MIRROR(1 << 30);

  private final int value;

  public static List<Mods> fromBitmask(int bit) {
    if (bit == 0) return new ArrayList<>(List.of(NO_MOD));

    return Arrays.stream(values())
        .filter(mod -> mod != NO_MOD && (bit & mod.value) == mod.value)
        .collect(Collectors.toList());
  }

  public static int toBitmask(List<Mods> mods) {
    if (mods == null) return 0;
    return mods.stream().mapToInt(Mods::getValue).reduce(0, (a, b) -> a | b);
  }
}
