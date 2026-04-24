package pe.nanamochi.banchus.redis.stream

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class MessageInfo(
    @JsonProperty("excluded_session_ids") val excludedSessionIds: List<UUID>? = null,
    @JsonProperty("read_privileges") val readPrivileges: Int? = null,
)
