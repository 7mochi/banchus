package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.SubmissionStatus

@Entity
@Table(
    name = "scores",
    indexes =
        [
            Index(
                name = "score_user_mode_status_pp_idx",
                columnList = "user_id, mode, submission_status, performance_points DESC",
            ),
            Index(name = "beatmap_mode_status_idx", columnList = "beatmap_id"),
            Index(name = "beatmap_status_idx", columnList = "submission_status"),
        ],
)
@EntityListeners(AuditingEntityListener::class)
class Score(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,
    @Column(name = "online_checksum", length = 32, nullable = false)
    var onlineChecksum: String = "",
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatmap_id", nullable = false)
    var beatmap: Beatmap? = null,
    @Column(name = "score", nullable = false) var score: Long = 0L,
    @Column(name = "performance_points", nullable = false) var performancePoints: Double = 0.0,
    @Column(name = "accuracy", nullable = false) var accuracy: Double = 0.0,
    @Column(name = "highest_combo", nullable = false) var highestCombo: Int = 0,
    @Column(name = "full_combo", nullable = false) var fullCombo: Boolean = false,
    @Column(name = "mods", nullable = false) var mods: Int = 0,
    @Column(name = "num_300s", nullable = false) var num300s: Int = 0,
    @Column(name = "num_100s", nullable = false) var num100s: Int = 0,
    @Column(name = "num_50s", nullable = false) var num50s: Int = 0,
    @Column(name = "num_misses", nullable = false) var numMisses: Int = 0,
    @Column(name = "num_gekis", nullable = false) var numGekis: Int = 0,
    @Column(name = "num_katus", nullable = false) var numKatus: Int = 0,
    @Column(name = "grade", length = 2, nullable = false) var grade: String = "F",
    @Column(name = "submission_status", nullable = false)
    @Enumerated(EnumType.STRING)
    var submissionStatus: SubmissionStatus = SubmissionStatus.SUBMITTED,
    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    var mode: Mode = Mode.OSU,
    @Column(name = "passed", nullable = false) var passed: Boolean = false,
    @Column(name = "time_elapsed", nullable = false) var timeElapsed: Int = 0,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    @PreUpdate
    fun updateAccuracy() {
        this.accuracy = calculateAccuracy()
    }

    fun calculateAccuracy(): Double =
        when (mode) {
            Mode.OSU -> calculateOsuAccuracy()
            Mode.TAIKO -> calculateTaikoAccuracy()
            Mode.CATCH -> calculateCatchAccuracy()
            Mode.MANIA -> calculateManiaAccuracy()
        }

    private fun calculateOsuAccuracy(): Double {
        val totalNotes = num300s + num100s + num50s + numMisses
        if (totalNotes == 0) return 0.0

        val acc =
            (100.0 * (num300s * 300.0 + num100s * 100.0 + num50s * 50.0) / (totalNotes * 300.0))
        return clampAccuracy(acc)
    }

    private fun calculateTaikoAccuracy(): Double {
        val totalNotes = num300s + num100s + numMisses
        if (totalNotes == 0) return 0.0

        val acc = (100.0 * (num100s * 0.5 + num300s) / totalNotes)
        return clampAccuracy(acc)
    }

    private fun calculateCatchAccuracy(): Double {
        val totalNotes = num300s + num100s + num50s + numKatus + numMisses
        if (totalNotes == 0) return 0.0

        val acc = (100.0 * (num300s + num100s + num50s) / totalNotes)
        return clampAccuracy(acc)
    }

    private fun calculateManiaAccuracy(): Double {
        val totalNotes = num300s + num100s + num50s + numGekis + numKatus + numMisses
        if (totalNotes == 0) return 0.0

        val acc =
            (100.0 *
                (num50s * 50.0 +
                    num100s * 100.0 +
                    numKatus * 200.0 +
                    (num300s + numGekis) * 300.0) / (totalNotes * 300.0))
        return clampAccuracy(acc)
    }

    private fun clampAccuracy(value: Double): Double = min(100.0, max(0.0, value))
}
