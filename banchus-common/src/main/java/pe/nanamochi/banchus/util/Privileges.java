package pe.nanamochi.banchus.util;

import pe.nanamochi.banchus.domain.enums.ClientPrivileges;
import pe.nanamochi.banchus.domain.enums.ServerPrivileges;

public class Privileges {
  private Privileges() {}

  public static int serverToClientPrivileges(int priv) {
    int ret = 0;
    if ((priv & ServerPrivileges.SUPPORTER.getValue()) != 0) {
      ret |= ClientPrivileges.SUPPORTER.getValue();
    }
    if ((priv & ServerPrivileges.CHAT_MODERATOR.getValue()) != 0) {
      ret |= ClientPrivileges.MODERATOR.getValue();
    }
    if ((priv & ServerPrivileges.SUPER_ADMIN.getValue()) != 0) {
      ret |= ClientPrivileges.OWNER.getValue();
    }
    return ret;
  }
}
