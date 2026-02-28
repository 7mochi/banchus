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
import org.hibernate.annotations.DynamicUpdate
import pe.nanamochi.banchus.domain.enums.Mode

@Entity
@DynamicUpdate
@Table(name = "stats")
class Stat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User? = null,
    @Column(name = "gamemode") @Enumerated(EnumType.ORDINAL) var gamemode: Mode = Mode.OSU,
    @Column(name = "total_score", nullable = false) var totalScore: Long = 0L,
    @Column(name = "ranked_score", nullable = false) var rankedScore: Long = 0L,
    @Column(name = "performance_points", nullable = false) var performancePoints: Int = 0,
    @Column(name = "play_count", nullable = false) var playCount: Int = 0,
    @Column(name = "play_time", nullable = false) var playTime: Int = 0,
    @Column(name = "accuracy", nullable = false) var accuracy: Double = 0.0,
    @Column(name = "highest_combo", nullable = false) var highestCombo: Int = 0,
    @Column(name = "total_hits", nullable = false) var totalHits: Int = 0,
    @Column(name = "replay_views", nullable = false) var replayViews: Int = 0,
    @Column(name = "xh_count", nullable = false) var xhCount: Int = 0,
    @Column(name = "x_count", nullable = false) var xCount: Int = 0,
    @Column(name = "sh_count", nullable = false) var shCount: Int = 0,
    @Column(name = "s_count", nullable = false) var sCount: Int = 0,
    @Column(name = "a_count", nullable = false) var aCount: Int = 0,
) : Cloneable {
    public override fun clone(): Stat = super.clone() as Stat
}
