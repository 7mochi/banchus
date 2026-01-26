package pe.nanamochi.banchus.utils;

import pe.nanamochi.banchus.entities.ClientPrivileges;

public class PrivilegesUtil {
  private PrivilegesUtil() {}

  public static int serverToClientPrivileges(int value) {
    // TODO: an actual function implementing this
    return (ClientPrivileges.PLAYER.getValue()
        | ClientPrivileges.MODERATOR.getValue()
        | ClientPrivileges.SUPPORTER.getValue()
        | ClientPrivileges.OWNER.getValue()
        | ClientPrivileges.DEVELOPER.getValue()
        | ClientPrivileges.TOURNAMENT.getValue());
  }
}
