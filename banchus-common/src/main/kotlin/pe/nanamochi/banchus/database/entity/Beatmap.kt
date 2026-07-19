package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import org.hibernate.annotations.DynamicUpdate
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.domain.enums.Mode

@Entity
@DynamicUpdate
@Table(name = "beatmaps", indexes = [Index(name = "beatmaps_md5_idx", columnList = "md5")])
class Beatmap(
    @Id @Column(name = "id", nullable = false) var id: Int = 0,
    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    var mode: Mode = Mode.OSU,
    @Column(name = "md5", length = 32, nullable = false) var md5: String = "",
    @Column(name = "status", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    var status: BeatmapRankedStatus = BeatmapRankedStatus.PENDING,
    @Column(name = "version", length = 128, nullable = false) var version: String = "",
    @Column(name = "submission_date", nullable = false) var submissionDate: Instant = Instant.now(),
    @Column(name = "last_updated", nullable = false) var lastUpdated: Instant = Instant.now(),
    @Column(name = "playcount", nullable = false) var playcount: Long = 0L,
    @Column(name = "passcount", nullable = false) var passcount: Long = 0L,
    @Column(name = "total_length", nullable = false) var totalLength: Int = 0,
    @Column(name = "drain_length", nullable = false) var drainLength: Int = 0,
    @Column(name = "count_normal", nullable = false) var countNormal: Int = 0,
    @Column(name = "count_slider", nullable = false) var countSlider: Int = 0,
    @Column(name = "count_spinner", nullable = false) var countSpinner: Int = 0,
    @Column(name = "max_combo", nullable = false) var maxCombo: Int = 0,
    @Column(name = "bpm", nullable = false) var bpm: Double = 0.0,
    @Column(name = "cs", nullable = false) var cs: Double = 0.0,
    @Column(name = "ar", nullable = false) var ar: Double = 0.0,
    @Column(name = "od", nullable = false) var od: Double = 0.0,
    @Column(name = "hp", nullable = false) var hp: Double = 0.0,
    @Column(name = "star_rating", nullable = false) var starRating: Double = 0.0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatmapset_id", nullable = false)
    var beatmapset: Beatmapset? = null,
) {
    fun hasLeaderboard() = status == BeatmapRankedStatus.RANKED

    fun objectCount() = countNormal + countSlider + countSpinner
}
