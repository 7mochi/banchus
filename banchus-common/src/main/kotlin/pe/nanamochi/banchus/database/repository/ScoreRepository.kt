package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.SubmissionStatus

@Repository
interface ScoreRepository : JpaRepository<Score, Int> {
    @Query(
        """
        SELECT s
        FROM Score s
        JOIN s.user u
        WHERE s.beatmap = :beatmap
          AND (:mode IS NULL OR s.mode = :mode)
          AND (:mods IS NULL OR s.mods = :mods)
          AND (:country IS NULL OR u.country = :country)
          AND s.submissionStatus = :status
          AND u.privileges >= 1
        ORDER BY s.score DESC
        """
    )
    fun fetchBeatmapLeaderboard(
        beatmap: Beatmap,
        mode: Mode?,
        mods: Int?,
        status: SubmissionStatus,
        country: CountryCode?,
    ): List<Score>

    fun findFirstByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
        beatmap: Beatmap,
        user: User,
        submissionStatus: SubmissionStatus,
    ): Score?

    fun findTop100ByUserAndModeAndSubmissionStatusInAndBeatmapStatusInOrderByPerformancePointsDesc(
        user: User,
        mode: Mode,
        submissionStatuses: List<SubmissionStatus>,
        beatmapRankedStatuses: List<BeatmapRankedStatus>,
    ): List<Score>
}
