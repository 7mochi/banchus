package pe.nanamochi.banchus.services.gameplay.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CalculatorType {
  OSU_NATIVE("osu-native"),
  ROSU("rosu-pp");

  private final String alias;

  public static CalculatorType fromAlias(String alias) {
    for (CalculatorType type : values()) {
      if (type.alias.equalsIgnoreCase(alias)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown calculator type alias: " + alias);
  }
}
