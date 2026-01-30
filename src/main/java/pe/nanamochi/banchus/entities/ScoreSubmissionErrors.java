package pe.nanamochi.banchus.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScoreSubmissionErrors {
  HANDLE_PASSWORD_RESET("reset"),
  REQUIRE_VERIFICATION("verify"),
  NO_SUCH_USER("nouser"),
  NEEDS_AUTHENTICATION("pass"),
  ACCOUNT_INACTIVE("inactive"),
  ACCOUNT_BANNED("ban"),
  BEATMAP_UNRANKED("beatmap"),
  MODE_OR_MODS_DISABLED("disabled"),
  OLD_OSU_VERSION("oldver"),
  NO("no");

  private final String value;
}
