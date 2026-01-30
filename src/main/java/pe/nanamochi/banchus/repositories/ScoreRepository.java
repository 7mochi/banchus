package pe.nanamochi.banchus.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.entities.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.SubmissionStatus;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.entities.db.User;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {
  List<Score> findByBeatmapAndUserAndSubmissionStatus(
      Beatmap beatmap, User user, SubmissionStatus submissionStatus);

  @Query(
      "SELECT s FROM Score s WHERE s.user = :user AND s.mode = :mode AND s.submissionStatus IN"
          + " :submissionStatuses AND s.beatmap.status IN :beatmapRankedStatuses")
  Page<Score> findManyFiltered(
      @Param("user") User user,
      @Param("mode") Mode mode,
      @Param("submissionStatuses") List<SubmissionStatus> submissionStatuses,
      @Param("beatmapRankedStatuses") List<BeatmapRankedStatus> beatmapRankedStatuses,
      Pageable pageable);

  @Query(
      "SELECT COUNT(s) FROM Score s WHERE s.user = :user AND s.mode = :mode AND s.submissionStatus"
          + " IN :submissionStatuses AND s.beatmap.status IN :beatmapRankedStatuses")
  int countManyFiltered(
      @Param("user") User user,
      @Param("mode") Mode mode,
      @Param("submissionStatuses") List<SubmissionStatus> submissionStatuses,
      @Param("beatmapRankedStatuses") List<BeatmapRankedStatus> beatmapRankedStatuses);

  @Query(
      "SELECT s FROM Score s WHERE s.beatmap = :beatmap AND s.user = :user AND s.submissionStatus ="
          + " :submissionStatus ORDER BY s.performancePoints DESC")
  List<Score> findTopByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
      @Param("beatmap") Beatmap beatmap,
      @Param("user") User user,
      @Param("submissionStatus") SubmissionStatus submissionStatus);
}
