package pe.nanamochi.banchus.core.util

import pe.nanamochi.banchus.core.enums.ClientPrivileges
import pe.nanamochi.banchus.core.enums.ServerPrivileges

fun Int.toClientPrivileges(): Int {
    var ret = 0

    if (this and ServerPrivileges.SUPPORTER.value != 0) {
        ret = ret or ClientPrivileges.SUPPORTER.value
    }

    if (this and ServerPrivileges.CHAT_MODERATOR.value != 0) {
        ret = ret or ClientPrivileges.MODERATOR.value
    }

    if (this and ServerPrivileges.SUPER_ADMIN.value != 0) {
        ret = ret or ClientPrivileges.OWNER.value
    }

    return ret
}
