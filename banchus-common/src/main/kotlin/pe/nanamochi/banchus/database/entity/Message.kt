package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import pe.nanamochi.banchus.redis.entity.Session

@Entity
@Table(
    name = "messages",
    indexes = [Index(name = "idx_messages_unread", columnList = "target_id, deleted_at, read_at")],
)
@EntityListeners(AuditingEntityListener::class)
class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Int,
    @Column(name = "sender_id", nullable = false) var senderId: Int,
    @Column(name = "sender_name", length = 32, nullable = false) var senderName: String,
    @Column(name = "target_id", nullable = true) var targetId: Int?,
    @Column(name = "target_channel", length = 64, nullable = true) var targetChannel: String?,
    @Column(name = "content", length = 2048, nullable = false) var content: String,
    @Column(name = "read_at", nullable = true) var readAt: Instant?,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "deleted_at", nullable = true) var deletedAt: Instant?,
) {}

data class MessageSendResult(var message: Message, var response: String) {}

sealed class Target {
    data class Channel(val channelName: ChannelName) : Target()

    data class UserSessions(val sessions: List<Session>) : Target()

    data class OfflineUser(val username: String) : Target()

    data object Bot : Target()
}

data class TargetInfo(
    var targetChannel: ChannelName? = null,
    var targetId: Int? = null,
    var markAsUnread: Boolean = false,
) {}
