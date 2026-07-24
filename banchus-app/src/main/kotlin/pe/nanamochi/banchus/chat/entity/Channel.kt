package pe.nanamochi.banchus.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import pe.nanamochi.banchus.core.StreamName

@Entity
@Table(name = "channels")
class Channel(
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @UuidGenerator
    @Column(name = "id", nullable = false, length = 36, updatable = false)
    var id: UUID? = null,
    @Column(name = "name", nullable = false, length = 96, unique = true) var name: String = "",
    @Column(name = "description", nullable = false, length = 256) var description: String = "",
    @Column(name = "read_privileges", nullable = false) var readPrivileges: Int = 0,
    @Column(name = "write_privileges", nullable = false) var writePrivileges: Int = 0,
    @Column(name = "status", nullable = false) var status: Boolean = false,
) {
    fun canRead(privileges: Int): Boolean =
        readPrivileges == 0 || (privileges and readPrivileges) != 0

    fun canWrite(privileges: Int): Boolean =
        writePrivileges == 0 || (privileges and writePrivileges) != 0

    companion object {
        fun spectator() =
            Channel(name = "#spectator", description = "Spectator channel", status = true)

        fun multiplayer() =
            Channel(name = "#multiplayer", description = "Multiplayer channel", status = false)
    }
}

sealed class ChannelName {
    abstract fun resolve(): String

    fun getMessageStream(): StreamName = StreamName.Channel(this.resolve())

    fun getUpdateStream(): StreamName {
        return when (this) {
            is Spectator -> StreamName.Spectator(this.sessionId)
            is Multiplayer -> StreamName.Multiplayer(this.matchId)
            is Chat -> {
                when (this.name) {
                    "#plus",
                    "#supporter",
                    "#premium" -> StreamName.Donator
                    "#staff" -> StreamName.Staff
                    "#devlog" -> StreamName.Developer
                    else -> StreamName.Main
                }
            }
        }
    }

    data class Spectator(val sessionId: UUID) : ChannelName() {
        override fun resolve(): String = "#spectator_$sessionId"
    }

    data class Multiplayer(val matchId: Long) : ChannelName() {
        override fun resolve(): String = "#multiplayer_$matchId"
    }

    data class Chat(val name: String) : ChannelName() {
        override fun resolve(): String = name
    }

    companion object {
        fun from(name: String): ChannelName {
            return when {
                name.startsWith("#spectator_") -> {
                    val idPart = name.removePrefix("#spectator_")
                    runCatching { UUID.fromString(idPart) }
                        .map { Spectator(it) }
                        .getOrElse { Chat(name) }
                }
                name.startsWith("#multiplayer_") -> {
                    val idPart = name.removePrefix("#multiplayer_")
                    idPart.toLongOrNull()?.let { Multiplayer(it) } ?: Chat(name)
                }
                else -> Chat(name)
            }
        }
    }
}
