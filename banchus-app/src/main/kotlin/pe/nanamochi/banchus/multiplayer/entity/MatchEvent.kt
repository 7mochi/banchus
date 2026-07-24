package pe.nanamochi.banchus.multiplayer.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import pe.nanamochi.banchus.identity.entity.User

@Entity
@Table(name = "match_events")
class MatchEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: Match,
    @Column(name = "game_id", nullable = true) var gameId: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var eventType: MatchEventType,
    @Column(name = "timestamp", nullable = false) var timestamp: Instant = Instant.now(),
)

enum class MatchEventType {
    MATCH_CREATED,
    MATCH_DISBANDED,
    MATCH_USER_JOINED,
    MATCH_USER_LEFT,
    MATCH_HOST_ASSIGNMENT,
    MATCH_GAME_PLAYTHROUGH,
}
