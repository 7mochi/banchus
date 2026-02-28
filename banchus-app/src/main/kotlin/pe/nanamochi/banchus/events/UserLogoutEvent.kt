package pe.nanamochi.banchus.events

import pe.nanamochi.banchus.database.entity.Session

data class UserLogoutEvent(val session: Session)
