package pe.nanamochi.banchus.entities.commons;

import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mods {
  NO_MOD(0, "No Mod", "NM"),
  NO_FAIL(1, "No Fail", "NF"),
  EASY(1 << 1, "Easy", "EZ"),
  NO_VIDEO(1 << 2, "No Video", "NV"), // Replaced by "Toushscreen" in later versions
  HIDDEN(1 << 3, "Hidden", "HD"),
  HARD_ROCK(1 << 4, "Hard Rock", "HR"),
  SUDDEN_DEATH(1 << 5, "Sudden Death", "SD"),
  DOUBLE_TIME(1 << 6, "Double Time", "DT"),
  RELAX(1 << 7, "Relax", "RX"),
  HALF_TIME(1 << 8, "Half Time", "HT"),
  NIGHTCORE(1 << 9, "Nightcore", "NC"), // Used as "Taiko" mod in older versions
  FLASHLIGHT(1 << 10, "Flashlight", "FL"),
  AUTOPLAY(1 << 11, "Autoplay", "AU"),
  SPUN_OUT(1 << 12, "Spun Out", "SO"),
  AUTOPILOT(1 << 13, "Autopilot", "AP"),
  PERFECT(1 << 14, "Perfect", "PF"),
  KEY4(1 << 15, "4K", "4K"),
  KEY5(1 << 16, "5K", "5K"),
  KEY6(1 << 17, "6K", "6K"),
  KEY7(1 << 18, "7K", "7K"),
  KEY8(1 << 19, "8K", "8K"),
  FADE_IN(1 << 20, "Fade-In", "FI"),
  RANDOM(1 << 21, "Random", "RD"),
  CINEMA(1 << 22, "Cinema", "CN"),
  TARGET(1 << 23, "Target Practice", "TC"),
  KEY9(1 << 24, "9K", "9K"),
  KEY_COOP(1 << 25, "Co-Op", "CO"),
  KEY1(1 << 26, "1K", "1K"),
  KEY3(1 << 27, "3K", "3K"),
  KEY2(1 << 28, "2K", "2K"),
  SCORE_V2(1 << 29, "Score V2", "V2"),
  MIRROR(1 << 30, "Mirror", "MR"),
  ;

  public static final int SPEED_CHANGING =
      DOUBLE_TIME.getValue() | HALF_TIME.getValue() | NIGHTCORE.getValue();
  private static final int KEY_MODS_MASK =
      KEY1.value
          | KEY2.value
          | KEY3.value
          | KEY4.value
          | KEY5.value
          | KEY6.value
          | KEY7.value
          | KEY8.value
          | KEY9.value
          | KEY_COOP.value;
  public static final int OSU_SPECIFIC_MODS = SPUN_OUT.value | AUTOPILOT.value;
  private static final int MANIA_SPECIFIC_MODS =
      RANDOM.value | MIRROR.value | FADE_IN.value | KEY_MODS_MASK;

  private final int value;
  private final String displayName;
  private final String initial;

  public static List<Mods> fromBitmask(int bit) {
    if (bit == 0) return new ArrayList<>(List.of(NO_MOD));

    return Arrays.stream(values())
        .filter(mod -> mod != NO_MOD && (bit & mod.value) == mod.value)
        .collect(Collectors.toList());
  }

  public static List<Mods> fromInitials(String[] initials) {
    return Arrays.stream(values())
        .filter(
            mod -> {
              for (String initial : initials) {
                if (mod.getInitial().equalsIgnoreCase(initial)) {
                  return true;
                }
              }
              return false;
            })
        .collect(Collectors.toList());
  }

  public static int toBitmask(List<Mods> mods) {
    if (mods == null) return 0;
    return mods.stream().mapToInt(Mods::getValue).reduce(0, (a, b) -> a | b);
  }

  public static int filterInvalidModCombinations(int bitmask, Mode mode) {
    int result = bitmask;

    if ((result & (DOUBLE_TIME.value | NIGHTCORE.value)) == (DOUBLE_TIME.value | NIGHTCORE.value)) {
      result &= ~DOUBLE_TIME.value;
    }

    if (((result & (DOUBLE_TIME.value | NIGHTCORE.value)) != 0)
        && (result & HALF_TIME.value) != 0) {
      result &= ~HALF_TIME.value;
    }

    if ((result & EASY.value) != 0 && (result & HARD_ROCK.value) != 0) {
      result &= ~HARD_ROCK.value;
    }

    if ((result & (NO_FAIL.value | RELAX.value | AUTOPILOT.value)) != 0) {
      result &= ~SUDDEN_DEATH.value;
      result &= ~PERFECT.value;
    }

    if ((result & (RELAX.value | AUTOPILOT.value)) != 0) {
      result &= ~NO_FAIL.value;
    }

    if ((result & PERFECT.value) != 0 && (result & SUDDEN_DEATH.value) != 0) {
      result &= ~SUDDEN_DEATH.value;
    }

    if (mode.getValue() != Mode.OSU.getValue()) {
      result &= ~OSU_SPECIFIC_MODS;
    }

    if (mode.getValue() != Mode.MANIA.getValue()) {
      result &= ~MANIA_SPECIFIC_MODS;
    }

    if (mode.getValue() == Mode.OSU.getValue()) {
      if ((result & AUTOPILOT.value) != 0) {
        if ((result & (SPUN_OUT.value | RELAX.value)) != 0) {
          result &= ~AUTOPILOT.value;
        }
      }
    }

    if (mode.getValue() == Mode.MANIA.getValue()) {
      result &= ~RELAX.value;

      if ((result & HIDDEN.value) != 0 && (result & FADE_IN.value) != 0) {
        result &= ~FADE_IN.value;
      }
    }

    int keyModsUsed = result & KEY_MODS_MASK;
    if (Integer.bitCount(keyModsUsed) > 1) {
      for (Mods mod : Mods.values()) {
        if ((mod.value & KEY_MODS_MASK) != 0 && (keyModsUsed & mod.value) != 0) {
          result &= ~(keyModsUsed & ~mod.value);
          break;
        }
      }
    }

    return result;
  }

  public static List<Mods> filterInvalidModCombinations(List<Mods> mods, Mode mode) {
    int bitmask = toBitmask(mods);
    int cleanBitmask = filterInvalidModCombinations(bitmask, mode);
    return fromBitmask(cleanBitmask);
  }

  public static Mods getManiaKeyCount(List<Mods> mods) {
    if (mods.contains(KEY9)) {
      return KEY9;
    } else if (mods.contains(KEY8)) {
      return KEY8;
    } else if (mods.contains(KEY7)) {
      return KEY7;
    } else if (mods.contains(KEY6)) {
      return KEY6;
    } else if (mods.contains(KEY5)) {
      return KEY5;
    } else if (mods.contains(KEY4)) {
      return KEY4;
    } else if (mods.contains(KEY3)) {
      return KEY3;
    } else if (mods.contains(KEY2)) {
      return KEY2;
    }
    return KEY1;
  }
}
