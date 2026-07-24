package pe.nanamochi.banchus.score.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.beatmap.entity.Beatmap
import pe.nanamochi.banchus.beatmap.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.core.enums.CountryCode
import pe.nanamochi.banchus.core.enums.Mode
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.score.entity.Score
import pe.nanamochi.banchus.score.enums.SubmissionStatus

@Repository
interface ScoreRepository : JpaRepository<Score, Long> {
    fun findScoreById(scoreId: Long): Score?

    fun findScoreByOnlineChecksum(onlineChecksum: String): Score?

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

    fun findFirstByBeatmapAndUserIdAndSubmissionStatusOrderByPerformancePointsDesc(
        beatmap: Beatmap,
        userId: Int,
        submissionStatus: SubmissionStatus,
    ): Score?

    fun findTop100ByUserAndModeAndSubmissionStatusInAndBeatmapStatusInOrderByPerformancePointsDesc(
        user: User,
        mode: Mode,
        submissionStatuses: List<SubmissionStatus>,
        beatmapRankedStatuses: List<BeatmapRankedStatus>,
    ): List<Score>

    @Query(
        """
        SELECT COUNT(s)
        FROM Score s
        JOIN s.beatmap b
        WHERE s.user.id = :userId
          AND s.mode = :mode
          AND s.submissionStatus = :status
          AND b.status IN :beatmapStatuses
        """
    )
    fun countRankedScores(
        userId: Int,
        mode: Mode,
        status: SubmissionStatus,
        beatmapStatuses: List<BeatmapRankedStatus>,
    ): Long

    @Query(
        """
        SELECT COUNT(s) + 1
        FROM Score s
        JOIN s.user u
        WHERE s.beatmap.id = :beatmapId
          AND s.mode = :mode
          AND s.submissionStatus = :status
          AND (u.privileges >= 1 OR u.id = :userId)
          AND (s.score > :score OR (s.score = :score AND s.id < :scoreId))
        """
    )
    fun findBeatmapRank(
        beatmapId: Int,
        mode: Mode,
        status: SubmissionStatus,
        userId: Int,
        score: Long,
        scoreId: Long,
    ): Long

    @Modifying
    @Query(
        value =
            """
        INSERT INTO scores_first (beatmap_id, mode, score_id, user_id)
        VALUES (:beatmapId, :mode, :scoreId, :userId)
        ON DUPLICATE KEY UPDATE score_id = :scoreId, user_id = :userId
        """,
        nativeQuery = true,
    )
    fun upsertFirstPlace(beatmapId: Int, mode: Int, scoreId: Long, userId: Int)
}
