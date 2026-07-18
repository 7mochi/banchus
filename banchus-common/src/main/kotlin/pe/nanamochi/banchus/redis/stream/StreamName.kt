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
                    runCatching { UUID.fromString(sessionIdStr) }.getOrNull()?.let { User(it) }
                }
                suffix.startsWith("channel:") -> {
                    val channelName = suffix.removePrefix("channel:")
                    Channel(channelName)
                }
                suffix.startsWith("spectator:") -> {
                    val hostSessionIdStr = suffix.removePrefix("spectator:")
                    runCatching { UUID.fromString(hostSessionIdStr) }.getOrNull()?.let { Spectator(it) }
                }
                suffix.startsWith("multiplayer:") -> {
                    val matchIdStr = suffix.removePrefix("multiplayer:")
                    runCatching { matchIdStr.toLong() }.getOrNull()?.let { Multiplayer(it) }
                }
                suffix.startsWith("multiplaying:") -> {
                    val matchIdStr = suffix.removePrefix("multiplaying:")
                    runCatching { matchIdStr.toLong() }.getOrNull()?.let { Multiplaying(it) }
                }
                else -> null
            }
        }
    }
}
