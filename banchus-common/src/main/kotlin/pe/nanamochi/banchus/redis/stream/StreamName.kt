package pe.nanamochi.banchus.redis.stream

import java.util.UUID

sealed class StreamName {
    abstract fun resolve(): String

    data class User(val sessionId: UUID) : StreamName() {
        override fun resolve(): String = "$BASE_KEY:user:$sessionId"
    }

    data object Main : StreamName() {
        override fun resolve(): String = "$BASE_KEY:main"
    }

    data object Lobby : StreamName() {
        override fun resolve(): String = "$BASE_KEY:lobby"
    }

    data object Donator : StreamName() {
        override fun resolve(): String = "$BASE_KEY:donator"
    }

    data object Staff : StreamName() {
        override fun resolve(): String = "$BASE_KEY:staff"
    }

    data object Developer : StreamName() {
        override fun resolve(): String = "$BASE_KEY:dev"
    }

    data class Channel(val name: String) : StreamName() {
        override fun resolve(): String = "$BASE_KEY:channel:$name"
    }

    data class Spectator(val hostSessionId: UUID) : StreamName() {
        override fun resolve(): String = "$BASE_KEY:spectator:$hostSessionId"
    }

    data class Multiplayer(val matchId: Long) : StreamName() {
        override fun resolve(): String = "$BASE_KEY:multiplayer:$matchId"
    }

    data class Multiplaying(val matchId: Long) : StreamName() {
        override fun resolve(): String = "$BASE_KEY:multiplaying:$matchId"
    }

    companion object {
        const val BASE_KEY = "banchus:streams"

        fun fromKey(key: String): StreamName? {
            val prefix = "$BASE_KEY:"
            if (!key.startsWith(prefix)) return null
            val suffix = key.removePrefix(prefix)

            return when {
                suffix == "main" -> Main
                suffix == "lobby" -> Lobby
                suffix == "donator" -> Donator
                suffix == "staff" -> Staff
                suffix == "dev" -> Developer
                suffix.startsWith("user:") -> {
                    val sessionIdStr = suffix.removePrefix("user:")
                    try {
                        User(UUID.fromString(sessionIdStr))
                    } catch (e: Exception) {
                        null
                    }
                }
                suffix.startsWith("channel:") -> {
                    val channelName = suffix.removePrefix("channel:")
                    Channel(channelName)
                }
                suffix.startsWith("spectator:") -> {
                    val hostSessionIdStr = suffix.removePrefix("spectator:")
                    try {
                        Spectator(UUID.fromString(hostSessionIdStr))
                    } catch (e: Exception) {
                        null
                    }
                }
                suffix.startsWith("multiplayer:") -> {
                    val matchIdStr = suffix.removePrefix("multiplayer:")
                    try {
                        Multiplayer(matchIdStr.toLong())
                    } catch (e: Exception) {
                        null
                    }
                }
                suffix.startsWith("multiplaying:") -> {
                    val matchIdStr = suffix.removePrefix("multiplaying:")
                    try {
                        Multiplaying(matchIdStr.toLong())
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> null
            }
        }
    }
}
