package pe.nanamochi.banchus.database.entity

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
import pe.nanamochi.banchus.domain.enums.Mode

@Entity
@Table(name = "match_games")
class MatchGame(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: Match,
    @Column(name = "beatmap_id", nullable = false) var beatmapId: Int = 0,
    @Column(name = "mode") @Enumerated(EnumType.ORDINAL) var mode: Mode = Mode.OSU,
    @Column(name = "mods", nullable = false) var mods: Int = 0,
    @Column(name = "win_condition", nullable = false) var winCondition: Int = 0,
    @Column(name = "team_type", nullable = false) var teamType: Int = 0,
    @Column(name = "start_time", nullable = false) var startTime: Instant = Instant.now(),
    @Column(name = "end_time", nullable = true) var endTime: Instant? = null,
)
