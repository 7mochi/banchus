package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pe.nanamochi.banchus.domain.enums.Mode

@Entity
@Table(
    name = "stats",
    indexes = [Index(name = "idx_stats_user_mode", columnList = "user_id, mode")],
)
class Stat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User? = null,
    @Column(name = "mode") @Enumerated(EnumType.ORDINAL) var mode: Mode = Mode.OSU,
    @Column(name = "ranked_score", nullable = false) var rankedScore: Long = 0L,
    @Column(name = "total_score", nullable = false) var totalScore: Long = 0L,
    @Column(name = "play_count", nullable = false) var playCount: Int = 0,
    @Column(name = "replays_watched", nullable = false) var replaysWatched: Int = 0,
    @Column(name = "total_hits", nullable = false) var totalHits: Int = 0,
    @Column(name = "level", nullable = false) var level: Int = 0,
    @Column(name = "average_accuracy", nullable = false) var averageAccuracy: Double = 0.0,
    @Column(name = "performance_points", nullable = false) var performancePoints: Int = 0,
    @Column(name = "play_time", nullable = false) var playTime: Int = 0,
    @Column(name = "xh_count", nullable = false) var xhCount: Int = 0,
    @Column(name = "x_count", nullable = false) var xCount: Int = 0,
    @Column(name = "sh_count", nullable = false) var shCount: Int = 0,
    @Column(name = "s_count", nullable = false) var sCount: Int = 0,
    @Column(name = "a_count", nullable = false) var aCount: Int = 0,
    @Column(name = "b_count", nullable = false) var bCount: Int = 0,
    @Column(name = "c_count", nullable = false) var cCount: Int = 0,
    @Column(name = "d_count", nullable = false) var dCount: Int = 0,
    @Column(name = "max_combo", nullable = false) var maxCombo: Int = 0,
    @Column(name = "latest_performance_point_awarded") var latestPerformancePointAwarded: Int = 0,
) {}
