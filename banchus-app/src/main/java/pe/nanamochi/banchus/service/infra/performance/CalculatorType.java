package pe.nanamochi.banchus.service.infra.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CalculatorType {
  OSU_NATIVE("osu-native"),
  ROSU("rosu-pp");

  private final String alias;

  public static CalculatorType fromAlias(String alias) {
    if (alias == null) return ROSU;

    for (CalculatorType type : values()) {
      if (type.alias.equalsIgnoreCase(alias)) {
        return type;
      }
    }
    return OSU_NATIVE;
  }
}
