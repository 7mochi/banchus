package pe.nanamochi.banchus.auth.dto

import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.entity.Channel
import pe.nanamochi.banchus.core.entity.Presence
import pe.nanamochi.banchus.identity.entity.User

data class LoginResult(
    val session: Session,
    val user: User,
    val friends: List<Int>,
    val channels: List<Channel>,
    val presence: Presence,
    val allPresences: List<Presence>,
    val joinedSpecialChannels: List<String>,
)
